package com.example.musicapp.bottomsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.musicapp.R
import com.example.musicapp.activity.advance.AlbumActivity
import com.example.musicapp.activity.advance.ArtistActivity
import com.example.musicapp.databinding.BottomSheetMoreBinding
import com.example.musicapp.dialog.DeleteDialog
import com.example.musicapp.dialog.DeleteMode
import com.example.musicapp.dialog.InformationDialog
import com.example.musicapp.dialog.RenameDialog
import com.example.musicapp.util.Constant
import com.example.service.model.Album
import com.example.service.model.Artist
import com.example.service.model.Audio
import com.example.service.model.Playlist
import com.example.service.service.MusicPlayerRemote
import com.example.service.service.SongLoader
import com.example.service.utils.DisplayMode
import com.example.service.utils.MDManager
import com.example.service.utils.PlaylistUtils
import com.example.service.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BottomSheetMore(private val context: Context, displayMode: DisplayMode) :
    BottomSheetDialog(context) {
    private val binding by lazy { BottomSheetMoreBinding.inflate(layoutInflater) }
    private val listArtist by lazy {
        SongLoader.getArtistLocal(context)
    }
    private val listAlbum by lazy {
        SongLoader.getAlbumLocal(context)
    }

    init {
        setContentView(binding.root)
        binding.apply {

            when (displayMode) {
                DisplayMode.AUDIO -> {
                    playSongTitle.text = context.resources.getString(R.string.play_song)
                    layoutRename.visibility = View.GONE
                    imageInfo.setImageResource(R.drawable.icon_song)

                    imageInfo.visibility = View.VISIBLE
                    trackIcon.visibility = View.GONE
                    iconAlbumLayout.visibility = View.GONE
                }

                DisplayMode.ARTIST -> {
                    playSongTitle.text = context.resources.getString(R.string.play_artist)
                    imageInfo.setImageResource(R.drawable.icon_song)

                    layoutRename.visibility = View.GONE
                    layoutMoreFromArtist.visibility = View.GONE
                    layoutMoreFromAlbum.visibility = View.GONE
                    firstLine.visibility = View.GONE

                    songInfo.visibility = View.GONE
                    songFavourite.visibility = View.GONE

                    imageInfo.visibility = View.VISIBLE
                    trackIcon.visibility = View.GONE
                    iconAlbumLayout.visibility = View.GONE
                    layoutDeleteFromDevice.visibility = View.GONE
                    layoutShare.visibility = View.GONE
                    secondLine.visibility = View.GONE
                }

                DisplayMode.ALBUM -> {
                    playSongTitle.text = context.resources.getString(R.string.play_album)
                    imageInfo.visibility = View.GONE
                    trackIcon.visibility = View.VISIBLE
                    iconAlbumLayout.visibility = View.VISIBLE

                    layoutRename.visibility = View.GONE
                    layoutMoreFromArtist.visibility = View.GONE
                    layoutMoreFromAlbum.visibility = View.GONE
                    firstLine.visibility = View.GONE

                    songInfo.visibility = View.GONE
                    songFavourite.visibility = View.GONE

                    layoutDeleteFromDevice.visibility = View.GONE
                    layoutShare.visibility = View.GONE
                    secondLine.visibility = View.GONE
                }

                DisplayMode.PLAYLIST -> {
                    layoutDeleteFromPlaylist.visibility = View.VISIBLE
                    playSongTitle.text = context.resources.getString(R.string.play_song)
                    firstLine.visibility = View.GONE
                    secondLine.visibility = View.GONE
                    layoutMoreFromArtist.visibility = View.GONE
                    layoutMoreFromAlbum.visibility = View.GONE

                    imageInfo.visibility = View.VISIBLE
                    trackIcon.visibility = View.GONE
                    iconAlbumLayout.visibility = View.GONE

                }

                else -> {
                    layoutDeleteFromPlayingQueue.visibility = View.VISIBLE
                    playSongTitle.text = context.resources.getString(R.string.play_song)
                    firstLine.visibility = View.GONE
                    secondLine.visibility = View.GONE
                    layoutMoreFromArtist.visibility = View.GONE
                    layoutMoreFromAlbum.visibility = View.GONE

                    imageInfo.visibility = View.VISIBLE
                    trackIcon.visibility = View.GONE
                    iconAlbumLayout.visibility = View.GONE
                }
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun setAudio(
        position: Int,
        listData: List<Any>,
        adapter: RecyclerView.Adapter<ViewHolder>,
        currentPlaylist: Playlist? = null,
    ) {
        val data = listData[position] as Audio
        CoroutineScope(Dispatchers.IO).launch {
            updateFavourite(data)
        }
        binding.apply {
            songName.text = data.mediaObject?.title ?: context.getString(R.string.unknown_song)
            val artistName = if (!data.artist[0].isLetter() || data.artist.contains(
                    Constant.UNKNOWN_STRING,
                    true
                )
            ) context.resources.getString(R.string.unknown_artist) else data.artist
            songArtist.text = "$artistName ⋅ ${data.timeSong}"

            songInfo.setOnClickListener {
                val infoDialog = InformationDialog(context)
                infoDialog.setData(data)
                infoDialog.show()
                dismiss()
            }

            layoutPlay.setOnClickListener {
                MusicPlayerRemote.openQueue(ArrayList(listData), position, true)
                adapter.notifyDataSetChanged()
                dismiss()
            }

            layoutAddPlaylist.setOnClickListener {
                val addPlaylistBottomSheet = BottomSheetAddPlaylist(context)
                addPlaylistBottomSheet.setData(data)
                addPlaylistBottomSheet.show()
                dismiss()
            }

            layoutAddToPlayingQueue.setOnClickListener {
                if (MusicPlayerRemote.getPlayingQueue().contains(data)) {
                    MDManager.getInstance(context)
                        ?.showMessage(context, context.resources.getString(R.string.playlist_song_added_before))
                } else {
                    MusicPlayerRemote.getPlayingQueue().add(data)
                    MDManager.getInstance(context)
                        ?.showMessage(context, context.resources.getString(R.string.playlist_added_successfully))
                }
            }

            songFavourite.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val isFavourite = PlaylistUtils.getInstance(context)
                        ?.checkIsFavourite(data)
                    if (isFavourite == true) {
                        PlaylistUtils.getInstance(context)
                            ?.removeFromFavourite(data)
                    } else {
                        PlaylistUtils.getInstance(context)?.addToFavourite(data)
                    }
                    updateFavourite(data)
                    context.sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
                }

            }

            layoutDeleteFromPlaylist.setOnClickListener {
                val deleteDialog = DeleteDialog(context, DeleteMode.PLAYLIST, data, currentPlaylist)
                deleteDialog.setData(data)
                deleteDialog.show()
                dismiss()
            }

            layoutDeleteFromPlayingQueue.setOnClickListener {
                val deleteDialog = DeleteDialog(context, DeleteMode.PLAYING_SONG, data, currentPlaylist)
                deleteDialog.setData(data)
                deleteDialog.show()
                dismiss()
            }

            layoutRename.setOnClickListener {
                val renameDialog = RenameDialog(context)
                renameDialog.setDataForAudio(data)
                renameDialog.show()
                dismiss()
            }

            layoutDeleteFromDevice.setOnClickListener {
                val deleteDialog = DeleteDialog(context, DeleteMode.AUDIO, data, currentPlaylist)
                deleteDialog.setData(data)
                deleteDialog.show()
                dismiss()
            }

            layoutShare.setOnClickListener {
                Utils.shareVideoOrAudio(context, data.mediaObject?.title, data.mediaObject?.path)
                dismiss()
            }

            layoutMoreFromArtist.setOnClickListener {
                val artistOfSong = listArtist.filter { it.nameArtist == data.artist }
                if (artistOfSong.isNullOrEmpty()) {
                    MDManager.getInstance(context)
                        ?.showMessage(context, "Can not find the artist of this song!")
                } else {
                    val intent = Intent(context, ArtistActivity::class.java)
                    intent.putExtra(ArtistActivity.ARTIST_KEY, Gson().toJson(artistOfSong.first()))
                    context.startActivity(intent)
                }
            }
            layoutMoreFromAlbum.setOnClickListener {
                val albumOfSong = listAlbum.filter { it.albumName == data.albumName }
                if (albumOfSong.isNullOrEmpty()) {
                    MDManager.getInstance(context)
                        ?.showMessage(context, "Can not find the album of this song!")
                } else {
                    val intent = Intent(context, AlbumActivity::class.java)
                    intent.putExtra(AlbumActivity.ALBUM_KEY, Gson().toJson(albumOfSong.first()))
                    context.startActivity(intent)
                }
            }
        }
    }

    private suspend fun updateFavourite(data: Audio) {
        val isFavourite = PlaylistUtils.getInstance(context)?.checkIsFavourite(data) ?: false
        withContext(Dispatchers.Main) {
            if (isFavourite) {
                binding.songFavourite.setImageResource(R.drawable.icon_liked)
            } else {
                binding.songFavourite.setImageResource(R.drawable.icon_favourite)

            }
        }
    }

    fun setArtist(
        position: Int = 0 , listData: List<Any> = listOf(), artist: Artist? = null
    ) {
        val data = artist ?: listData[position] as Artist
        binding.apply {
            songName.text = data.nameArtist
            val albumCount = context.resources.getQuantityString(
                R.plurals.album_count,
                data.numberOfAlbum.toInt(),
                data.numberOfAlbum.toInt()
            )
            songArtist.text = albumCount

            layoutPlay.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val songs = SongLoader.getSongByArtistId(data.artistId.toLong(), context)
                    withContext(Dispatchers.Main) {
                        MusicPlayerRemote.openQueue(ArrayList(songs), 0, true)
                        dismiss()
                    }
                }
            }

            layoutAddPlaylist.setOnClickListener {
                val addPlaylistBottomSheet = BottomSheetAddPlaylist(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val artistDetail = SongLoader.getSongByArtistId(data.artistId.toLong(), context)
                    withContext(Dispatchers.Main) {
                        addPlaylistBottomSheet.setData(null, artistDetail)
                        addPlaylistBottomSheet.show()
                        dismiss()
                    }
                }
            }

            layoutAddToPlayingQueue.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val artistDetail = SongLoader.getSongByArtistId(data.artistId.toLong(), context)
                    withContext(Dispatchers.Main) {
                        if (MusicPlayerRemote.getPlayingQueue().contains(artistDetail)) {
                            MDManager.getInstance(context)
                                ?.showMessage(context, context.resources.getString(R.string.playlist_song_added_before))
                        } else {
                            MusicPlayerRemote.getPlayingQueue().addAll(artistDetail)
                            MDManager.getInstance(context)
                                ?.showMessage(context, context.resources.getString(R.string.playlist_added_successfully))
                        }
                    }
                }

            }

            layoutDeleteFromDevice.setOnClickListener {
                val deleteDialog = DeleteDialog(context, DeleteMode.ARTIST)
                deleteDialog.show()
                dismiss()
            }

        }
    }

    fun setAlbum(
       data: Album
    ) {
        binding.apply {
            songName.text = data.albumName
            val quantityString = context.resources.getQuantityString(
                R.plurals.song_count,
                data.numberSong.toInt(),
                data.numberSong.toInt(),
                data.numberSong.toInt(),
            )
            songArtist.text = quantityString

            layoutPlay.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val songs = SongLoader.getSongAlbumById(data.albumId.toLong(), context)
                    withContext(Dispatchers.Main) {
                        MusicPlayerRemote.openQueue(ArrayList(songs), 0, true)
                        dismiss()
                    }
                }
            }

            layoutAddPlaylist.setOnClickListener {
                val addPlaylistBottomSheet = BottomSheetAddPlaylist(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val albumDetail = SongLoader.getSongByArtistId(data.albumId.toLong(), context)
                    withContext(Dispatchers.Main) {
                        addPlaylistBottomSheet.setData(null, albumDetail)
                        addPlaylistBottomSheet.show()
                        dismiss()
                    }
                }
            }

            layoutAddToPlayingQueue.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val albumDetail = SongLoader.getSongAlbumById(data.albumId.toLong(), context)
                    withContext(Dispatchers.Main) {
                        if (MusicPlayerRemote.getPlayingQueue().contains(albumDetail)) {
                            MDManager.getInstance(context)
                                ?.showMessage(context, context.resources.getString(R.string.playlist_song_added_before))
                        } else {
                            MusicPlayerRemote.getPlayingQueue().addAll(albumDetail)
                            MDManager.getInstance(context)
                                ?.showMessage(context, context.resources.getString(R.string.playlist_added_successfully))
                        }
                    }
                }
            }

            layoutDeleteFromDevice.setOnClickListener {
                val deleteDialog = DeleteDialog(context, DeleteMode.ARTIST)
                deleteDialog.show()
                dismiss()
            }
        }
    }
}

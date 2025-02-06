package com.example.musicapp.adapter.advance

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.example.musicapp.R
import com.example.musicapp.adapter.SongPlayingAdapter
import com.example.musicapp.bottomsheet.BottomSheetMore
import com.example.musicapp.databinding.ItemAlbumLineHeaderBinding
import com.example.musicapp.databinding.ItemSongDetailBinding
import com.example.musicapp.dialog.SortAudioDialog
import com.example.musicapp.util.Constant
import com.example.musicapp.util.Utils.getColorFromAttr
import com.example.service.model.Audio
import com.example.service.model.Playlist
import com.example.service.service.MusicPlayerRemote
import com.example.service.utils.DisplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlbumLineAdapter(
    private val isPlaylist: Boolean = false,
    private val displayMode: DisplayMode,
    private val onClick: () -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listAlbum = mutableListOf<Any>()
    private var currentPlaylist: Playlist? = Playlist()
    var lastVisibleItemPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == SongPlayingAdapter.TYPE_HEADER) {
            val binding = ItemAlbumLineHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return AlbumStatisticsHeaderViewHolder(binding)
        }

        val binding =
            ItemSongDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumSongViewHolder(binding)

    }

    override fun getItemCount(): Int {
        return if (listAlbum.isEmpty()) 0 else listAlbum.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AlbumSongViewHolder) {
            holder.bind(position - 1)
            if (position > lastVisibleItemPosition) {
                val animation =
                    AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_zoom_in)
                holder.itemView.startAnimation(animation)
            }
        } else if (holder is AlbumStatisticsHeaderViewHolder) {
            holder.updateData()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) SongPlayingAdapter.TYPE_HEADER else SongPlayingAdapter.TYPE_SONG
    }

    fun updateData(list: List<Any>, playList: Playlist? = null) {
        lastVisibleItemPosition = 9999
        currentPlaylist = playList
        listAlbum.clear()
        listAlbum.clear()
        listAlbum.addAll(list)
        notifyDataSetChanged()
    }

    inner class AlbumStatisticsHeaderViewHolder(private val binding: ItemAlbumLineHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
        fun updateData() {
            binding.apply {
                val quantityString =
                    context.resources.getQuantityString(
                        R.plurals.song_count,
                        listAlbum.size,
                        listAlbum.size,
                        listAlbum.size
                    )
                albumCount.text = quantityString
                albumSort.setOnClickListener {
                    val dialogName = SortAudioDialog(context)
                    dialogName.show()
                }
                playAllButton.setOnClickListener {
                    MusicPlayerRemote.openQueue(ArrayList(listAlbum), 0, true)
                    notifyDataSetChanged()
                }
                shuffleButton.setOnClickListener {
                    MusicPlayerRemote.openAndShuffleQueue(ArrayList(listAlbum), true)
                    notifyDataSetChanged()
                }

                albumAddSong.setOnClickListener {
                    onClick()
                }
                if (isPlaylist) {
                    albumAddSong.visibility = View.VISIBLE
                }
            }
        }

    }

    inner class AlbumSongViewHolder(private val binding: ItemSongDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
        fun bind(position: Int) {
            val song = listAlbum[position] as Audio
            val string =
                song.mediaObject?.path?.let { it.substring(it.lastIndexOf('/') + 1, it.length) }
            val songTitle = song.mediaObject?.title
            val title = if (songTitle.isNullOrEmpty()) string else songTitle
            binding.songDuration.text = song.timeSong
            val artistName = if (!song.artist[0].isLetter() || song.artist.contains(
                    Constant.UNKNOWN_STRING,
                    true
                )
            ) context.resources.getString(R.string.unknown_artist) else song.artist
            binding.songArtist.text = artistName
            binding.songName.text = title

            binding.root.setOnClickListener {
                MusicPlayerRemote.openQueue(ArrayList(listAlbum), position, true)
                notifyDataSetChanged()
            }


            binding.songAddMore.setOnClickListener {
                val bottomSheetMore = BottomSheetMore(context, displayMode)
                bottomSheetMore.setAudio(position, listAlbum, this@AlbumLineAdapter, currentPlaylist)
                bottomSheetMore.show()
            }
            val currentSong = MusicPlayerRemote.getCurrentSong()
            if (song.mediaObject?.path == currentSong.mediaObject?.path && song.mediaObject?.path != null) {

                binding.songIcon.visibility = View.GONE
                binding.animation.visibility = View.VISIBLE

                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    if (MusicPlayerRemote.isPlaying()) {
                        binding.animation.playAnimation()
                    } else {
                        binding.animation.pauseAnimation()
                    }
                }

                val color = ContextCompat.getColor(context, R.color.high_light_color)
                binding.songName.setTextColor(color)
                binding.songArtist.setTextColor(color)
                binding.songDuration.setTextColor(color)
                binding.songDot.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                binding.animation.addValueCallback(
                    KeyPath("**"), LottieProperty.STROKE_COLOR
                ) {
                    color
                }

            } else {
                binding.songIcon.visibility = View.VISIBLE
                binding.animation.visibility = View.GONE

                val color = ContextCompat.getColor(context, R.color.purple_text)
                val unselectedColor = context.getColorFromAttr(R.attr.textColor)
                binding.songName.setTextColor(unselectedColor)
                binding.songArtist.setTextColor(color)
                binding.songDuration.setTextColor(color)
                binding.songDot.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            binding.iconQueue.visibility = View.GONE

        }
    }
}
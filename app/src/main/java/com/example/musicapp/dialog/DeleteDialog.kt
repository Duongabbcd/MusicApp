package com.example.musicapp.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import androidx.annotation.RequiresApi
import com.example.musicapp.R
import com.example.musicapp.activity.advance.PlaylistActivity.Companion.ACTION_UPDATE_DATA_PLAYLIST
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.DialogDeleteBinding
import com.example.service.model.Audio
import com.example.service.model.Playlist
import com.example.service.service.MusicPlayerRemote
import com.example.service.utils.PlaylistUtils
import com.example.service.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteDialog(
    context: Context,
    private val mode: DeleteMode,
    private val currentAudio: Audio? = null,
    private val currentPlaylist: Playlist? = null,
) : Dialog(context) {
    private val binding by lazy { DialogDeleteBinding.inflate(layoutInflater) }
    private var isFavourite : Boolean  = false

    init {
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = currentAudio?.mediaObject?.title ?: currentPlaylist?.title ?: ""
        binding.clearQueue.text = when (mode) {
            DeleteMode.ARTIST -> context.getString(R.string.delete_artist)
            DeleteMode.ALBUM -> context.getString(R.string.delete_album)
            DeleteMode.QUEUE -> context.getString(R.string.delete_queue)
            DeleteMode.PLAYLIST -> context.getString(R.string.remove_from_playlist)
            DeleteMode.AUDIO -> context.getString(R.string.delete_file)
            DeleteMode.PLAYING_SONG -> context.getString(R.string.remove_from_playing_queue)
            DeleteMode.DEFAULT -> context.getString(R.string.delete_playlist)
        }
        val text = when (mode) {
            DeleteMode.ARTIST -> context.getString(R.string.delete_artist_question)
            DeleteMode.ALBUM -> context.getString(R.string.delete_album_question)
            DeleteMode.QUEUE -> context.getString(R.string.delete_queue_question)
            DeleteMode.PLAYLIST -> {
                context.getString(
                    R.string.delete_audio_from_playlist, title
                )
            }

            DeleteMode.AUDIO -> {
                context.getString(
                    R.string.delete_audio_question,
                    title
                )
            }

            DeleteMode.PLAYING_SONG -> context.getString(
                R.string.delete_audio_from_playlist, title
            )

            DeleteMode.DEFAULT -> {
                if(isFavourite) { context.getString(
                    R.string.delete_favourite_playlist_question,
                    title
                )} else
                context.getString(
                    R.string.delete_playlist_question,
                    title
                )
            }
        }
        val spannable = SpannableString(text)
        val startIndex = text.indexOf(title, ignoreCase = true)
        val style = TextAppearanceSpan(context, R.style.CustomTextMediumStyle16sp)
        if (startIndex >= 0) {
            spannable.setSpan(
                style,
                startIndex, startIndex + title.trim().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.clearQueueSubText.text = spannable


        binding.textOk.text = when (mode) {
            DeleteMode.ARTIST -> context.getString(R.string.delete)
            DeleteMode.ALBUM -> context.getString(R.string.delete)
            DeleteMode.QUEUE -> context.getString(R.string.clear)
            DeleteMode.PLAYLIST -> context.getString(R.string.remove)
            DeleteMode.AUDIO -> context.getString(R.string.delete)
            DeleteMode.PLAYING_SONG -> context.getString(R.string.remove)
            DeleteMode.DEFAULT -> context.getString(R.string.delete)
        }

        binding.textCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setData(data: Any, isFavouriteList: Boolean = false) {
        isFavourite = isFavouriteList
        binding.textOk.setOnClickListener {
            println("deleteSongFromPlaylist: $mode")
            when (mode) {
                DeleteMode.AUDIO -> deleteAudioFromDevice(data)
                DeleteMode.ALBUM -> deleteAlbumFromDevice(data)
                DeleteMode.ARTIST -> deleteArtistFromDevice(data)
                DeleteMode.QUEUE -> deleteQueueFromDevice()
                DeleteMode.PLAYLIST -> deleteSongFromPlaylist(
                    data,
                    currentPlaylist = currentPlaylist!!
                )

                DeleteMode.PLAYING_SONG -> deleteSongFromPlayingQueue(data)
                DeleteMode.DEFAULT -> deletePlaylist(
                    currentPlaylist
                )
            }
        }
    }

    private fun deleteSongFromPlayingQueue(data: Any) {
        val song = data as Audio
        MusicPlayerRemote.removeFromQueue(song)
        context.sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
        dismiss()
    }

    private fun deletePlaylist(currentPlaylist: Playlist?) {
        currentPlaylist?.let {
            if (it.id == PlaylistUtils.favouriteId()) {
                it.tracks.clear()
                CoroutineScope(Dispatchers.IO).launch {
                    PlaylistUtils.getInstance(context)?.savePlaylist(currentPlaylist)
                }
            } else {
                PlaylistUtils.getInstance(context)
                    ?.dropPlaylist(it)
            }
            context.sendBroadcast(Intent(ACTION_UPDATE_DATA_PLAYLIST))
            dismiss()
        }
    }

    private fun deleteSongFromPlaylist(data: Any, currentPlaylist: Playlist) {
        val index = currentPlaylist.tracks.indexOf(data)
        val newPlaylist = currentPlaylist
        CoroutineScope(Dispatchers.IO).launch {
            if (newPlaylist.tracks.isNotEmpty()) {
                newPlaylist.tracks.removeAt(index)
            }

            PlaylistUtils.getInstance(context)?.savePlaylist(newPlaylist)
            context.sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
            withContext(Dispatchers.Main) {
                dismiss()
            }
        }

    }

    private fun deleteQueueFromDevice() {
        MusicPlayerRemote.clearQueue()
        context.sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
        dismiss()
    }

    private fun deleteArtistFromDevice(data: Any) {
        TODO("Not yet implemented")
    }

    private fun deleteAlbumFromDevice(data: Any) {
        TODO("Not yet implemented")
    }

    private fun deleteAudioFromDevice(data: Any) {
        val audio = data as Audio
        val intent = Intent(BaseActivity.ACTION_DELETE_FILE)
        intent.putExtra(BaseActivity.KEY_ID_SONG, audio.mediaObject?.path)
        context.sendBroadcast(intent)
        dismiss()
    }
}

enum class DeleteMode {
    ARTIST,
    ALBUM,
    QUEUE,
    PLAYLIST,
    AUDIO,
    PLAYING_SONG,
    DEFAULT
}
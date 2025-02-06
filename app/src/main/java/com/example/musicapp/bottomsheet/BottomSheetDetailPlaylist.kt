package com.example.musicapp.bottomsheet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.activity.advance.PlaylistActivity.Companion.ACTION_UPDATE_DATA_PLAYLIST
import com.example.musicapp.databinding.BottomSheetPlaylistDetailBinding
import com.example.musicapp.dialog.DeleteDialog
import com.example.musicapp.dialog.DeleteMode
import com.example.musicapp.dialog.RenameDialog
import com.example.service.model.Playlist
import com.example.service.service.MusicPlayerRemote
import com.example.service.utils.MDManager
import com.example.service.utils.PlaylistUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BottomSheetDetailPlaylist(context: Context) : BottomSheetDialog(context) {
    private var playlist: Playlist? = null
    private lateinit var allPlaylists: MutableList<Playlist>
    private var currentPlaylistPosition: Int = -1
    private val binding by lazy { BottomSheetPlaylistDetailBinding.inflate(layoutInflater) }

    private var isFavourite = false

    init {
        setContentView(binding.root)

    }

    fun setData(selected: Playlist? = null ,position: Int, listData: MutableList<Any>) {
        playlist = selected ?: listData[position] as Playlist
        if(position in listOf(-1, 0)) isFavourite = true
        currentPlaylistPosition = position
        allPlaylists = listData as MutableList<Playlist>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        if(isFavourite) {
            binding.layoutRename.visibility = View.GONE
            binding.layoutDeleteFromDevice.visibility = View.GONE
            binding.clearMyFavourite.visibility = View.VISIBLE
        }

        binding.clearMyFavourite.setOnClickListener {
            playlist?.let { playlist ->
                val dialog = DeleteDialog(context, DeleteMode.DEFAULT, currentPlaylist = playlist)
                dialog.setData(playlist, true)
                dialog.show()
                dismiss()
            }
        }

        binding.layoutDeleteFromDevice.setOnClickListener {
            playlist?.let { playlist ->
                val dialog = DeleteDialog(context, DeleteMode.DEFAULT, currentPlaylist = playlist)
                dialog.setData(playlist)
                dialog.show()
                dismiss()
            }
        }

        binding.layoutPlay.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val songs = playlist?.tracks ?: emptyList()
                withContext(Dispatchers.Main) {
                    MusicPlayerRemote.openQueue(ArrayList(songs), 0, true)
                    dismiss()
                }
            }
        }

        binding.layoutAddToPlayingQueue.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val songs = playlist?.tracks ?: emptyList()
                withContext(Dispatchers.Main) {
                    if (MusicPlayerRemote.getPlayingQueue().contains(songs)) {
                        MDManager.getInstance(context)
                            ?.showMessage(context, context.resources.getString(R.string.playlist_song_added_before))
                    } else {
                        MusicPlayerRemote.getPlayingQueue().addAll(songs)
                        MDManager.getInstance(context)
                            ?.showMessage(context, context.resources.getString(R.string.playlist_added_successfully))
                    }
                }
            }
        }

        binding.layoutAddPlaylist.setOnClickListener {
            val addPlaylistBottomSheet = BottomSheetAddPlaylist(context)
            addPlaylistBottomSheet.setData(listAudio = playlist!!.tracks)
            addPlaylistBottomSheet.show()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ACTION_UPDATE_DATA_PLAYLIST)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                updateBroadcast, intentFilter, Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(updateBroadcast, intentFilter)
        }
        reloadData()
    }

    override fun onStop() {
        super.onStop()
        context.unregisterReceiver(updateBroadcast)
    }

    private fun reloadData() {
        if (MainActivity.isChangeTheme) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val playlists =
                PlaylistUtils.getInstance(context)?.getPlaylists() ?: arrayListOf()
            allPlaylists = playlists.toMutableList()
            withContext(Dispatchers.Main) {

                binding.apply {
                    layoutRename.setOnClickListener {
                        val dialog = RenameDialog(context)
                        playlist?.let {
                            dialog.setDataForPlaylist(it)
                        }
                        dialog.show()
                        dismiss()
                    }

                    songName.text = playlist?.title ?: ""
                    val songCountString = context.resources.getQuantityString(
                        R.plurals.song_count, playlist?.tracks?.size!!,
                        playlist?.tracks?.size!!,
                        playlist?.tracks?.size!!,
                    )
                    songArtist.text = songCountString
                }
            }
        }
    }

    private val updateBroadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadData()
        }
    }
}
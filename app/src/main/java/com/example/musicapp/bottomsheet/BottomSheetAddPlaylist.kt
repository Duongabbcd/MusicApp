package com.example.musicapp.bottomsheet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.musicapp.R
import com.example.musicapp.activity.advance.PlaylistActivity
import com.example.musicapp.adapter.basic.AddPlaylistAdapter
import com.example.musicapp.databinding.BottomSheetAddPlaylistBinding
import com.example.musicapp.dialog.NewPlayListDialog
import com.example.service.model.Audio
import com.example.service.utils.MDManager
import com.example.service.utils.PlaylistUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BottomSheetAddPlaylist(context: Context) : BottomSheetDialog(context) {
    private val binding by lazy { BottomSheetAddPlaylistBinding.inflate(layoutInflater) }
    private var audio: Audio? = null
    private var dataList: MutableList<Audio> = mutableListOf()
    private val adapter = AddPlaylistAdapter {
        CoroutineScope(Dispatchers.IO).launch {
            val playlist = PlaylistUtils.getInstance(context)?.getPlaylistById(it)
            println("BottomSheetAddPlaylist: $it $playlist")
            var x = true
            if (dataList.isEmpty()) {
                x = PlaylistUtils.getInstance(context)?.addSongToPlaylist(audio, playlist) ?: false
            } else {
                PlaylistUtils.getInstance(context)
                    ?.addListSongToPlaylistOne(dataList, playlist)
            }
            withContext(Dispatchers.Main) {
               if(x) {
                   MDManager.getInstance(context)?.showMessage(
                       context, context.getString(R.string.add_song_success)
                   )
               } else {
                   MDManager.getInstance(context)?.showMessage(
                       context, context.getString(R.string.song_added_before)
                   )
               }
                context.sendBroadcast(Intent(PlaylistActivity.ACTION_UPDATE_DATA_PLAYLIST))
                dismiss()
            }

        }
    }

    init {
        setContentView(binding.root)
    }

    fun setData(inputData: Audio? = null, listAudio: List<Audio> = mutableListOf()) {
        audio = inputData
        dataList.clear()
        dataList.addAll(listAudio)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.addPlaylistRV.adapter = adapter
        CoroutineScope(Dispatchers.IO).launch{
            val playlist = PlaylistUtils.getInstance(context)?.getPlaylists()
            withContext(Dispatchers.Main) {
                adapter.updateData(playlist)
            }
        }
        binding.layoutPlay.setOnClickListener {
            val dialogName = NewPlayListDialog(context)
            dialogName.setData(audio, dataList)
            dialogName.setOnReloadData {
                context.sendBroadcast(Intent(PlaylistActivity.ACTION_UPDATE_DATA_PLAYLIST))
            }
            dialogName.show()
            dismiss()
        }
    }
}
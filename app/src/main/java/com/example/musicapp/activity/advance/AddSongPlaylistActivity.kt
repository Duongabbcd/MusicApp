package com.example.musicapp.activity.advance

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.activity.advance.DetailPlaylistActivity.Companion.KEY_ID_PLAYLIST
import com.example.service.model.Audio
import com.example.musicapp.adapter.advance.AddSongPlaylistAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivityAddSongPlaylistBinding
import com.example.musicapp.util.Utils.hideKeyBoard
import com.example.service.model.Playlist
import com.example.service.service.SongLoader
import com.example.service.utils.PlaylistUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddSongPlaylistActivity :
    BaseActivity<ActivityAddSongPlaylistBinding>(ActivityAddSongPlaylistBinding::inflate) {
    private val addSongPlaylistAdapter = AddSongPlaylistAdapter {
        listSelected.clear()
        listSelected.addAll(it)
        updateSelected()
    }

    private fun updateSelected() {
        if (listSelected.isEmpty()) {
            binding.addSongBtn.isEnabled = false
            binding.playlistName.text = getString(R.string.add_songs)
            binding.backBtn.setImageResource(R.drawable.icon_back)
            binding.addSongBtn.setBackgroundResource(R.drawable.background_round_green_button)
            binding.selectSongsBtn.setImageResource(R.drawable.icon_unselected_top)
        } else {
            binding.addSongBtn.isEnabled = true
            val quantityString = binding.root.context.resources.getQuantityString(
                R.plurals.song_selected, listSelected.size, listSelected.size,
                listSelected.size
            )
            binding.playlistName.text = quantityString
            binding.backBtn.setImageResource(R.drawable.icon_close_top)
            binding.addSongBtn.setBackgroundResource(R.drawable.background_highlight_round)
        }
    }

    private val idPlaylist: Int by lazy {
        intent.getIntExtra(KEY_ID_PLAYLIST, -1)
    }
    private val listSongs = mutableListOf<Audio>()
    private val currentListSong = mutableListOf<Audio>()
    private val listSelected = mutableListOf<Audio>()

    private var playlist: Playlist? = null

    private var isGetAll: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.songOfAlbumRV.adapter = addSongPlaylistAdapter
        binding.playlistName.text = getString(R.string.add_songs)
        listSelected.clear()
        updateSelected()
        try {
            CoroutineScope(Dispatchers.IO).launch {
                playlist = PlaylistUtils.getInstance(this@AddSongPlaylistActivity)
                    ?.getPlaylistById(idPlaylist)

                val listData = SongLoader.getAllSongs(this@AddSongPlaylistActivity)
                listSongs.clear()
                listSongs.addAll(listData.filter { !playlist!!.tracks.contains(it) })
                currentListSong.clear()
                currentListSong.addAll(listSongs)
                withContext(Dispatchers.Main) {
                    addSongPlaylistAdapter.updateData(
                        currentListSong,
                        mutableListOf()
                    )
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        binding.searchBarIcon.visibility = View.VISIBLE
        binding.searchBarTypingIcon.visibility = View.GONE

        binding.searchBarText.addTextChangedListener {
            if (it.toString().isEmpty()) {
                currentListSong.clear()
                currentListSong.addAll(listSongs)
                addSongPlaylistAdapter.updateData(currentListSong, listSelected)
                binding.searchBarTypingIcon.setImageResource(R.drawable.icon_search_purple)
            } else {
                binding.searchBarIcon.visibility = View.GONE
                binding.searchBarTypingIcon.visibility = View.VISIBLE
                binding.searchBarTypingIcon.setImageResource(R.drawable.icon_close)
                val listResult = listSongs.filter { data ->
                    data.mediaObject!!.title.contains(it.toString(), true)
                }

                currentListSong.clear()
                currentListSong.addAll(listResult)
                addSongPlaylistAdapter.updateData(currentListSong, listSelected, it.toString())
            }
        }

        binding.searchBarTypingIcon.setOnClickListener {
            binding.searchBarText.setText("")
            this@AddSongPlaylistActivity.hideKeyBoard(binding.searchBarText)
        }

        binding.backBtn.setOnClickListener {
            if(listSelected.isEmpty()) {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            } else {
                listSelected.clear()
                addSongPlaylistAdapter.updateData(
                    currentListSong,
                    listSelected.toMutableList(),
                )
                updateSelected()
            }
        }

        binding.addSongBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (listSelected.isNotEmpty()) {
                    PlaylistUtils.getInstance(this@AddSongPlaylistActivity)?.addListSongToPlaylistOne(
                        listSelected, playlist
                    )
                }
                withContext(Dispatchers.Main) {
                    finish()
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }
            }
        }

        binding.selectSongsBtn.setOnClickListener {
            if (isGetAll) {
                binding.selectSongsBtn.setImageResource(R.drawable.icon_unselected)
                isGetAll = false

                listSelected.clear()
                updateSelected()
                addSongPlaylistAdapter.updateData(currentListSong, listSelected)

            } else {
                binding.selectSongsBtn.setImageResource(R.drawable.icon_selected)
                isGetAll = true

                listSelected.addAll(currentListSong)
                updateSelected()
                addSongPlaylistAdapter.updateData(currentListSong, listSelected)
            }
        }

        binding.songOfAlbumRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                this@AddSongPlaylistActivity.hideKeyBoard(binding.searchBarText)
            }
        })

    }

}
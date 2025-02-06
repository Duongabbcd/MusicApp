package com.example.musicapp.fragment.basic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.adapter.basic.SongAdapter
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.databinding.FragmentSongBinding
import com.example.musicapp.util.Utils.hideKeyBoard
import com.example.scrollbar.attachTo
import com.example.service.model.Audio
import com.example.service.service.MusicService
import com.example.service.service.SongLoader
import com.example.service.utils.DisplayMode
import com.example.service.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongFragment : BaseFragment<FragmentSongBinding>(FragmentSongBinding::inflate) {
    private var songAdapter: SongAdapter = SongAdapter(DisplayMode.AUDIO, false, {str -> searchSong(str)}, {}) { audio, isSelected ->
        if(isSelected) {
            selectedSongs.add(audio)
        } else {
            selectedSongs.remove(audio)
        }
    }

    private var selectedSongs = mutableListOf<Audio>()
    private val listDataSong = mutableListOf<Audio>()

    private val broadcastChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
                )
            ) {
                songAdapter.notifyDataSetChanged()
            }
            if (action == Utils.ACTION_FINISH_DOWNLOAD) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    reloadData()
                }
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadData()
        binding.apply {
            swipeRefreshLayout.setOnRefreshListener {
                reloadData()
            }
            allSongsRV.apply {
                layoutManager = LinearLayoutManager(requireContext())
                itemAnimator = null
                adapter = songAdapter

            }
            swipeRefreshLayout.setOnRefreshListener {
                reloadData()
            }

            setUpDisplayList()

            verticalScrollbar.apply {
                attachTo(allSongsRV)
                customTrackDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.scrollbar_custom_track)
            }
        }
    }

    private fun setUpDisplayList() {
        binding.scrollToTop.setOnClickListener {
            binding.allSongsRV.smoothScrollToPosition(0)
        }

        binding.allSongsRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                requireContext().hideKeyBoard(binding.root)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                songAdapter.lastVisibleItemPosition = lastVisibleItemPosition

                binding.scrollToTop.isVisible = firstVisibleItemPosition > 0
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(
            MusicService.PLAY_STATE_CHANGED.replace(
                MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
            )
        )
        intentFilter.addAction(Utils.ACTION_FINISH_DOWNLOAD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                broadcastChange, intentFilter, Context.RECEIVER_EXPORTED
            )
        } else {
            requireContext().registerReceiver(broadcastChange, intentFilter)
        }

    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(broadcastChange)
    }

    override fun onResume() {
        super.onResume()
        reloadData()
        songAdapter.notifyItemRangeChanged(1, listDataSong.size - 1)
    }

    override fun reloadData() {
        if (MainActivity.isChangeTheme) {
            return
        }

        binding.swipeRefreshLayout.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            val listSong = SongLoader.getAllSongs(requireContext())
            listDataSong.clear()
            listDataSong.addAll(listSong)
            if (listSong.isEmpty()) {
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (isAdded) {
                    binding.allSongsRV.adapter = songAdapter
                    if (context != null) {
                        songAdapter.updateData(listDataSong)
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun searchSong(searchKey: String) {
        try {

            CoroutineScope(Dispatchers.IO).launch {

                val listSong =
                    SongLoader.getAllSongs(
                        requireContext())
                if (listSong.isEmpty()) {
                    return@launch
                }
                val selected = if (!searchKey.isNullOrEmpty()) listSong.filter {
                    it.mediaObject!!.title.contains(
                        searchKey,
                        true
                    )
                } else listSong

                withContext(Dispatchers.Main) {
                    songAdapter.updateData(selected, searchKey)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
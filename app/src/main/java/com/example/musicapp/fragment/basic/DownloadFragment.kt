package com.example.musicapp.fragment.basic

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import com.example.service.model.Audio
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.adapter.basic.SongAdapter
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.databinding.FragmentDownloadBinding
import com.example.musicapp.util.Constant
import com.example.musicapp.util.Utils.hideKeyBoard
import com.example.scrollbar.attachTo
import com.example.service.service.MusicService
import com.example.service.service.SongLoader
import com.example.service.utils.DisplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DownloadFragment : BaseFragment<FragmentDownloadBinding>(FragmentDownloadBinding::inflate) {
    private val musicAdapter = SongAdapter(DisplayMode.AUDIO, true, { str ->
        searchSong(str)
    }, {}) { audio, isSelected ->
        println("SongFragment: $audio and $isSelected")
    }
    private val listSong = mutableListOf<Audio>()

    private val broadcastChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME,
                    MusicService.MUSIC_PACKAGE_NAME
                )
            ) {
                musicAdapter.notifyDataSetChanged()
            }
            if (action in listOf(Constant.ACTION_FINISH_DOWNLOAD, MusicService.QUEUE_CHANGED)) {
                reloadData(true)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(
            MusicService.PLAY_STATE_CHANGED.replace(
                MusicService.MUSIC_PLAYER_PACKAGE_NAME,
                MusicService.MUSIC_PACKAGE_NAME
            )
        )
        intentFilter.addAction(
            Constant.ACTION_FINISH_DOWNLOAD
        )
        intentFilter.addAction(
            MusicService.QUEUE_CHANGED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                broadcastChange, intentFilter, Context.RECEIVER_EXPORTED
            )
        } else {
            requireContext().registerReceiver(broadcastChange, intentFilter)
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            setUpTopToolbar()
            layoutRefresh.setOnRefreshListener {
                reloadData(true)
            }

            downloadSongRV.layoutManager = LinearLayoutManager(requireContext())
            downloadSongRV.adapter = musicAdapter
            downloadSongRV.itemAnimator = null

            scrollToTop.setOnClickListener {
                downloadSongRV.smoothScrollToPosition(0)
            }
            downloadSongRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    requireContext().hideKeyBoard(binding.root)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    musicAdapter.lastVisibleItemPosition = lastVisibleItemPosition
                    scrollToTop.isVisible = firstVisibleItemPosition > 0
                }
            })

            verticalScrollbar.apply {
                attachTo(downloadSongRV)
                customTrackDrawable =
                    ContextCompat.getDrawable(requireContext(), R.drawable.scrollbar_custom_track)
            }
        }
    }

    private fun setUpTopToolbar() {
        var inputText = ""
        binding.apply {
            val listData: ArrayList<Audio> = SongLoader.getAllSongsDownload(requireContext())

            topToolbar.apply {
                albumCount.text = requireContext().resources.getQuantityString(
                    R.plurals.song_count, listData.size, listData.size, listData.size
                )

                searchBarText.addTextChangedListener { s ->
                    inputText = s.toString()
                    if (inputText.isNotEmpty()) {
                        searchBarIcon.visibility = View.VISIBLE
                        searchBarIconLeft.visibility = View.GONE
                        searchBarIcon.setImageResource(R.drawable.icon_close)
                    } else {
                        searchBarIcon.setImageResource(R.drawable.icon_search_purple)

                    }

                    searchSong(inputText)
                }

                searchBarIcon.setOnClickListener {
                    searchBarText.setText("")
                    searchSong("")
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(broadcastChange)
    }

    override fun onResume() {
        super.onResume()
        reloadData(false)
    }

    override fun reloadData(isLoading: Boolean) {
        if (MainActivity.isChangeTheme) {
            return
        }
        binding.apply {
            layoutRefresh.isRefreshing = isLoading
            CoroutineScope(Dispatchers.IO).launch {
                val listData: ArrayList<Audio> = SongLoader.getAllSongsDownload(requireContext())
                listSong.clear()
                listSong.addAll(listData)
                withContext(Dispatchers.Main) {
                    if (listData.isEmpty()) {
                        emptyListLayout.visibility = View.VISIBLE
                    } else {
                        emptyListLayout.visibility = View.GONE
                    }
                    downloadSongRV.isVisible = listSong.isNotEmpty()

                    musicAdapter.updateData(listSong)
                    layoutRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun searchSong(searchKey: String) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val listSong =
                    SongLoader.getAllSongsDownload(
                        requireContext()
                    )
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
                    musicAdapter.updateData(selected, searchKey)
                    binding.topToolbar.albumCount.text = requireContext().resources.getQuantityString(
                        R.plurals.song_count, listSong.size, listSong.size, listSong.size
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
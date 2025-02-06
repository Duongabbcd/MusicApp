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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.activity.advance.AlbumActivity
import com.example.musicapp.adapter.basic.AlbumAdapter
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.databinding.FragmentAlbumBinding
import com.example.scrollbar.attachTo
import com.example.service.model.Album
import com.example.service.service.MusicService
import com.example.service.service.SongLoader
import com.example.service.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumFragment : BaseFragment<FragmentAlbumBinding>(FragmentAlbumBinding::inflate) {
    private var albumList = mutableListOf<Album>()
    private val adapter = AlbumAdapter {
        val intent = Intent(requireContext(), AlbumActivity::class.java)
        intent.putExtra(AlbumActivity.ALBUM_KEY, Gson().toJson(it))
        intent.putExtra(AlbumActivity.ALBUM_POSITION, albumList.indexOf(it))
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private val broadcastChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
                )
            ) {
                adapter.notifyItemRangeChanged(2, albumList.size - 1)
            }
            if (action == Utils.ACTION_FINISH_DOWNLOAD) {
                reloadData()
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadData()
        binding.apply {
            layoutRefresh.setOnRefreshListener {
                reloadData()
            }
            allAlbums.layoutManager = GridLayoutManager(requireContext(), 2)

            allAlbums.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    adapter.lastVisibleItemPosition = lastVisibleItemPosition
                    scrollToTop.isVisible = firstVisibleItemPosition > 0
                }
            })
            scrollToTop.setOnClickListener {
                allAlbums.smoothScrollToPosition(0)
            }
            verticalScrollbar.apply {
                attachTo(allAlbums)
                customTrackDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.scrollbar_custom_track)
            }
        }

    }

    override fun reloadData() {
        if (MainActivity.isChangeTheme) {
            return
        }

        binding.layoutRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            albumList.clear()
            val allAlbums = SongLoader.getAlbumLocal(requireContext())
            allAlbums.onEach { album ->
                if (SongLoader.getSongAlbumById(album.albumId.toLong(), requireContext())
                        .isNotEmpty()
                ) {
                    albumList.add(album)
                }
            }

            withContext(Dispatchers.Main) {
                binding.allAlbums.adapter = adapter
                if (context != null) {
                    adapter.updateData(albumList.distinct())
                    binding.layoutRefresh.isRefreshing = false
                }
            }

        }

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

}
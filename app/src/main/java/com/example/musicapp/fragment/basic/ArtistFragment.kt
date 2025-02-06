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
import com.example.musicapp.activity.advance.ArtistActivity
import com.example.musicapp.adapter.basic.SongAdapter
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.databinding.FragmentArtitstBinding
import com.example.scrollbar.attachTo
import com.example.service.model.Artist
import com.example.service.service.MusicService
import com.example.service.service.SongLoader
import com.example.service.utils.DisplayMode
import com.example.service.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistFragment : BaseFragment<FragmentArtitstBinding>(FragmentArtitstBinding::inflate) {
    private val artistAdapter = SongAdapter(DisplayMode.ARTIST, setOnClickListener = {}, onClickItem = {
        val intent = Intent(requireContext(), ArtistActivity::class.java)
        intent.putExtra(ArtistActivity.ARTIST_KEY, Gson().toJson(it))
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }) {
            audio, isSelected ->
        println("SongFragment: $audio and $isSelected")
    }

    private val broadcastChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
                )
            ) {
                artistAdapter.notifyDataSetChanged()
            }
            if (action == Utils.ACTION_FINISH_DOWNLOAD) {
                reloadData()
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadData()
        binding.layoutRefresh.setOnRefreshListener {
            reloadData()
        }
        binding.allArtists.layoutManager = LinearLayoutManager(requireContext())

        binding.allArtists.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                artistAdapter.lastVisibleItemPosition = lastVisibleItemPosition
                binding.scrollToTop.isVisible = firstVisibleItemPosition > 0
            }
        })
        binding.scrollToTop.setOnClickListener {
            binding.allArtists.smoothScrollToPosition(0)
        }

        binding.verticalScrollbar.apply {
            attachTo(binding.allArtists)
            customTrackDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.scrollbar_custom_track)
        }
    }

    override fun reloadData() {
        if(MainActivity.isChangeTheme) {
            return
        }
        binding.layoutRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            val listArtist = SongLoader.getArtistLocal(requireContext())
            val allArtist = mutableListOf<Artist>()
            allArtist.addAll(listArtist)
            withContext(Dispatchers.Main) {
                binding.allArtists.adapter = artistAdapter
                if(context != null) {
                    artistAdapter.updateData(allArtist)
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

    override fun onResume() {
        super.onResume()
        reloadData()
    }

}
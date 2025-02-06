package com.example.musicapp.activity.advance

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.activity.advance.PlaylistActivity.Companion.ACTION_UPDATE_DATA_PLAYLIST
import com.example.musicapp.adapter.advance.AlbumLineAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.bottomsheet.BottomSheetDetailPlaylist
import com.example.musicapp.databinding.ActivityPlaylistDetailBinding
import com.example.scrollbar.attachTo
import com.example.service.model.Playlist
import com.example.service.service.MusicPlayerRemote
import com.example.service.service.MusicService
import com.example.service.utils.DisplayMode
import com.example.service.utils.PlaylistUtils
import com.example.service.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class DetailPlaylistActivity :
    BaseActivity<ActivityPlaylistDetailBinding>(ActivityPlaylistDetailBinding::inflate) {
    private val playingAdapter = AlbumLineAdapter(isPlaylist = true, DisplayMode.PLAYLIST) {
        val intent = Intent(this@DetailPlaylistActivity, AddSongPlaylistActivity::class.java)
        intent.putExtra(KEY_ID_PLAYLIST, idPlaylist)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    private val listDataSong = mutableListOf<Any>()
    private lateinit var playlist: Playlist
    private val idPlaylist: Int by lazy { intent.getIntExtra(KEY_ID_PLAYLIST, -1) }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            println("DetailPlaylistActivity: $action")
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
                )
            ) {
                playingAdapter.notifyDataSetChanged()
            }
            if (action in listOf(Utils.ACTION_FINISH_DOWNLOAD, ACTION_UPDATE_DATA_PLAYLIST) ) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100)
                    loadData()
                }

            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }

        val layoutManager = LinearLayoutManager(this)
        binding.songOfAlbumRV.layoutManager = layoutManager
        val currentSongIndex =
            MusicPlayerRemote.getPlayingQueue().indexOf(MusicPlayerRemote.getCurrentSong())
        layoutManager.scrollToPositionWithOffset(currentSongIndex, 0)


        binding.songOfAlbumRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                playingAdapter.lastVisibleItemPosition = lastVisibleItemPosition

                binding.scrollToTop.isVisible =
                    firstVisibleItemPosition > 0 && recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING
            }
        })

        binding.scrollToTop.setOnClickListener {
            binding.songOfAlbumRV.smoothScrollToPosition(0)
        }

        binding.backBtn.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binding.emptySongBtn.setOnClickListener {
            val intent = Intent(this@DetailPlaylistActivity, AddSongPlaylistActivity::class.java)
            intent.putExtra(KEY_ID_PLAYLIST, idPlaylist)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binding.moreBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val playLists =
                    PlaylistUtils.getInstance(this@DetailPlaylistActivity)?.getPlaylists()
                        ?: arrayListOf()
                withContext(Dispatchers.Main) {
                    val dialogName = BottomSheetDetailPlaylist(this@DetailPlaylistActivity)
                    dialogName.setData(playlist ,idPlaylist, playLists.toMutableList())
                    dialogName.show()
                }
            }
        }


        binding.verticalScrollbar.apply {
            attachTo(binding.songOfAlbumRV)
            customTrackDrawable = ContextCompat.getDrawable(this@DetailPlaylistActivity, R.drawable.scrollbar_custom_track)
        }
    }

    private fun loadData() {
        binding.swipeRefreshLayout.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                playlist = PlaylistUtils.getInstance(this@DetailPlaylistActivity)!!
                    .getPlaylistById(idPlaylist)
                withContext(Dispatchers.Main) {
                    binding.albumName.text = playlist.title
                    binding.songOfAlbumRV.adapter = playingAdapter

                    if (playlist.tracks.isNullOrEmpty()) {
                        binding.songOfAlbumRV.visibility = View.GONE
                        binding.emptyListLayout.visibility = View.VISIBLE
                    } else {
                        binding.songOfAlbumRV.visibility = View.VISIBLE
                        binding.emptyListLayout.visibility = View.GONE
                        playingAdapter.updateData(playlist.tracks, playlist)
                    }
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            } catch (e: Exception) {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                e.printStackTrace()
            }
        }


    }

    override fun setUpMiniPlayer() {
        super.setUpMiniPlayer()
        playingAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
            loadData()
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
        intentFilter.addAction(ACTION_UPDATE_DATA_PLAYLIST)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, intentFilter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiver)
    }


    companion object {
        const val PLAYLIST = "Playlist"
        const val KEY_ID_PLAYLIST = "KEY_ID_PLAYLIST"
    }
}
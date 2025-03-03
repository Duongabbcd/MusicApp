package com.example.musicapp.activity.advance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.adapter.advance.AlbumLineAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.bottomsheet.BottomSheetMore
import com.example.musicapp.databinding.ActivityAlbumBinding
import com.example.musicapp.util.Constant
import com.example.scrollbar.attachTo
import com.example.service.model.Album
import com.example.service.model.Audio
import com.example.service.service.MusicService
import com.example.service.service.SongLoader
import com.example.service.utils.DisplayMode
import com.example.service.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumActivity: BaseActivity<ActivityAlbumBinding>(ActivityAlbumBinding::inflate) {
    private val albumAdapter = AlbumLineAdapter(displayMode = DisplayMode.AUDIO) {

    }
    private val listData = mutableListOf<Audio>()
    private var albumList = listOf<Album>()

    private val album by lazy {
        Gson().fromJson(
            intent.getStringExtra(ALBUM_KEY), Album::class.java
        )
    }

    private val broadcastChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            println("AlbumActivity: $action")
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
                )
            ) {
                albumAdapter.notifyDataSetChanged()
            }
            if (action == Constant.ACTION_FINISH_DOWNLOAD) {
                reloadData()
            }
        }
    }

    private val albumId  by lazy {
        intent.getIntExtra(ALBUM_POSITION, 0)
    }


    private fun reloadData() {
        binding.swipeRefreshLayout.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            val idAlbum = try {
               album.albumId.toLong()
            } catch (e: Exception) {
                0L
            }
            val listSong = SongLoader.getSongAlbumById(idAlbum, this@AlbumActivity)
            println("AlbumActivity: ${listSong.first()}")

            albumList = SongLoader.getAlbumLocal(this@AlbumActivity)
            listData.clear()
            listData.addAll(listSong)
            if (listSong.isEmpty()) {
                return@launch
            }

            withContext(Dispatchers.Main) {
                binding.songOfAlbumRV.adapter = albumAdapter
                albumAdapter.updateData(listData.toList())
                binding.swipeRefreshLayout.isRefreshing = false
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
            registerReceiver(
                broadcastChange, intentFilter, Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(broadcastChange, intentFilter)
        }
        albumAdapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding.apply {
            backBtn.setOnClickListener {
                finish()
                overridePendingTransition(R.anim.fade_in_quick, R.anim.exit_to_top)
            }
            moreBtn.setOnClickListener {
                val bottomSheetMore = BottomSheetMore(this@AlbumActivity, DisplayMode.ALBUM )
                bottomSheetMore.setAlbum(album)
                bottomSheetMore.show()
            }

            reloadData()

            albumName.text = album.albumName
            songOfAlbumRV.layoutManager = LinearLayoutManager(this@AlbumActivity)
            songOfAlbumRV.addOnScrollListener(object : RecyclerView.OnScrollListener(){
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    albumAdapter.lastVisibleItemPosition = lastVisibleItemPosition

                    scrollToTop.isVisible = firstVisibleItemPosition > 0
                }
            })
            scrollToTop.setOnClickListener {
                songOfAlbumRV.smoothScrollToPosition(0)
            }

            swipeRefreshLayout.setOnRefreshListener {
                reloadData()
            }

            songOfAlbumRV.adapter = albumAdapter
            verticalScrollbar.apply {
                attachTo(songOfAlbumRV)
                customTrackDrawable = ContextCompat.getDrawable(this@AlbumActivity, R.drawable.scrollbar_custom_track)
            }
        }
    }

    override fun setUpMiniPlayer() {
        super.setUpMiniPlayer()
        albumAdapter.notifyItemRangeChanged(1, listData.size - 1)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastChange)
    }

    override fun onResume() {
        super.onResume()
        reloadData()
        albumAdapter.notifyItemRangeChanged(1, listData.size - 1)
    }


    companion object {
        const val ALBUM_KEY = "KEY_ALBUM"
        const val ALBUM_POSITION = "ALBUM_POSITION"
    }
}
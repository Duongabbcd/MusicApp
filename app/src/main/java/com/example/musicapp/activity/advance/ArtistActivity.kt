package com.example.musicapp.activity.advance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.adapter.advance.AlbumLineAdapter
import com.example.musicapp.adapter.advance.AlbumListAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.bottomsheet.BottomSheetMore
import com.example.musicapp.databinding.ActivityArtistBinding
import com.example.musicapp.util.Constant
import com.example.scrollbar.attachTo
import com.example.service.model.Album
import com.example.service.model.Artist
import com.example.service.model.Audio
import com.example.service.service.MusicService
import com.example.service.service.SongLoader
import com.example.service.utils.DisplayMode
import com.example.service.utils.Utils
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class ArtistActivity : BaseActivity<ActivityArtistBinding>(ActivityArtistBinding::inflate) {
    private val allSongAdapter = AlbumLineAdapter(displayMode = DisplayMode.AUDIO) {

    }
    private val allAlbumAdapter = AlbumListAdapter {
        val intent = Intent(this, AlbumActivity::class.java)
        intent.putExtra(AlbumActivity.ALBUM_KEY, Gson().toJson(it))
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    private val listData = mutableListOf<Audio>()
    private var artistList = listOf<Artist>()
    private var allAlbums = listOf<Album>()
    private var isOnlyOneSong = false

    private val artist by lazy {
        Gson().fromJson(
            intent.getStringExtra(ARTIST_KEY), Artist::class.java
        )
    }

    private val broadcastChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
                )
            ) {
                allSongAdapter.notifyDataSetChanged()
                allAlbumAdapter.notifyItemRangeChanged(0, allAlbums.size)
            }
            if (action == Utils.ACTION_FINISH_DOWNLOAD) {
                reloadData()
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
        allSongAdapter.notifyItemRangeChanged(0, listData.size)
        allAlbumAdapter.notifyItemRangeChanged(0, allAlbums.size)
    }


    private fun reloadData() {
        try {
            val idArtist = artist.artistId.toLong()

            binding.swipeRefreshLayout.isRefreshing = true
            CoroutineScope(Dispatchers.IO).launch {
                val listSong = SongLoader.getSongByArtistId(idArtist, this@ArtistActivity)
                if(listSong.isEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        finish()
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                    return@launch

                }
                isOnlyOneSong = listSong.isEmpty()
                allAlbums = SongLoader.getAlbumLocal(this@ArtistActivity)
                    .filter {
                        it.artistKey == artist.nameArtist
                    }
                artistList = SongLoader.getArtistLocal(this@ArtistActivity)
                listData.clear()
                listData.addAll(listSong)

                withContext(Dispatchers.Main) {
                    binding.artistDetailRV.adapter = allSongAdapter
                    binding.allAlbums.adapter = allAlbumAdapter

                    allSongAdapter.updateData(listData.toList())
                    if (allAlbums.isNullOrEmpty()) {
                        binding.allAlbums.visibility = View.GONE
                        binding.allAlbumTotal.visibility = View.GONE
                    }
                    allAlbumAdapter.updateData(allAlbums.toList())

                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            reloadData()
            swipeRefreshLayout.setOnRefreshListener {
                reloadData()
            }

            val quantityString = this@ArtistActivity.resources.getQuantityString(
                R.plurals.album_count,
                artist.numberOfAlbum.toInt(),
                artist.numberOfAlbum.toInt()
            )
            val firstArtistName = artist.nameArtist[0]
            val artistName = if (!firstArtistName.isLetter() || artist.nameArtist.contains(
                    Constant.UNKNOWN_STRING,
                    true
                )
            ) this@ArtistActivity.resources.getString(R.string.unknown_artist) else artist.nameArtist
            allAlbumTotal.text = quantityString
            artistFullName.text = artistName

            if (firstArtistName.isLetter()) {
                artistFirstName.text = firstArtistName.toString()
                artistFirstName.visibility = View.VISIBLE
                anonymousIcon.visibility = View.GONE
            } else if (!firstArtistName.isLetter() || artist.nameArtist.contains(
                    Constant.UNKNOWN_STRING,
                    true
                )
            ) {
                artistFirstName.visibility = View.GONE
                artistIcon.setBackgroundResource(R.color.transparent)
                anonymousIcon.visibility = View.VISIBLE
            } else {
                artistFirstName.visibility = View.GONE
                artistIcon.setBackgroundResource(R.color.transparent)
                anonymousIcon.visibility = View.VISIBLE
            }

            backBtn.setOnClickListener {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            allAlbums.layoutManager =
                LinearLayoutManager(this@ArtistActivity, LinearLayoutManager.HORIZONTAL, false)

            appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                val isExpand = abs(verticalOffset) != appBarLayout.totalScrollRange
                expandLayout.isVisible = isExpand

                if (isExpand) {
                    nameOfArtist.text = ""
                } else {
                    nameOfArtist.text = artistName
                    toolbar.setBackgroundColor(Color.TRANSPARENT)
                }
            })

            moreBtn.setOnClickListener {
                val bottomSheetMore = BottomSheetMore(this@ArtistActivity, DisplayMode.ARTIST )
                bottomSheetMore.setArtist(0, artistList, artist = artist)
                bottomSheetMore.show()
            }


            binding.scrollToTop.setOnClickListener {
                binding.artistDetailRV.smoothScrollToPosition(0)
            }

            artistDetailRV.addOnScrollListener(object : RecyclerView.OnScrollListener(){
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    allSongAdapter.lastVisibleItemPosition = lastVisibleItemPosition

                    scrollToTop.isVisible = firstVisibleItemPosition > 0
                }
            })

            verticalScrollbar.apply {
                attachTo(artistDetailRV)
                customTrackDrawable = ContextCompat.getDrawable(this@ArtistActivity, R.drawable.scrollbar_custom_track)
            }

            horizontalScrollbar.apply {
                attachTo(allAlbums)
                customTrackDrawable = ContextCompat.getDrawable(this@ArtistActivity, R.drawable.scrollbar_custom_track)
            }
        }
    }

    override fun setUpMiniPlayer() {
        super.setUpMiniPlayer()
        allAlbumAdapter.notifyItemRangeChanged(0, allAlbums.size)
        allSongAdapter.notifyItemRangeChanged(0, listData.size)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastChange)
    }

    override fun onResume() {
        super.onResume()
        allAlbumAdapter.notifyItemRangeChanged(0, allAlbums.size)
        allSongAdapter.notifyItemRangeChanged(0, listData.size)
    }


    companion object {
        const val ARTIST_KEY = "ARTIST_KEY"
        const val ARTIST_POSITION = "ARTIST_POSITION"
    }
}
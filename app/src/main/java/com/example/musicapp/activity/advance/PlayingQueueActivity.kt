package com.example.musicapp.activity.advance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.adapter.SongPlayingAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivityPlayingQueueBinding
import com.example.musicapp.dialog.DeleteDialog
import com.example.musicapp.dialog.DeleteMode
import com.example.musicapp.util.Utils.setOnSWipeListener
import com.example.service.service.MusicPlayerRemote
import com.example.service.service.MusicService
import com.example.service.service.MusicService.Companion.SHUFFLE_MODE_NONE
import com.example.service.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayingQueueActivity :
    BaseActivity<ActivityPlayingQueueBinding>(ActivityPlayingQueueBinding::inflate) {
    private val playingAdapter = SongPlayingAdapter()
    private val listDataSong = mutableListOf<Any>()
    private val broadcastChange = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == MusicService.PLAY_STATE_CHANGED.replace(
                    MusicService.MUSIC_PLAYER_PACKAGE_NAME, MusicService.MUSIC_PACKAGE_NAME
                ) || action == Utils.ACTION_FINISH_DOWNLOAD
            ) {
                reloadData()
                displaySummaryPlayingQueue()
                playingAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun reloadData() {
        CoroutineScope(Dispatchers.IO).launch {
            val listSong = MusicPlayerRemote.getPlayingQueue()
            listDataSong.clear()
            listDataSong.addAll(listSong)

            withContext(Dispatchers.Main) {
                binding.playingQueueSong.adapter = playingAdapter
                playingAdapter.updateData(listDataSong)
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
        playingAdapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentSong = MusicPlayerRemote.getCurrentSong()
        println("PlayingQueueActivity: ${MusicPlayerRemote.getCurrentSong().mediaObject?.title}")
        displaySummaryPlayingQueue()
        reloadData()
        checkShuffle()
        binding.playingQueueSongName.text = currentSong.mediaObject?.title ?: ""
        binding.playingQueueArtist.text = currentSong.artist
        binding.playingQueueSong.adapter = playingAdapter

        val itemTouchHelper = ItemTouchHelper(DragManageAdapter(playingAdapter))
        itemTouchHelper.attachToRecyclerView(binding.playingQueueSong)

        // Assuming you have a RecyclerView named recyclerView and a LinearLayoutManager
        val layoutManager = LinearLayoutManager(this)
        binding.playingQueueSong.layoutManager = layoutManager
        val currentSongIndex =
            MusicPlayerRemote.getPlayingQueue().indexOf(currentSong)
        layoutManager.scrollToPositionWithOffset(currentSongIndex, 0)


        binding.playingQueueSong.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
            binding.playingQueueSong.smoothScrollToPosition(0)
        }

        binding.root.setOnSWipeListener(this)
        binding.root.setOnClickListener {

        }
    }

    private fun displaySummaryPlayingQueue() {
        binding.summaryLine.apply {
            val listSong = MusicPlayerRemote.getPlayingQueue()
            if(listSong.isEmpty()) {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            playingDelete.setOnClickListener {
                val dialogName = DeleteDialog(
                    this@PlayingQueueActivity,
                    DeleteMode.QUEUE
                )
                dialogName.setData(listSong)
                dialogName.show()
            }

            playingShuffle.setOnClickListener {
                MusicPlayerRemote.toggleShuffleMode()
                checkShuffle()
                playingAdapter.updateData(MusicPlayerRemote.getPlayingQueue())
            }
            val currentSongIndex = listSong.indexOf(MusicPlayerRemote.getCurrentSong()) + 1
            val totalSongs = listSong.size
            val quantityString =
                this@PlayingQueueActivity.resources.getQuantityString(
                    R.plurals.playing_song_queue, totalSongs, currentSongIndex,
                    totalSongs, currentSongIndex, totalSongs, currentSongIndex
                )
            playingCurrentTrack.text = quantityString
        }
    }

    private fun checkShuffle() {
        val shuffleMode = MusicPlayerRemote.getShuffleMode()
        val resource = if(shuffleMode == SHUFFLE_MODE_NONE)
            R.drawable.icon_gray_shuffle
        else R.drawable.icon_highlight_shuffle
        binding.summaryLine.playingShuffle.setImageResource(resource)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastChange)
    }

    inner class DragManageAdapter(
        private val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    ) : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            //Allow drag upward or downward
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            return makeMovementFlags(dragFlags, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            adapter.notifyItemMoved(fromPosition, toPosition)
            MusicPlayerRemote.moveSong(fromPosition, toPosition)
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            try {
                if (!recyclerView.isComputingLayout && !recyclerView.isAnimating) {
                    playingAdapter.updateData(MusicPlayerRemote.getPlayingQueue())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            TODO("Not yet implemented")
        }
    }

}
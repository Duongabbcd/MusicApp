package com.example.musicapp.activity.advance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.activity.advance.search.SearchSongActivity
import com.example.musicapp.adapter.NavigationItemModel
import com.example.musicapp.adapter.NavigationRVAdapter
import com.example.musicapp.adapter.PlaylistAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivityPlaylistBinding
import com.example.musicapp.dialog.NewPlayListDialog
import com.example.musicapp.util.Utils.hideKeyBoard
import com.example.scrollbar.attachTo
import com.example.service.utils.PlaylistUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.service.model.Playlist

class PlaylistActivity : BaseActivity<ActivityPlaylistBinding>(ActivityPlaylistBinding::inflate) {
    private val playlistAdapter = PlaylistAdapter {
        val intent = Intent(binding.root.context, DetailPlaylistActivity::class.java)
        intent.putExtra(DetailPlaylistActivity.KEY_ID_PLAYLIST, it.id)
        binding.root.context.startActivity(intent)
        this.overridePendingTransition(
            R.anim.fade_in,
            R.anim.fade_out
        )
    }
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationAdapter: NavigationRVAdapter
    private var playList = listOf<Playlist>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reloadData()
        initView()
        binding.createNewPlaylistLayout.setOnClickListener {
            val dialogName = NewPlayListDialog(this)
            dialogName.setOnReloadData {
                reloadData()
            }
            dialogName.show()
        }

        binding.layoutRefresh.setOnRefreshListener {
            reloadData()
        }

        binding.scrollToTop.setOnClickListener {
            binding.allPlaylistRV.scrollToPosition(0)
        }

        binding.allPlaylistRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                this@PlaylistActivity.hideKeyBoard(binding.searchBarText)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                playlistAdapter.lastVisibleItemPosition = lastVisibleItemPosition

                binding.scrollToTop.isVisible = firstVisibleItemPosition > 0
            }
        })

        binding.searchBarIcon.visibility = View.GONE
        binding.searchBarIconLeft.visibility = View.VISIBLE

        binding.searchBarText.addTextChangedListener { s ->
            val text = s.toString()
            if (text.isNotEmpty()) {
                binding.searchBarIcon.visibility = View.VISIBLE
                binding.searchBarIconLeft.visibility = View.GONE
                binding.searchBarIcon.setImageResource(R.drawable.icon_close)
                val list = playList.filter { it.title.contains(text, ignoreCase = true) }
                playlistAdapter.updateData(list)
            } else {
                binding.searchBarIcon.setImageResource(R.drawable.icon_search_purple)
                playlistAdapter.updateData(playList)
            }
        }

        binding.searchBarIcon.setOnClickListener {
            binding.searchBarText.setText("")
        }

        binding.verticalScrollbar.apply {
            attachTo(binding.allPlaylistRV)
            customTrackDrawable = ContextCompat.getDrawable(this@PlaylistActivity, R.drawable.scrollbar_custom_track)
        }
    }

    private fun initView() {
        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        binding.toolbarIcon.setOnClickListener {
            binding.main.openDrawer(GravityCompat.START)
        }

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root
        toggle.syncState()
        updateAdapter()
        binding.drawerFunctions.layoutManager = LinearLayoutManager(this)
        binding.drawerFunctions.setHasFixedSize(true)
    }

    private fun chooseTheNextFunction(position: Int) {
        when (position) {
            0 -> {
                val intent = Intent(this@PlaylistActivity, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

            }

            1 -> {
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.main.closeDrawer(GravityCompat.START)
        }, 200)
    }

    private fun updateAdapter() {
        val items = arrayListOf(
            NavigationItemModel(R.drawable.icon_music, getString(R.string.my_music)),
            NavigationItemModel(R.drawable.icon_playlist, getString(R.string.my_playlist)),
        )

        navigationAdapter = NavigationRVAdapter(items) { position ->
            chooseTheNextFunction(position)
        }
        binding.drawerFunctions.adapter = navigationAdapter
        navigationAdapter.notifyItemRangeChanged(0,2)
    }

    private val updateBroadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == ACTION_UPDATE_DATA_PLAYLIST) {
                playlistAdapter.notifyDataSetChanged()
                reloadData()
            }
        }
    }

    private fun reloadData() {
        if (MainActivity.isChangeTheme) {
            return
        }
        binding.layoutRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            playList =
                PlaylistUtils.getInstance(this@PlaylistActivity)?.getPlaylists() ?: arrayListOf()
            val searchBarText =  binding.searchBarText.text
            val selectedPlaylists = if(searchBarText.isNullOrEmpty()) playList else playList.filter { it.title.contains(searchBarText) }
            withContext(Dispatchers.Main) {
                binding.allPlaylistRV.adapter = playlistAdapter
                playlistAdapter.updateData(selectedPlaylists)
                binding.layoutRefresh.isRefreshing = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ACTION_UPDATE_DATA_PLAYLIST)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                updateBroadcast, intentFilter, Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(updateBroadcast, intentFilter)
        }
        reloadData()
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(updateBroadcast)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        const val ACTION_UPDATE_DATA_PLAYLIST = "ACTION_UPDATE_DATA_PLAYLIST"
    }
}
package com.example.musicapp.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.musicapp.R
import com.example.musicapp.activity.advance.PlaylistActivity
import com.example.musicapp.adapter.NavigationItemModel
import com.example.musicapp.adapter.NavigationRVAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivityMainBinding
import com.example.musicapp.databinding.BottomSheetExitAppBinding
import com.example.musicapp.fragment.basic.AlbumFragment
import com.example.musicapp.fragment.basic.ArtistFragment
import com.example.musicapp.fragment.basic.DownloadFragment
import com.example.musicapp.fragment.basic.HomeFragment
import com.example.musicapp.fragment.basic.SongFragment
import com.example.musicapp.viewpager.ViewPagerAdapter
import com.example.service.download.get.DownloadManager
import com.example.service.download.service.DownloadManagerService
import com.example.service.service.MusicPlayerRemote
import com.example.service.service.MusicService
import com.example.service.service.notification.PlayingNotificationImpl24.Companion.bitmapDefault
import com.example.service.utils.PermissionUtils
import com.example.service.utils.PlaylistUtils
import com.example.service.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import android.os.Handler
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.example.musicapp.activity.advance.search.SearchSongActivity
import com.example.service.download.service.DownloadManagerService.listUrlDownloading
import com.example.service.service.SongLoader

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    private val musicPlayerRemote = MusicPlayerRemote
    private lateinit var toggle: ActionBarDrawerToggle
    private var serviceToken: MusicPlayerRemote.ServiceToken? = null

    private lateinit var navigationAdapter: NavigationRVAdapter

    private val serviceConnect: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mBinder = service as DownloadManagerService.DMBinder
            mManager = mBinder!!.downloadManager
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        bitmapDefault = BitmapFactory.decodeResource(
            resources,
            com.example.service.R.drawable.music_player_icon_slash_screen
        )

        if (!PermissionUtils.havePermission(this)) {
            PermissionUtils.requestPermission(this)
        }

        try {
            val intent = Intent()
            intent.setClass(this, DownloadManagerService::class.java)
            startService(intent)
            bindService(intent, serviceConnect, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        CoroutineScope(Dispatchers.IO).launch {
            PlaylistUtils.getInstance(this@MainActivity)?.getPlaylists()
        }
        initServicePlay()
        initView()
    }

    override fun onStart() {
        super.onStart()
        if (isChangeTheme) {
            recreate()
        }
    }


    private fun initView() {
        // Set up the toolbar
        binding.toolbarIcon.setOnClickListener {
            binding.main.openDrawer(GravityCompat.START)
        }

        binding.fab.setOnClickListener {
            val listDataSong = SongLoader.getAllSongs(this)
            MusicPlayerRemote.openQueue(ArrayList(listDataSong), 0, true)
        }

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        binding.root
        toggle.syncState()
        updateAdapter()
        setViewPager()
        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
               binding.fab.visibility = if(position == 4) {
                   View.VISIBLE
               } else View.GONE
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        })

    }

    private fun setViewPager() {
        val fragments: ArrayList<Fragment> = arrayListOf(
            HomeFragment(),
            DownloadFragment(),
            ArtistFragment(),
            AlbumFragment(),
            SongFragment(),
        )

        val adapter = ViewPagerAdapter(items = fragments, this)

        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 5

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> getString(R.string.home)
                1 -> getString(R.string.download)
                2 -> getString(R.string.artist)
                3 -> getString(R.string.album)
                4 -> getString(R.string.song)
                else -> null
            }
        }.attach()
    }

    private fun chooseTheNextFunction(position: Int) {
        when (position) {

            0 -> {
            }

            1 -> {
                val intent = Intent(this@MainActivity, PlaylistActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.main.closeDrawer(GravityCompat.START)
        }, 200)
    }

    private fun updateAdapter() {
        val items = arrayListOf(
            NavigationItemModel(R.drawable.icon_music, getString(R.string.my_music)),
            NavigationItemModel(R.drawable.icon_playlist, getString(R.string.my_playlist))
        )

        navigationAdapter = NavigationRVAdapter(items) { position ->
            chooseTheNextFunction(position)
        }
        binding.drawerFunctions.adapter = navigationAdapter
        navigationAdapter.notifyItemRangeChanged(0, 2)
    }

    private fun initServicePlay() {
        serviceToken = musicPlayerRemote.bindToService(this, object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
            }

        })
    }

    override fun onDestroy() {
        if (isChangeTheme) {
            isChangeTheme = false
        } else {
            try {
                unbindService(serviceConnect)
            } catch (e: Exception) {
                Log.e("unbindService", "$e")
            }

            try {
                musicPlayerRemote.unbindFromService(serviceToken)
            } catch (e: Exception) {
                Log.e("unbindFromService", "$e")
            }
            try {
                val intent = Intent(this, MusicService::class.java)
                stopService(intent)

            } catch (e: Exception) {
                Log.e("stopService", "$e")
            }
        }

        super.onDestroy()
    }

    override fun onBackPressed() {
        val bottomSheetExitAppBinding = BottomSheetExitAppBinding.inflate(
            layoutInflater
        )
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetExitAppBinding.root)
        bottomSheetExitAppBinding.apply {
            textCancel.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
            textExit.setOnClickListener {
                moveTaskToBack(true)
                bottomSheetDialog.dismiss()
            }
        }
        bottomSheetDialog.show()
    }

    override fun setUpMiniPlayer() {
        super.setUpMiniPlayer()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
        }
    }

    companion object {
        var isChangeTheme = false
        private var mBinder: DownloadManagerService.DMBinder? = null
        private var mManager: DownloadManager? = null


        fun startMission(
            url: String?,
            name: String,
            id: String?,
            source: String?,
            referer: String?,
            context: Context
        ) {
            if (mManager == null || mBinder == null) {
                return
            }
            val currentSong = MusicPlayerRemote.getCurrentSong()
            val message = context.resources.getString(
                R.string.download_song_notification,
                currentSong.mediaObject?.title ?: ""
            )
            val notification = context.resources.getString(R.string.download_song_conclusion)
            val res: Int = mManager!!.startMission(
                url, generateUniqueFileNameMp3(formatMp3FileName(name)),
                id, source, referer, message, notification
            )

            listUrlDownloading.add(url)
            mBinder!!.onMissionAdded(
                mManager!!.getMission(res)
            )
        }


        private fun generateUniqueFileNameMp3(baseName: String): String {
            var fileName = baseName
            var suffix = 0
            val extension = ".mp3"
            var targetFile = File(Utils.getPathDownload(), fileName + extension)
            while (targetFile.exists()) {
                suffix++
                fileName = "$baseName-$suffix"
                targetFile = File(Utils.getPathDownload(), fileName + extension)
            }
            return fileName + extension
        }


        private fun formatMp3FileName(fileName: String): String {
            var mp3fileName = fileName

            mp3fileName = mp3fileName.replace("/", "\\")
            mp3fileName = mp3fileName.replace(":", "-")
            mp3fileName = mp3fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "")
            mp3fileName = mp3fileName.replace("#", "")

            mp3fileName = mp3fileName.replace("Kism", "Kis")
            mp3fileName = mp3fileName.replace("kism", "kis")
            mp3fileName = mp3fileName.replace("KISM", "kis")

            return mp3fileName
        }

    }
}
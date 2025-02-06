package com.example.musicapp.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.LocaleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.activity.advance.PlayingQueueActivity
import com.example.musicapp.activity.basic.PlayerActivity
import com.example.musicapp.util.Constant
import com.example.musicapp.util.Utils.hideKeyBoard
import com.example.service.download.service.DownloadManagerService
import com.example.service.model.Audio
import com.example.service.model.Playlist
import com.example.service.service.MusicPlayerRemote
import com.example.service.service.MusicService
import com.example.service.service.SongLoader
import com.example.service.utils.MDManager
import com.example.service.utils.MusicUtil
import com.example.service.utils.PlaylistUtils
import com.example.service.utils.PreferenceUtil
import com.example.service.utils.RemoveOrRenameFile
import com.example.service.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.abs

typealias Inflate<T> = (LayoutInflater) -> T

@Suppress("DEPRECATION")
abstract class BaseActivity<T : ViewBinding>(private val inflater: Inflate<T>) :
    AppCompatActivity() {
    protected val binding: T by lazy { inflater(layoutInflater) }
    private val removeOrRenameFile = RemoveOrRenameFile()
    val deleteResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            sendBroadcast(Intent(Constant.ACTION_FINISH_DOWNLOAD))
            if (result.resultCode == Activity.RESULT_OK) {
                MusicPlayerRemote.checkAfterDeletePlaying()
                removeAudioNotExist()
            }
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                MusicService.META_CHANGED, MusicService.PLAY_STATE_CHANGED -> {
                    setUpMiniPlayer()
                }

                ACTION_RECREATE -> {
                    recreate()
                }

                ACTION_DELETE_FILE -> {
                    val path = intent.getStringExtra(KEY_ID_SONG)
                    val deletedAudio = SongLoader.getAllSongs(this@BaseActivity)
                        .find { it.mediaObject!!.path == path }
                    val id = deletedAudio?.mediaObject!!.id.toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val pendingIntent = MediaStore.createDeleteRequest(
                            contentResolver, mutableListOf(MusicUtil.getSongFileUri(id))
                        )
                        deleteResultLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } else {
                        removeOrRenameFile.deleteAudio(this@BaseActivity, id)
                        MusicPlayerRemote.checkAfterDeletePlaying()
                        removeAudioNotExist()
                    }
                    sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
                }

                ACTION_RENAME_FILE -> {
                    val id = intent.getIntExtra(KEY_ID_SONG, 0)
                    val pathSong = intent.getStringExtra(KEY_PATH_SONG) ?: ""
                    val newName = intent.getStringExtra(KEY_NAME_SONG) ?: ""
                    removeOrRenameFile.renameAudio(this@BaseActivity, id, pathSong, newName)
                }
            }

        }

    }

    val downloadManagerService = DownloadManagerService()

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        setUpMiniPlayer()
        intentFilter.addAction(MusicService.PLAY_STATE_CHANGED)
        intentFilter.addAction(ACTION_DELETE_FILE)
        intentFilter.addAction(ACTION_RECREATE)
        intentFilter.addAction(ACTION_RENAME_FILE)
        intentFilter.addAction(MusicService.META_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(broadcastReceiver, intentFilter)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        val isDarkMode = (this is PlayerActivity) || PreferenceUtil.getInstance(this)
            ?.getTheme(this) == PreferenceUtil.DARK_THEME

        val flag: Int
        if (isDarkMode) {
            setTheme(R.style.Theme_Night_MusicApp)
            flag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            setTheme(R.style.Theme_Day_MusicApp)
            flag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        }
        changeLanguage()
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = flag

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                    val isDarkMode =
                        (this@BaseActivity is PlayerActivity) || PreferenceUtil.getInstance(this@BaseActivity)
                            ?.getTheme(this@BaseActivity) == PreferenceUtil.DARK_THEME

                    val fl = if (isDarkMode) {

                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                    } else {
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                    window.decorView.systemUiVisibility = fl
                }
            }

        }
        window.navigationBarColor = Color.TRANSPARENT
        window.statusBarColor = Color.TRANSPARENT

//        setToolbarFitSystemWindow()
        setContentView(binding.root)

        if (binding.root is ViewGroup) {
            (binding.root as ViewGroup).children.forEach {
                if (it !is EditText) {
                    it.setOnTouchListener { view, _ ->
                        hideKeyBoard(currentFocus ?: view)
                        false
                    }
                }
            }
        }
    }

    private fun changeLanguage() {
        val sharedPreferences = PreferenceUtil.getInstance(this)
        val language = sharedPreferences?.getLanguage() ?: ""
        var savedLanguage = Locale.getDefault().language
        if (language != "") {
            savedLanguage = language
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(savedLanguage)
            return
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
        }
    }

    private fun setToolbarFitSystemWindow() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            view.updatePadding(top = systemBarsInsets.top)
            insets
        }
    }

    protected open fun setUpMiniPlayer() {
        try {
            val currentSong = MusicPlayerRemote.getCurrentSong()
            if (currentSong == Audio.EMPTY_SONG && currentSong.mediaObject?.path.isNullOrEmpty()) {
                findViewById<View>(R.id.layout_mini_player).isVisible = false
                return
            }
            findViewById<View>(R.id.layout_mini_player).isVisible = true
            val res = if (MusicPlayerRemote.isPlaying()) {
                R.drawable.icon_pause
            } else {
                R.drawable.icon_play
            }

            val imageSongAvt = findViewById<ImageView>(R.id.miniTrack)
            val imagePlayPause = findViewById<ImageView>(R.id.miniPlaying)
            val textSongTitle = findViewById<TextView>(R.id.miniSongName)
            val textArtist = findViewById<TextView>(R.id.miniArtist)
            val layoutMiniPlayer = findViewById<ConstraintLayout>(R.id.layout_mini_player)
            val imagePlaylist = findViewById<ImageView>(R.id.playerMoreFunctions)

            Glide.with(this).load(currentSong.mediaObject?.imageThumb)
                .placeholder(R.drawable.icon_track).error(R.drawable.icon_track)
                .into(imageSongAvt)
            imagePlayPause.setImageResource(res)
            textSongTitle.text =
                currentSong.mediaObject?.title ?: resources.getString(R.string.unknown_song)
            val artist = if (currentSong.artist[0].isLetter() || currentSong.artist.contains(
                    Constant.UNKNOWN_STRING,
                    true
                )
            ) resources.getString(R.string.unknown_artist) else currentSong.artist
            textArtist.text = artist
            textSongTitle.isSelected = true

            imagePlayPause.setOnClickListener {
                if (MusicPlayerRemote.isPlaying()) {
                    MusicPlayerRemote.pauseSong()
                } else {
                    MusicPlayerRemote.resumePlaying()
                }
                setUpMiniPlayer()
            }
            layoutMiniPlayer.setOnClickListener {
                startActivity(Intent(this@BaseActivity, PlayerActivity::class.java))
                overridePendingTransition(R.anim.enter_from_bot, R.anim.fade_out)
            }
            layoutMiniPlayer.setOnSWipeListener(this@BaseActivity)
            //online later
            if (currentSong.isOnline) {
                imagePlaylist.setImageResource(R.drawable.icon_download)
            } else {
                imagePlaylist.setImageResource(R.drawable.icon_more_functions)
            }

            imagePlaylist.setOnClickListener {
                if (currentSong.isOnline) {
                    if (DownloadManagerService.listUrlDownloading.contains(currentSong.mediaObject?.path)) {
                        MDManager.getInstance(this@BaseActivity)?.showMessage(
                            this@BaseActivity,
                            this@BaseActivity.resources.getString(
                                R.string.download_song_notification,
                                currentSong.mediaObject?.title
                            )
                        )
                    } else {
                        MainActivity.startMission(
                            currentSong.mediaObject!!.path,
                            currentSong.mediaObject!!.title,
                            System.currentTimeMillis().toString(),
                            currentSong.videoType,
                            currentSong.ccmixterReferrer,this
                        )
                    }
                } else {
                    startActivity(Intent(this@BaseActivity, PlayingQueueActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }
            }
        } catch (e: Exception) {
            Log.e("BaseActivity", "$e")
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    class SwipeGestureListener(private val activity: Activity) :
        GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                val diffY = e2.y.minus(e1!!.y)
                val diffX = e2.x.minus(e1.x)

                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        return true
                    }
                } else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY <= 0) {
                        onSwipeUp()
                    }
                    return true
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            return false
        }

        private fun onSwipeUp() {
            activity.startActivity(Intent(activity, PlayerActivity::class.java))
            activity.overridePendingTransition(R.anim.enter_from_bot, R.anim.fade_out)
        }

        private fun onSwipeLeft() {
            MusicPlayerRemote.playNextSong()
        }

        private fun onSwipeRight() {
            MusicPlayerRemote.playPreviousSong()
        }
    }

    private fun View.setOnSWipeListener(activity: Activity) {
        val gestureDetector = GestureDetector(activity, SwipeGestureListener(activity))
        this.setOnTouchListener() { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiver)
    }

    private fun removeAudioNotExist() {
        CoroutineScope(Dispatchers.IO).launch {
            //modify in playlist
            val playLists: List<Playlist>? =
                PlaylistUtils.getInstance(this@BaseActivity)?.getPlaylists()
            playLists?.let {
                it.forEach { playlist ->
                    var isChange = false
                    val tempTrack = mutableListOf<Audio>()
                    tempTrack.addAll(playlist.tracks)
                    tempTrack.forEach { audio ->
                        if (!File(audio.mediaObject?.path).exists()) {
                            isChange = true
                            playlist.tracks.remove(audio)
                        }
                    }
                    if (isChange) {
                        PlaylistUtils.getInstance(context = this@BaseActivity)
                            ?.savePlaylist(playlist)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_DELETE_FILE = "ACTION_DELETE_FILE"
        const val ACTION_RENAME_FILE = "ACTION_RENAME_FILE"
        const val KEY_ID_SONG = "KEY_ID_SONG"
        const val KEY_PATH_SONG = "KEY_PATH_SONG"
        const val KEY_NAME_SONG = "KEY_NAME_SONG"
        const val ACTION_RECREATE = "ACTION_RECREATE"
    }
}
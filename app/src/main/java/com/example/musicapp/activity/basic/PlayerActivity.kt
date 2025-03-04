package com.example.musicapp.activity.basic

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.activity.advance.PlayingQueueActivity
import com.example.musicapp.activity.advance.PlaylistActivity
import com.example.musicapp.activity.advance.PlaylistActivity.Companion.ACTION_UPDATE_DATA_PLAYLIST
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.bottomsheet.BottomSheetAddPlaylist
import com.example.musicapp.bottomsheet.BottomTimeSleepOptions
import com.example.musicapp.databinding.ActivityPlayerBinding
import com.example.musicapp.util.Utils
import com.example.musicapp.util.Utils.setOnSWipeListener
import com.example.service.download.service.DownloadManagerService.listUrlDownloading
import com.example.service.service.MusicPlayerRemote
import com.example.service.service.MusicService
import com.example.service.utils.MDManager
import com.example.service.utils.PlaylistUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : BaseActivity<ActivityPlayerBinding>(ActivityPlayerBinding::inflate) {

    private lateinit var thumbView: View
    private val broadcastPlayerMusic = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            println("PlayerActivity intent.action: $action")
            when (action) {
                MusicService.META_CHANGED, MusicService.PLAY_STATE_CHANGED -> {
                    setUpPlayer()
                }

                MusicService.NEW_SONG_CHANGED -> {
                    updateImage()
                }

                ACTION_UPDATE_DATA_PLAYLIST -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(100)
                        updateFavourite()
                    }
                }
            }
        }

    }

    private fun setUpPlayer() {
        val currentAudio = MusicPlayerRemote.getCurrentSong()
        MusicPlayerRemote.setCurrentAudio(currentAudio)
        binding.playingQueueSongName.isSelected = true
        binding.playingQueueSongName.text = currentAudio.mediaObject?.title ?: ""
        binding.playingQueueArtist.text = currentAudio.artist


        val res = if (MusicPlayerRemote.isPlaying()) {
            rotateAnimation.resume()
            R.drawable.icon_track_pause
        } else {
            rotateAnimation.pause()
            R.drawable.icon_play_song
        }

        binding.playingFavourite.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val isFavourite = PlaylistUtils.getInstance(this@PlayerActivity)
                    ?.checkIsFavourite(MusicPlayerRemote.getCurrentSong())
                if (isFavourite == true) {
                    PlaylistUtils.getInstance(this@PlayerActivity)
                        ?.removeFromFavourite(MusicPlayerRemote.getCurrentSong())
                } else {
                    PlaylistUtils.getInstance(this@PlayerActivity)?.addToFavourite(
                        MusicPlayerRemote.getCurrentSong()
                    )
                }
                updateFavourite()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            updateFavourite()
        }

        //online later
        if (MusicPlayerRemote.getCurrentSong().isOnline) {
            binding.playerMoreFunctions.setImageResource(R.drawable.icon_download)
        } else {
            binding.playerMoreFunctions.setImageResource(R.drawable.icon_add_queue)
        }

        val resRepeat = when (MusicPlayerRemote.getRepeatMode()) {
            MusicService.REPEAT_MODE_ALL -> R.drawable.icon_white_duplicate
            MusicService.REPEAT_MODE_THIS -> R.drawable.icon_white_repeat_one
            else -> R.drawable.icon_white_no_repeat
        }

        binding.playingDuplicate.setImageResource(resRepeat)

        binding.playingDuplicate.setOnClickListener {
            MusicPlayerRemote.cycleRepeatMode()
            val resourceRepeat = when (MusicPlayerRemote.getRepeatMode()) {
                MusicService.REPEAT_MODE_ALL -> R.drawable.icon_white_duplicate
                MusicService.REPEAT_MODE_THIS -> R.drawable.icon_white_repeat_one
                else -> R.drawable.icon_white_no_repeat
            }

            binding.playingDuplicate.setImageResource(resourceRepeat)
        }

        val resShuffle = checkShuffle()

        binding.playingShuffle.setImageResource(resShuffle)

        binding.playingShuffle.setOnClickListener {
            MusicPlayerRemote.toggleShuffleMode()
            val resourceShuffle = checkShuffle()

            binding.playingShuffle.setImageResource(resourceShuffle)
        }

        binding.playingAlarm.setOnClickListener {
            val dialog = BottomTimeSleepOptions(context = this@PlayerActivity)
            dialog.show()
        }

        binding.playingPower.setImageResource(res)

        binding.playingSeekbar.max = MusicPlayerRemote.getSongDurationMillis()
        binding.playingSeekbar.progress = MusicPlayerRemote.getSongProgressMillis().toInt()

        binding.playerMoreFunctions.setOnClickListener {
            val currentSong = MusicPlayerRemote.getCurrentSong()
            if (currentSong.isOnline) {
                if (listUrlDownloading.contains(currentSong.mediaObject?.path)) {
                    MDManager.getInstance(this)
                        ?.showMessage(
                            this@PlayerActivity,
                            resources.getString(R.string.downloading_song)
                        )
                } else {
                    MainActivity.startMission(
                        currentSong.mediaObject!!.path,
                        currentSong.mediaObject!!.title,
                        System.currentTimeMillis().toString(),
                        currentSong.videoType,
                        currentSong.ccmixterReferrer,
                        this
                    )
                }
            } else {
                startActivity(Intent(this@PlayerActivity, PlayingQueueActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

            }
        }
    }

    private fun checkShuffle(): Int {
        return if (MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE) {
            R.drawable.icon_white_shuffle_on
        } else R.drawable.icon_white_shuffle
    }

    private suspend fun updateFavourite() {
        val isFavourite =
            PlaylistUtils.getInstance(this)?.checkIsFavourite(MusicPlayerRemote.getCurrentSong())
        println("updateFavourite: $isFavourite")
        val res = if (isFavourite == true) {
            R.drawable.icon_liked
        } else {
            R.drawable.icon_white_favourite
        }
        withContext(Dispatchers.Main) {
            binding.playingFavourite.setImageResource(res)
        }
    }

    private val rotateAnimation: ObjectAnimator by lazy { createRotateAnimation(binding.playingSongIcon) }

    private fun createRotateAnimation(playingSongIcon: ImageView): ObjectAnimator {
        return ObjectAnimator.ofFloat(playingSongIcon, "rotation", 0f, 360f).apply {
            duration = 10000 //Time for each completed round.
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }

    }

    private fun updateImage() {
        rotateAnimation.cancel()
        binding.playingSongIcon.rotation = 0f
        rotateAnimation.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scope = CoroutineScope(Dispatchers.Main)
        setUpPlayer()
        thumbView = LayoutInflater.from(this).inflate(R.layout.player_thumb_background, null, false)

        val textView = thumbView.findViewById<TextView>(R.id.tvProgress)
        scope.launch {
            while (isActive) {
                val progress = MusicPlayerRemote.getSongProgressMillis().toInt()
                val max = MusicPlayerRemote.getSongDurationMillis()
                binding.playingSeekbar.thumb = Utils.getThumb(
                    progress, max, textView, thumbView, resources
                )
                binding.playingSeekbar.progress = MusicPlayerRemote.getSongProgressMillis().toInt()
                delay(100)
            }
        }

        binding.root.setOnSWipeListener(this)
        binding.root.setOnClickListener {

        }

        binding.playingSeekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                MusicPlayerRemote.seekTo(seekBar?.progress ?: 0)
            }

        })

        binding.playingPower.setOnClickListener {
            if (MusicPlayerRemote.isPlaying()) {
                MusicPlayerRemote.pauseSong()
            } else {
                MusicPlayerRemote.resumePlaying()
            }
        }

        binding.playingNext.setOnClickListener {
            MusicPlayerRemote.playNextSong()
        }

        binding.playingPrevious.setOnClickListener {
            MusicPlayerRemote.playPreviousSong()
        }

        binding.playingEqualizer.setOnClickListener {
            try {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                intent.putExtra(
                    AudioEffect.EXTRA_AUDIO_SESSION,
                    MusicPlayerRemote.getAudioSessionId()
                )
                intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)

                if (intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, 10)
                } else {
                    MDManager.getInstance(this)?.showMessage(
                        this,
                        resources.getString(R.string.equalizer_is_not_available)
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.playingAddPlaylist.setOnClickListener {
            val addPlaylistBottomSheet = BottomSheetAddPlaylist(this)
            addPlaylistBottomSheet.setData(MusicPlayerRemote.getCurrentSong())
            addPlaylistBottomSheet.show()
        }

        binding.playingSong.setOnClickListener {
            val currentSong = MusicPlayerRemote.getCurrentSong()
            com.example.service.utils.Utils.shareVideoOrAudio(
                this@PlayerActivity,
                currentSong.mediaObject?.title,
                currentSong.mediaObject?.path
            )
        }

    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter()
        intentFilter.addAction(
            MusicService.PLAY_STATE_CHANGED
        )
        intentFilter.addAction(
            MusicService.META_CHANGED
        )
        intentFilter.addAction(
            MusicService.NEW_SONG_CHANGED
        )
        intentFilter.addAction(
            ACTION_UPDATE_DATA_PLAYLIST
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastPlayerMusic, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(broadcastPlayerMusic, intentFilter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastPlayerMusic)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in_quick, R.anim.exit_to_top)
    }

    override fun onResume() {
        super.onResume()
        checkShuffle()
        setUpPlayer()
    }
}
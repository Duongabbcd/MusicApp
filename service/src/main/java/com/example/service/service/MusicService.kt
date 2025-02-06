package com.example.service.service

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.Process
import android.preference.PreferenceManager
import android.provider.MediaStore.Audio.Media
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.widget.Toast
import com.example.service.model.Audio
import com.example.service.service.notification.PlayingNotification
import com.example.service.service.notification.PlayingNotificationImpl24
import com.example.service.utils.FileUtils
import com.example.service.utils.Keys
import com.example.service.utils.PreferenceUtil
import com.example.service.utils.ShuffleHelper
import com.example.service.utils.StopWatch
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import java.io.File
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class MusicService : Service(), SharedPreferences.OnSharedPreferenceChangeListener,
    Player.Listener {
    private val musicBind = MusicBinder()
    private var playback: ExoPlayer? = null
    private var playingQueue = arrayListOf<Any>()
    private var originalPlayingQueue: ArrayList<Any>? = arrayListOf()

    private var position = -1
    private var nextPosition = -1
    private var shuffleMode: Int = 0
    private var repeatMode: Int = 0

    private var queuesRestored = false
    private var pausedByTransientLossOfFocus = false

    private lateinit var playingNotification: PlayingNotification
    private var audioManager: AudioManager? = null
    private lateinit var audio: Audio
    private var currentAudio: Audio = Audio.EMPTY_SONG

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var playerHandler: PlaybackHandler
    private val audioFocusChangeListener by lazy {
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            playerHandler.obtainMessage(
                FOCUS_CHANGE,
                focusChange,
                0
            ).sendToTarget()
        }
    }

    private val mediaButtonIntentReceiver = MediaButtonIntentReceiver()

    private lateinit var queueSaveHandler: QueueSaveHandler
    private lateinit var musicPlayerHandlerThread: HandlerThread
    private lateinit var queueSaveHandlerThread: HandlerThread

    private val songPlayCountHelper = SongPlayCountHelper()
    private lateinit var throttledSeekHandler: ThrottledSeekHandler

    private var becomingNoisyReceiverRegistered = false
    private var becomingNoisyReceiverIntentFilter =
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val becomingNoisyReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action!! == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    pause()
                }
            }

        }
    }

    private lateinit var mediaStoreObserver: ContentObserver
    private var notHandledMetaChangedForCurrentTrack = false
    private lateinit var uiThreadHandler: Handler

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        wakeLock.setReferenceCounted(false)

        musicPlayerHandlerThread = HandlerThread("PlaybackHandler")
        musicPlayerHandlerThread.start()

        playerHandler = PlaybackHandler(this)
        playback = ExoPlayer.Builder(this).build()

        setupMediaSession()

        // queue saving needs to run on a separate thread so that it doesn't block the playback handler events
        queueSaveHandlerThread =
            HandlerThread("QueueSaveHandler", Process.THREAD_PRIORITY_BACKGROUND)
        queueSaveHandlerThread.start()
        queueSaveHandler = QueueSaveHandler(this, queueSaveHandlerThread.looper)

        uiThreadHandler = Handler()

        initNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateNotification()
        }

        mediaStoreObserver = MediaStoreObserver(playerHandler)
        throttledSeekHandler = ThrottledSeekHandler(playerHandler)

        contentResolver.registerContentObserver(
            Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver
        )
        contentResolver.registerContentObserver(
            Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
        )

        PreferenceUtil.getInstance(this)?.registerOnSharedPreferenceChangedListener(this)

        restoreState()

        mediaSession.isActive = true

        sendBroadcast(Intent(MUSIC_PLAYER_MUSIC_SERVICE_CREATED))

    }

    private fun getAudioManager(): AudioManager? {
        if (audioManager == null) {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        return audioManager
    }

    private fun setupMediaSession() {
        val mediaButtonReceiverComponentName =
            ComponentName(applicationContext, MediaButtonIntentReceiver::class.java)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.component = mediaButtonReceiverComponentName

        val mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            mediaButtonIntent,
            PlayingNotificationImpl24.flag
        )

        mediaSession = MediaSessionCompat(
            this,
            "MusicPlayer",
            mediaButtonReceiverComponentName,
            mediaButtonReceiverPendingIntent
        )
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                play()
            }

            override fun onPause() {
                pause()
            }

            override fun onSkipToNext() {
                back(true)
            }

            override fun onSkipToPrevious() {
                back(true)
            }

            override fun onStop() {
                quit()
            }

            override fun onSeekTo(pos: Long) {
                seek(pos)
            }

            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                return mediaButtonIntentReceiver.handleIntent(this@MusicService, mediaButtonEvent)
            }

            override fun onCustomAction(action: String?, extras: Bundle?) {
                super.onCustomAction(action, extras)
                if (action == ACTION_QUIT) {
                    quit()
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (playingQueue.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updateNotification()
            } else if (intent != null) {
                if (intent.action != null && intent.action == ACTION_QUIT) {
                    quit()
                }
            }
            return START_NOT_STICKY
        }
        val action: String
        if (intent != null) {
            if (intent.action != null) {
                action = intent.action ?: ""
                when (action) {
                    ACTION_TOGGLE_PAUSE -> {
                        if (isPlaying()) pause() else play()
                    }

                    ACTION_PAUSE -> pause()
                    ACTION_PLAY -> play()
                    ACTION_REWIND -> back(true)
                    ACTION_SKIP -> playNextSong(true)
                    ACTION_STOP, ACTION_QUIT -> quit()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && intent?.action != ACTION_QUIT) {
            updateNotification()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (becomingNoisyReceiverRegistered) {
            unregisterReceiver(becomingNoisyReceiver)
            becomingNoisyReceiverRegistered = false
        }

        mediaSession.isActive = false
        quit()
        releaseResources()
        contentResolver.unregisterContentObserver(mediaStoreObserver)
        PreferenceUtil.getInstance(this)?.unRegisterOnSharedPreferenceChangedListener(this)
        wakeLock.release()

        sendBroadcast(Intent(MUSIC_SERVICE_ON_DESTROYED))
    }

    override fun onBind(intent: Intent?): IBinder {
        return musicBind
    }

    fun checkAfterDeletePlaying() {
        try {
            var deletedPosition = 0
            val currentList = getPlayingQueue()
            var entityExits: Audio? = null
            for (i in 0 until currentList.size) {
                val audio = currentList[i] as Audio
                if (audio.mediaObject?.path?.let { !File(it).exists() } == true && !audio.isOnline) {
                    entityExits = audio
                    deletedPosition = i
                    break
                }
            }

            if (entityExits != null) {
                val isPlaying = entityExits.mediaObject?.path == getCurrentSong().mediaObject?.path
                getPlayingQueue().remove(entityExits).also {
                    rePosition(deletedPosition)
                }
                if (isPlaying) {
                    if (position >= getPlayingQueue().size) {
                        playSongAt(0)
                    } else {
                        playSongAt(position)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class QueueSaveHandler(service: MusicService, looper: Looper) : Handler() {
        private var mService: WeakReference<MusicService>? = null

        override fun handleMessage(msg: Message) {
            val service = mService?.get()
            if (msg.what == SAVE_QUEUES) {
                service?.saveQueuesImpl()
            }
        }
    }

    private fun saveQueuesImpl() {
    }

    private fun savePosition() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(SAVED_POSITION, getPosition()).apply()
    }

    private fun savePositionInTrack() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putLong(SAVED_POSITION_IN_TRACK, getSongProgressMillis()).apply()
    }

    private fun saveState() {
        saveQueues()
        savePosition()
        savePositionInTrack()
    }

    private fun saveQueues() {
        queueSaveHandler.removeMessages(SAVE_QUEUES)
        queueSaveHandler.sendEmptyMessage(SAVE_QUEUES)
    }

    private fun restoreState() {
        shuffleMode =
            PreferenceManager.getDefaultSharedPreferences(this).getInt(SAVED_SHUFFLE_MODE, 0)
        repeatMode =
            PreferenceManager.getDefaultSharedPreferences(this).getInt(SAVED_REPEAT_MODE, 0)

        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
        handleAndSendChangeInternal(REPEAT_MODE_CHANGED)

        playerHandler.removeMessages(RESTORE_QUEUES)
        playerHandler.sendEmptyMessage(RESTORE_QUEUES)
    }

    private fun quit() {
        pause()
        playingNotification.stop()
        closeAudioEffectSession()
        getAudioManager()?.abandonAudioFocus(audioFocusChangeListener)
        stopSelf()
    }

    private fun releaseResources() {
        playerHandler.removeCallbacksAndMessages(null)
        musicPlayerHandlerThread.quitSafely()
        queueSaveHandler.removeCallbacksAndMessages(null)
        queueSaveHandlerThread.quitSafely()
        playback?.release()
        playback = null
        mediaSession.release()
    }

    fun isPlaying(): Boolean {
        println("isPlaying: ${playback?.isPlaying}")
        return playback?.isPlaying ?: false
    }

    fun getPosition(): Int {
        return try {
            position
        } catch (e: Exception) {
            0
        }
    }

    fun playNextSong(force: Boolean) {
        try {
            val obj = getPlayingQueue()[getNextPosition(force)]
            if (obj is Audio) {
                playSongAt(getNextPosition(force))
            } else {
                playNextSong(true)
            }
        } catch (e: Exception) {
            pause()
            e.printStackTrace()
        }
    }

    private fun setDataSourceImpl(currentSong: Audio): Boolean {
        try {
            this.audio = currentSong
            playback?.stop()
            playback?.release()

            playback = ExoPlayer.Builder(this).build()
            if (audio.videoType == "ccmixter") {
                val header: MutableMap<String, String> = mutableMapOf()
                header["Referer"] = audio.ccmixterReferrer
                val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(header)
                val mediaItem = MediaItem.fromUri(audio.mediaObject?.path!!)
                val mediaSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)
                playback?.setMediaSource(mediaSource)
            } else {
                val mediaItem = MediaItem.fromUri(Uri.parse(audio.mediaObject?.path!!))
                playback?.setMediaItem(mediaItem)
            }
            playback?.prepare()
            playback?.play()
        } catch (e: Exception) {
            return false
        }

        playback?.addListener(this)
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId())
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        sendBroadcast(intent)
        return true
    }

    private fun openTrackAndPrepareNextAt(position: Int): Boolean {
        synchronized(this) {
            this.position = position
            val prepared = openCurrent()
            if (prepared) prepareNextImpl()
            notifyChange(META_CHANGED)
            notHandledMetaChangedForCurrentTrack = false
            return prepared
        }
    }

    private fun openCurrent(): Boolean {
        synchronized(this) {
            try {
                setDataSourceImpl(getCurrentSong())
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    }

    fun setCurrentSong(runningAudio: Audio) {
        currentAudio = runningAudio
    }

    private fun prepareNext() {
        playerHandler.removeMessages(PREPARE_NEXT)
        playerHandler.obtainMessage(PREPARE_NEXT).sendToTarget()
    }

    private fun prepareNextImpl(): Boolean {
        synchronized(this) {
            try {
                val nextPosition = getNextPosition(false)
                this.nextPosition = nextPosition
                return true
            } catch (e: Exception) {
                return false
            }
        }
    }

    private fun closeAudioEffectSession() {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playback?.audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(audioEffectsIntent)
    }

    private fun requestFocus(): Boolean {
        return (getAudioManager()?.requestAudioFocus(
            audioFocusChangeListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    private fun initNotification() {
        playingNotification = PlayingNotificationImpl24()
        playingNotification.init(this)
    }

    private fun updateNotification() {
        playingNotification.update()
    }

    private fun updateMediaSessionPlaybackState() {
        try {
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder().setActions(MEDIA_SESSION_ACTIONS)
                    .setState(
                        if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        getSongProgressMillis(),
                        1F
                    ).addCustomAction(
                        ACTION_QUIT, "Application Quit",
                        android.R.drawable.ic_menu_close_clear_cancel
                    )
                    .build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMediaSessionMetaData() {
        val song = getCurrentSong()

        if (getCurrentSong().mediaObject?.id == "-1") {
            mediaSession.setMetadata(null)
            return
        }

        val metaData = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.mediaObject?.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.timeCount)
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (getPosition() + 1).toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, 1999L)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, playingQueue.size.toLong())

        if (PreferenceUtil.getInstance(this)?.albumArtOnLockScreen() == true) {
            mediaSession.setMetadata(metaData.build())
        }
    }

    fun runOnUiThread(runnable: Runnable) {
        uiThreadHandler.post(runnable)
    }

    fun getCurrentSong(): Audio {
        return getSongAt(getPosition())
    }

    private fun getSongAt(position: Int): Audio {
        try {
            if (position >= 0 && position < getPlayingQueue().size) {
                val obj = getPlayingQueue()[position]
                if (obj is Audio) {
                    return obj
                } else {
                    val newObj = getPlayingQueue()[playingQueue.size - 1]
                    return newObj as Audio
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Audio.EMPTY_SONG
    }

    private fun getNextPosition(force: Boolean): Int {
        var position = getPosition() + 1
        when (getRepeatMode()) {
            REPEAT_MODE_ALL -> {
              if (isLastTrack()) {
                  position = 0
                }
            }

            REPEAT_MODE_THIS -> {
                if (force) {
                    if (isLastTrack()) {
                        position = 0
                    }
                } else {
                    position -= 1
                }
            }

            else -> {
                if (isLastTrack()) {
                    position -= 1
                }
            }
        }
        return position
    }

    private fun isLastTrack(): Boolean = getPosition() == getPlayingQueue().size - 1

    fun getPlayingQueue(): ArrayList<Any> = playingQueue

    fun getRepeatMode(): Int = repeatMode

    fun openQueue(queue: ArrayList<Any>?, startPosition: Int, startPlaying: Boolean) {
        if (!queue.isNullOrEmpty() && startPosition >= 0 && startPosition < queue.size) {
            // it is important to copy the playing queue here first as we might add/remove songs later

            originalPlayingQueue = ArrayList(queue)
            playingQueue = originalPlayingQueue?.let { ArrayList(it) }!!

            var position = startPosition
            if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                ShuffleHelper.makeShuffleList(this.playingQueue, startPosition)
                position = 0
            }

            if (startPlaying) {
                playSongAt(position)
            } else {
                setPosition(position)
            }
            notifyChange(QUEUE_CHANGED)
        }
    }

    private fun addSongRecentlyPlay(currentSong: Audio) {
        //Not add recently added song
        if (currentSong.strLinkDownload != null || currentSong.strLinkDownload.isNotBlank()
        ) {
            return
        }

        var listRecently: ArrayList<Audio> =
            FileUtils.read(Keys.KEY_RECENTLY_CACHE, this) as ArrayList<Audio>

        if (listRecently == null) {
            listRecently = arrayListOf()
        }

        if (listRecently.size > 40) {
            listRecently.removeAt(listRecently.size - 1)
        }

        val ccc = listRecently.indexOf(currentSong)
        if (ccc != 1) {
            listRecently.removeAt(ccc)
        }
        listRecently.add(0, currentSong)
    }

    fun addSong(position: Int, song: Audio) {
        playingQueue.add(position, song)
        originalPlayingQueue?.add(position, song)
        notifyChange(QUEUE_CHANGED)
    }

    fun addSong(song: Audio) {
        playingQueue.add(song)
        originalPlayingQueue?.add(song)
        notifyChange(QUEUE_CHANGED)
    }

    fun addSongs(position: Int, songs: List<Any>) {
        playingQueue.addAll(position, songs)
        originalPlayingQueue?.addAll(position, songs)
        notifyChange(QUEUE_CHANGED)
    }

    fun addSongs(songs: List<Any>) {
        playingQueue.addAll(songs)
        originalPlayingQueue?.addAll(songs)
        notifyChange(QUEUE_CHANGED)
    }

    fun removeSong(position: Int) {
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            playingQueue.remove(position)
            originalPlayingQueue?.remove(position)
        } else {
            originalPlayingQueue?.remove(playingQueue.remove(position))
        }

        rePosition(position)

        notifyChange(QUEUE_CHANGED)
    }

    fun removeSong(song: Audio) {
        for (i in 0 until playingQueue.size) {
            if ((playingQueue[i] as Audio).mediaObject?.id == song.mediaObject?.id) {
                playingQueue.remove(i)
                rePosition(i)
            }
        }

        originalPlayingQueue?.let {
            for (i in 0 until it.size) {
                if ((it[i] as Audio).mediaObject?.id == song.mediaObject?.id) {
                    it.remove(i)
                    rePosition(i)
                }
            }
        }
        notifyChange(QUEUE_CHANGED)
    }

    private fun rePosition(deletedPosition: Int) {
        val currentPosition = getPosition()
        if (deletedPosition < currentPosition) {
            position = currentPosition - 1
        } else if (deletedPosition == currentPosition) {
            if (playingQueue.size > deletedPosition) {
                setPosition(position)
            } else {
                setPosition(position - 1)
            }
        }
    }

    fun moveSong(from: Int, to: Int) {
        if (from == to) return
        val currentPosition = getPosition()
        val songToMove = playingQueue.removeAt(from) as Audio
        playingQueue.add(to, songToMove)
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            val tmpSong: Audio = originalPlayingQueue!!.removeAt(from) as Audio
            originalPlayingQueue!!.add(to, tmpSong)
        }

        if (currentPosition in to..<from) {
            position = currentPosition + 1
        } else if (currentPosition in (from + 1)..to) {
            position = currentPosition - 1
        } else if (from == currentPosition) {
            position = to
        }
        notifyChange(QUEUE_CHANGED)
    }

    fun clearQueue() {
        playingQueue.clear()
        originalPlayingQueue?.clear()

        setPosition(-1)
        notifyChange(QUEUE_CHANGED)
    }

    fun playSongAt(position: Int) {
        // handle this on the handlers thread to avoid blocking the ui thread
        playSongAtImpl(position)
        sendBroadcast(Intent(NEW_SONG_CHANGED))
    }

    fun setPosition(position: Int) {
        openTrackAndPrepareNextAt(position)
        notifyChange(PLAY_STATE_CHANGED)
    }

    private fun playSongAtImpl(position: Int) {
        try {
            playback?.stop()
            if (position == -1) {
                return
            }
            if (openTrackAndPrepareNextAt(position)) {
                play()
            } else {
                Toast.makeText(this, "Can't play this song!", Toast.LENGTH_SHORT).show()
                if (!TextUtils.isEmpty(MusicPlayerRemote.getCurrentSong().mediaObject?.path)) {
                    playNextSong(true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            pausedByTransientLossOfFocus = false
            playback?.pause()
            notifyChange(PLAY_STATE_CHANGED)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTitle() {
        notifyChange(PLAY_STATE_CHANGED)
    }

    fun play() {
        synchronized(this) {
            try {
                if (requestFocus()) {
                    if (!isPlaying()) {
                        playback?.play()
                        if (!becomingNoisyReceiverRegistered) {
                            registerReceiver(
                                becomingNoisyReceiver,
                                becomingNoisyReceiverIntentFilter
                            )
                            becomingNoisyReceiverRegistered = true
                        }
                        if (notHandledMetaChangedForCurrentTrack) {
                            handleChangeInternal(META_CHANGED)
                            notHandledMetaChangedForCurrentTrack = false
                        }
                        notifyChange(PLAY_STATE_CHANGED)

                        // fixes a bug where the volume would stay ducked because the AudioManager.AUDIOFOCUS_GAIN event is not sent
                        playerHandler.removeMessages(DUCK)
                        playerHandler.sendEmptyMessage(UNDUCK)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playPreviousSong(force: Boolean) {
        try {
            playSongAt(getPreviousPosition(force))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun back(force: Boolean) {
        if (getSongProgressMillis() > 5000L) {
            seek(0)
        } else {
            playPreviousSong(force)
        }
    }


    private fun getPreviousPosition(force: Boolean): Int {
        var newPosition = getPosition() - 1
        when (repeatMode) {
            REPEAT_MODE_ALL -> {
                if (newPosition < 0) {
                    newPosition = getPlayingQueue().size - 1
                }
            }

            REPEAT_MODE_THIS -> {
                if (force) {
                    if (newPosition < 0) {
                        newPosition = getPlayingQueue().size - 1
                    }
                } else {
                    newPosition = getPosition()
                }
            }

            REPEAT_MODE_NONE -> {
                if (newPosition < 0) {
                    newPosition = 0
                }
            }
        }
        return newPosition
    }

    fun getSongProgressMillis(): Long {
        return try {
            playback?.currentPosition ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getSongDurationMillis(): Long {
        return if (playback != null) {
            playback!!.duration
        } else 0L
    }

    fun seek(millis: Long): Long {
        synchronized(this) {
            try {
                playback?.seekTo(millis)
                throttledSeekHandler.notifySeek()

                updateMediaSessionMetaData()
                return playback?.currentPosition ?: 0L
            } catch (e: Exception) {
                return 0L
            }
        }
    }

    fun getShuffleMode() = shuffleMode

    fun setShuffleMode(shuffleMode: Int) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(SAVED_SHUFFLE_MODE, shuffleMode).apply()
        when (shuffleMode) {
            SHUFFLE_MODE_SHUFFLE -> {
                this.shuffleMode = shuffleMode
                ShuffleHelper.makeShuffleList(this.getPlayingQueue(), getPosition())
                position = 0
            }

            SHUFFLE_MODE_NONE -> {
                this.shuffleMode = shuffleMode
                playingQueue = ArrayList(originalPlayingQueue)
                var newPosition = 0
                val selectedSong = if(currentAudio == Audio.EMPTY_SONG) getCurrentSong() else currentAudio
                for (item in getPlayingQueue()) {
                    val song = item as Audio
                    if (song.mediaObject?.path == selectedSong.mediaObject?.path) {
                        newPosition = getPlayingQueue().indexOf(song)
                    }
                }
                position = newPosition
            }
        }
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
    }

    private fun notifyChange(playStateChanged: String) {
        handleAndSendChangeInternal(playStateChanged)
        sendPublicIntent(playStateChanged)
    }

    fun handleAndSendChangeInternal(mediaStoreChanged: String) {
        handleChangeInternal(mediaStoreChanged)
        sendChangeInternal(mediaStoreChanged)
    }

    // to let other apps know whats playing. i.E. last.fm (scrobbling) or musixmatch
    private fun sendPublicIntent(playStateChanged: String) {
        val intent = Intent(playStateChanged.replace(MUSIC_PLAYER_PACKAGE_NAME, MUSIC_PACKAGE_NAME))

        val song = getCurrentSong()
        intent.putExtra(ID, song.mediaObject?.id)
        intent.putExtra(ARTIST, song.artist)
        intent.putExtra(ALBUM, song.albumName)
        intent.putExtra(TRACK, song.mediaObject?.title)

        intent.putExtra(DURATION, song.timeCount)
        intent.putExtra(POSITION, getSongProgressMillis())

        intent.putExtra(PLAYING, isPlaying())
        intent.putExtra(SCROBBLING_SOURCE, MUSIC_PLAYER_PACKAGE_NAME)

        sendBroadcast(intent)
    }

    private fun sendChangeInternal(mediaStoreChanged: String) {
        sendBroadcast(Intent(mediaStoreChanged))
    }

    private val MEDIA_SESSION_ACTIONS: Long = (PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_SEEK_TO)

    private fun handleChangeInternal(mediaStoreChanged: String) {
        when (mediaStoreChanged) {
            PLAY_STATE_CHANGED -> {
                updateMediaSessionPlaybackState()
                updateNotification()
                val isPlaying = isPlaying()
                if (!isPlaying && getSongProgressMillis() > 0) {
                    savePositionInTrack()
                }
                songPlayCountHelper.notifyPlayStateChanged(isPlaying)
            }

            META_CHANGED -> {
                updateMediaSessionMetaData()
                updateNotification()
                savePosition()
                savePositionInTrack()
                val currentSong = getCurrentSong()
                addSongRecentlyPlay(currentSong)
                songPlayCountHelper.notifySongChanged(currentSong)
            }

            QUEUE_CHANGED -> {
                updateMediaSessionMetaData()
                saveState()
                if (playingQueue.isNotEmpty()) {
                    prepareNext()
                } else {
                    playingNotification.stop()
                }
            }
        }
    }

    fun getAudioSessionId(): Int {
        return playback?.audioSessionId ?: 0
    }

    fun getMediaSession(): MediaSessionCompat {
        return mediaSession
    }

    fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferenceUtil.GAPLESS_PLAYBACK -> {
                if (sharedPreferences?.getBoolean(key, false) == true) {
                    prepareNext()
                } else {
                    playback?.stop()
                }
            }

            PreferenceUtil.ALBUM_ART_ON_LOCKSCREEN,
            PreferenceUtil.BLURRED_ALBUM_ART -> {
                updateMediaSessionMetaData()
            }
        }
    }

    private class PlaybackHandler(val service: MusicService) : Handler() {
        var mService: WeakReference<MusicService>? = null
        var currentDuckVolume = 1.0f

        init {
            mService = WeakReference(service)
        }

        override fun handleMessage(msg: Message) {
            val service = mService?.get() ?: return

            when (msg.what) {
                DUCK -> {
                    if (PreferenceUtil.getInstance(service)?.audioDucking() == true) {
                        currentDuckVolume -= .05f
                        if (currentDuckVolume > .2f) {
                            sendEmptyMessageDelayed(DUCK, 10)
                        } else {
                            currentDuckVolume = .2f
                        }
                    } else {
                        currentDuckVolume = 1f
                    }
                    service.playback?.volume = currentDuckVolume
                }

                UNDUCK -> {
                    if (PreferenceUtil.getInstance(service)?.audioDucking() == true) {
                        currentDuckVolume += .03f
                        if (currentDuckVolume < 1f) {
                            sendEmptyMessageDelayed(UNDUCK, 10)
                        } else {
                            currentDuckVolume = .1f
                        }
                    } else {
                        currentDuckVolume = 1f
                    }
                    service.playback?.volume = currentDuckVolume
                }

                TRACK_WENT_TO_NEXT -> {
                    if (service.repeatMode == REPEAT_MODE_NONE && service.isLastTrack()) {
                        service.pause()
                        service.seek(0)
                    } else {
                        service.position = service.nextPosition
                        service.prepareNextImpl()
                        service.notifyChange(META_CHANGED)
                    }
                }

                TRACK_ENDED -> {
                    if (service.repeatMode == REPEAT_MODE_NONE && service.isLastTrack()) {
                        service.notifyChange(PLAY_STATE_CHANGED)
                        service.seek(0)
                    } else {
                        service.playNextSong(false)
                    }
                    sendEmptyMessage(RELEASE_WAKELOCK)
                }

                RELEASE_WAKELOCK -> {
                    service.releaseWakeLock()
                }

                PLAY_SONG -> {
                    service.playSongAtImpl(msg.arg1)
                }

                SET_POSITION -> {
                    service.openTrackAndPrepareNextAt(msg.arg1)
                    service.notifyChange(PLAY_STATE_CHANGED)
                }

                PREPARE_NEXT -> {
                    service.prepareNextImpl()
                }

                FOCUS_CHANGE -> {
                    when (msg.arg1) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            if (!service.isPlaying() && service.pausedByTransientLossOfFocus) {
                                service.play()
                                service.pausedByTransientLossOfFocus = false
                            }
                        }

                        AudioManager.AUDIOFOCUS_LOSS -> {
                            // Lost focus for an unbounded amount of time: stop playback and release media playback
                            service.pause()
                        }

                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            // Lost focus for a short time, but we have to stop
                            // playback?. We don't release the media playback because playback
                            // is likely to resume
                            val wasPlaying = service.isPlaying()
                            service.pause()
                            service.pausedByTransientLossOfFocus = wasPlaying
                        }

                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            // Lost focus for a short time, but it's ok to keep playing
                            // at an attenuated level
                            removeMessages(UNDUCK)
                            sendEmptyMessage(DUCK)
                        }
                    }
                }
            }
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService {
            return this@MusicService
        }
    }

    inner class MediaStoreObserver(handler: Handler) : ContentObserver(handler), Runnable {
        // milliseconds to delay before calling refresh to aggregate events
        private val REFRESH_DELAY = 500L
        private var mHandler: Handler? = null

        init {
            mHandler = handler
        }

        override fun onChange(selfChange: Boolean) {
            // if a change is detected, remove any scheduled callback
            // then post a new one. This is intended to prevent closely
            // spaced events from generating multiple refresh calls
            mHandler?.let {
                it.removeCallbacks(this)
                it.postDelayed(this, REFRESH_DELAY)
            }
        }

        override fun run() {
            // actually call refresh when the delayed callback fires
            // do not send a sticky broadcast here
            handleAndSendChangeInternal(MEDIA_STORE_CHANGED)
        }
    }

    inner class ThrottledSeekHandler(handler: Handler) : Runnable {
        // milliseconds to throttle before calling run() to aggregate events
        private val THROTTLE = 500L
        private var mHandler: Handler? = null

        init {
            mHandler = handler
        }

        fun notifySeek() {
            mHandler?.let {
                it.removeCallbacks(this)
                it.postDelayed(this, THROTTLE)
            }
        }

        override fun run() {
            savePositionInTrack()
            sendPublicIntent(PLAY_STATE_CHANGED)
        }

    }

    private class SongPlayCountHelper {
        val stopWatch: StopWatch = StopWatch()

        private var song = Audio.EMPTY_SONG

        fun shouldBumpPlayCount(): Boolean {
            return song.timeCount * 0.5 < stopWatch.getElapsedTime()
        }

        fun notifySongChanged(song: Audio) {
            synchronized(this) {
                stopWatch.reset()
                this.song = song
            }
        }

        fun notifyPlayStateChanged(isPlaying: Boolean) {
            synchronized(this) {
                if (isPlaying) {
                    stopWatch.start()
                } else {
                    stopWatch.pause()
                }
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_READY -> {
                playback?.let {
                    val time = it.duration
                    getCurrentSong().timeCount = time
                }
            }

            Player.STATE_ENDED -> {
                if (getRepeatMode() == REPEAT_MODE_NONE) {
                    if (!isLastTrack()) {
                        playNextSong(false)
                    }
                } else {
                    playNextSong(false)
                }
            }

            else -> {

            }
        }
        notifyChange(PLAY_STATE_CHANGED)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        playback?.release()
        playback = ExoPlayer.Builder(this).build()
        playback?.setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)

        if (!TextUtils.isEmpty(MusicPlayerRemote.getCurrentSong().mediaObject?.path)
            && audio.mediaObject?.path?.startsWith("http")!!
        ) {
            MusicPlayerRemote.playNextSong()
        }
    }

    //Time sleeper
    var countDownTimer: CountDownTimer? = null
    var timeRemaining: Long = 0L

    fun startCountDown(time: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished
                val intent = Intent(MusicService.ACTION_UPDATE_SLEEP_TIMER)
                sendBroadcast(intent)
            }

            override fun onFinish() {
                pause()
                countDownTimer = null
                timeRemaining = 0
                val intent = Intent(ACTION_UPDATE_SLEEP_TIMER)
                sendBroadcast(intent)
            }

        }
        countDownTimer?.start()
    }

    fun cancelCountDown() {
        countDownTimer?.cancel()
        countDownTimer = null
        timeRemaining = 0

        val intent = Intent(ACTION_UPDATE_SLEEP_TIMER)
        sendBroadcast(intent)
    }

    fun cycleRepeatMode() {
        when (getRepeatMode()) {
            REPEAT_MODE_NONE -> setRepeatMode(REPEAT_MODE_ALL)
            REPEAT_MODE_ALL -> setRepeatMode(REPEAT_MODE_THIS)
            REPEAT_MODE_THIS -> setRepeatMode(REPEAT_MODE_NONE)
            else -> REPEAT_MODE_NONE
        }
    }

    private fun setRepeatMode(value: Int) {
        when (value) {
            REPEAT_MODE_NONE,
            REPEAT_MODE_ALL,
            REPEAT_MODE_THIS -> {
                repeatMode = value
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putInt(SAVED_REPEAT_MODE, value).apply()
                prepareNext()
                handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
            }
        }
    }

    fun toggleShuffle() {
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            setShuffleMode(SHUFFLE_MODE_SHUFFLE)
        } else {
            setShuffleMode(SHUFFLE_MODE_NONE)
        }
    }


    companion object {
        private var TAG = "MusicService"

        const val MUSIC_PLAYER_MUSIC_SERVICE_CREATED =
            "com.example.service.service.MUSIC_PLAYER_MUSIC_SERVICE_CREATED"

        const val MUSIC_PLAYER_PACKAGE_NAME = "com.example.musicapp"
        const val MUSIC_PACKAGE_NAME = "com.android.music"
        const val ACTION_UPDATE_SLEEP_TIMER = "com.ACTION_UPDATE_SLEEP_TIMER.music"

        const val ACTION_TOGGLE_PAUSE = "$MUSIC_PLAYER_PACKAGE_NAME.togglepause"
        const val ACTION_PLAY = "$MUSIC_PLAYER_PACKAGE_NAME.play"
        const val ACTION_PLAY_PLAYLIST = "$MUSIC_PLAYER_PACKAGE_NAME.play.playlist"
        const val ACTION_PAUSE = "$MUSIC_PLAYER_PACKAGE_NAME.pause"
        const val ACTION_STOP = "$MUSIC_PLAYER_PACKAGE_NAME.stop"
        const val ACTION_SKIP = "$MUSIC_PLAYER_PACKAGE_NAME.skip"
        const val ACTION_REWIND = "$MUSIC_PLAYER_PACKAGE_NAME.rewind"
        const val ACTION_QUIT = "$MUSIC_PLAYER_PACKAGE_NAME.quitservice"
        const val INTENT_EXTRA_PLAYLIST = "$MUSIC_PLAYER_PACKAGE_NAME.intentextra.playlist"
        const val INTENT_EXTRA_SHUFFLE_MODE = "$MUSIC_PLAYER_PACKAGE_NAME.intentextra.shufflemode"

        const val APP_WIDGET_UPDATE = "$MUSIC_PLAYER_PACKAGE_NAME.appwidgetupdate"
        const val EXTRA_APP_WIDGET_NAME = "$MUSIC_PLAYER_PACKAGE_NAME.app_widget_name"

        const val META_CHANGED = "$MUSIC_PLAYER_PACKAGE_NAME.metachanged"
        const val QUEUE_CHANGED = "$MUSIC_PLAYER_PACKAGE_NAME.queuechanged"
        const val PLAY_STATE_CHANGED = "$MUSIC_PLAYER_PACKAGE_NAME.playstatechanged"
        const val NEW_SONG_CHANGED = "$MUSIC_PLAYER_PACKAGE_NAME.newsongchanged"

        const val NO_INTERNET = "$MUSIC_PLAYER_PACKAGE_NAME.nointernet"
        const val STREAM_VIA_WIFI = "$MUSIC_PLAYER_PACKAGE_NAME.streamviawifi"

        const val REPEAT_MODE_CHANGED = "$MUSIC_PLAYER_PACKAGE_NAME.nointernet"
        const val SHUFFLE_MODE_CHANGED = "$MUSIC_PLAYER_PACKAGE_NAME.shufflemodechanged"
        const val MEDIA_STORE_CHANGED = "$MUSIC_PLAYER_PACKAGE_NAME.mediastorechanged"

        const val SAVED_POSITION = "POSITION"
        const val SAVED_POSITION_IN_TRACK = "POSITION_IN_TRACK"
        const val SAVED_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val SAVED_REPEAT_MODE = "REPEAT_MODE"

        const val RELEASE_WAKELOCK = 0
        const val TRACK_ENDED = 1
        const val TRACK_WENT_TO_NEXT = 2
        const val PLAY_SONG = 3
        const val PREPARE_NEXT = 4
        const val SET_POSITION = 5
        const val FOCUS_CHANGE = 6
        const val DUCK = 7
        const val UNDUCK = 8
        const val RESTORE_QUEUES = 9

        const val SHUFFLE_MODE_NONE = 0
        const val SHUFFLE_MODE_SHUFFLE = 1

        const val REPEAT_MODE_NONE = 0
        const val REPEAT_MODE_ALL = 1
        const val REPEAT_MODE_THIS = 2

        const val SAVE_QUEUES = 0

        const val ID = "id"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val TRACK = "track"
        const val DURATION = "duration"
        const val POSITION = "position"
        const val PLAYING = "playing"
        const val SCROBBLING_SOURCE = "scrobbling_source"

        const val MUSIC_SERVICE_ON_DESTROYED =
            "com.example.service.service.MusicService.MUSIC_SERVICE_ON_DESTROYED"
    }
}
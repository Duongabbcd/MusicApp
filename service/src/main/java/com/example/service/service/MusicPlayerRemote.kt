package com.example.service.service

import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.service.model.Audio
import com.example.service.utils.PreferenceUtil
import com.example.service.utils.Utils
import java.io.File
import java.util.Random
import java.util.WeakHashMap

object MusicPlayerRemote {
    private lateinit var mConnectionMap: WeakHashMap<Context, ServiceBinder>
    fun bindToService(context: Context, callback: ServiceConnection?): ServiceToken? {
        try {
            var realActivity = (context as Activity).parent
            if (realActivity == null) {
                realActivity = context
            }

            val contextWrapper = ContextWrapper(realActivity)
            contextWrapper.startService(Intent(contextWrapper, MusicService::class.java))
            val binder = ServiceBinder(callback)

            if (contextWrapper.bindService(
                    Intent().setClass(contextWrapper, MusicService::class.java),
                    binder,
                    Context.BIND_AUTO_CREATE
                )
            ) {
                mConnectionMap[contextWrapper] = binder
                return ServiceToken(contextWrapper)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun unbindFromService(token: ServiceToken?) {
        if (token == null) {
            return
        }
        val mContextWrapper = token.mWrappedContext
        val mBinder = mConnectionMap.remove(mContextWrapper) ?: return

        mContextWrapper.unbindService(mBinder)
        if (mConnectionMap.isEmpty()) {
            musicService = null
        }
    }

    class ServiceBinder(callback: ServiceConnection?) : ServiceConnection {
        private var mCallBack: ServiceConnection? = callback

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            mCallBack?.onServiceConnected(className, service)
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            mCallBack?.onServiceDisconnected(className)
            musicService = null
        }
    }

    class ServiceToken(context: Context) {
        val mWrappedContext = context
    }

    fun setCurrentAudio(runningAudio: Audio) {
        try {
            musicService?.setCurrentSong(runningAudio)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSongAt(position: Int) {
        try {
            if (musicService != null) {

                musicService?.playSongAt(position)
            }
        } catch (e: java.lang.Exception) {
            //FirebaseEventUtils.getInstances().recordException(e);
        }
    }

    fun setPosition(position: Int) {
        try {
            if (musicService != null) {
                musicService?.setPosition(position)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseSong() {
        try {
            if (musicService != null) {
                musicService?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun playPreviousSong() {
        try {
            musicService?.playPreviousSong(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun back() {
        try {
            if (musicService != null) {
                musicService?.back(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isPlaying(): Boolean {
        return musicService != null && musicService?.isPlaying() == true
    }

    fun resumePlaying() {
        try {
            if (musicService != null) {
                musicService?.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openQueue(queue: ArrayList<Any>, startPosition: Int, startPlaying: Boolean) {
//      println("openQueue: ${!tryToHandleOpenPlayingQueue(queue, startPosition, startPlaying)}")
        if (!tryToHandleOpenPlayingQueue(queue, startPosition, startPlaying)) {
            musicService?.openQueue(queue, startPosition, startPlaying)
            if (musicService == null) return

            if (PreferenceUtil.getInstance(musicService!!)?.rememberShuffle() == true) {
                setShuffleMode(MusicService.SHUFFLE_MODE_NONE)
            }

        }
    }

    fun openAndShuffleQueue(queue: ArrayList<Any>, startPlaying: Boolean) {
        var startPosition = 0
        if (queue.isNotEmpty()) {
            startPosition = Random().nextInt(queue.size)
        }
        if (!tryToHandleOpenPlayingQueue(
                queue,
                startPosition,
                startPlaying
            ) && musicService != null
        ) {
            openQueue(queue, startPosition, startPlaying)
            setShuffleMode(MusicService.SHUFFLE_MODE_SHUFFLE)
        }
    }

    private fun tryToHandleOpenPlayingQueue(
        queue: ArrayList<Any>,
        startPosition: Int,
        startPlaying: Boolean
    ): Boolean {
        println("tryToHandleOpenPlayingQueue: ${getPlayingQueue() == queue}")
        if (getPlayingQueue() == queue) {
            if (startPlaying) {
                playSongAt(startPosition)
            } else {
                setPosition(startPosition)
            }

            return true
        }
        return false
    }

    fun updateTitle() {
        musicService?.updateTitle()
    }

    fun getPosition(): Int {
        return if (musicService != null) {
            musicService!!.getPosition()
        } else -1
    }

    private fun setShuffleMode(shuffleMode: Int) {
        if (musicService != null) {
            musicService?.setShuffleMode(shuffleMode)
        }
    }

    fun enqueue(song: Audio): Boolean {
        if (musicService != null) {
            if (getPlayingQueue().isNotEmpty()) {
                musicService!!.addSong(song)
            } else {
                val queue = ArrayList<Any>()
                queue.add(song)
                openQueue(queue, 0, false)
            }
            return true
        }
        return false
    }

    fun enqueue(songs: ArrayList<Any>): Boolean {
        if (musicService != null) {
            if (getPlayingQueue().isNotEmpty()) {
                musicService!!.addSongs(songs.toList())
            } else {
                openQueue(songs, 0, false)
            }
            return true
        }
        return false
    }

    fun removeFromQueue(song: Audio): Boolean {
        if (musicService != null) {
            musicService!!.removeSong(song)
            return true
        }
        return false
    }

    fun moveSong(from: Int, to: Int): Boolean {
        if (musicService != null && from >= 0 && to >= 0 && from < getPlayingQueue().size && to < getPlayingQueue().size) {
            musicService?.moveSong(from, to)
            return true
        }
        return false
    }

    fun clearQueue(): Boolean {
        if (musicService != null) {
            musicService?.clearQueue()
            return true
        }
        return false
    }

    fun getAudioSessionId(): Int {
        if (musicService != null) {
            return musicService!!.getAudioSessionId()
        }
        return -1
    }

    fun playFromUri(uri: Uri) {
        if (musicService != null) {
            var songs: ArrayList<Audio>? = null
            if (uri.scheme != null && uri.authority != null) {
                if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
                    var songId: String? = ""
                    if (uri.authority == PROVIDER_MEDIA_CONTENT) {
                        songId = getSongIdFromMediaProvider(uri)
                    } else if (uri.authority == PROVIDER_MEDIA) {
                        songId = uri.lastPathSegment
                    }
                    if (songId != null) {
                        songs = SongLoader.getSongs(
                            SongLoader.makeSongCursor(
                                musicService!!, MediaStore.Audio.AudioColumns._ID + "=?",
                                arrayOf(songId)
                            )
                        )
                    }
                }
            }
            if (songs == null) {
                var songFile: File? = null
                if (uri.authority != null && uri.authority == PROVIDER_MEDIA_EXTERNAL_STORAGE) {
                    val path: String = uri.path ?: ""
                    songFile = File(
                        Environment.getExternalStorageDirectory(),
                        uri.path!!.split(":".toRegex(), 2).toTypedArray()[1]
                    )
                }
                if (songFile == null) {
                    val path = getFilePathFromUri(musicService!!, uri)
                    if (path != null) songFile = File(path)
                }

                if (songFile == null && uri.path != null) {
                    songFile = File(uri.path)
                }
                if (songFile != null) {
                    songs = SongLoader.getSongs(
                        SongLoader.makeSongCursor(
                            musicService!!, MediaStore.Audio.AudioColumns.DATA + "=?",
                            arrayOf(songFile.absolutePath)
                        )
                    )
                }
            }
            if (!songs.isNullOrEmpty()) {
                openQueue(ArrayList(songs), 0, true)
            } else {
                //TODO
            }
        }
    }

    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        val column = "_data"
        val projection = arrayOf(
            column
        )
        var cursor: Cursor?

        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            (null as Cursor?)?.close()
        }
        return null
    }

    fun getSongIdFromMediaProvider(uri: Uri): String {
        return DocumentsContract.getDocumentId(uri).split(":")[1]
    }


    private var TAG = MusicPlayerRemote.javaClass.simpleName
    var musicService: MusicService? = null
    private const val PROVIDER_MEDIA_CONTENT = "com.android.providers.media.documents"
    private const val PROVIDER_MEDIA_EXTERNAL_STORAGE = "com.android.externalstorage.documents"
    private const val PROVIDER_MEDIA = "media"

    fun getCurrentSong(): Audio {
        return if (musicService != null) {
            musicService!!.getCurrentSong()
        } else Audio.EMPTY_SONG
    }

    fun playNextSong() {
        try {
            if (musicService != null) {
                musicService?.playNextSong(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkAfterDeletePlaying() {
        musicService?.checkAfterDeletePlaying()
    }

    fun getPlayingQueue(): ArrayList<Any> {
        return if (musicService != null) {
            musicService!!.getPlayingQueue()
        } else arrayListOf()
    }

    fun removeFromQueue(position: Int) {
        if (musicService != null && position >= 0 && position < getPlayingQueue().size) {
            musicService!!.removeSong(position)
        }
    }

    fun cycleRepeatMode() {
        musicService?.cycleRepeatMode()
    }

    fun getShuffleMode(): Int {
        if (musicService != null) {
            return musicService!!.getShuffleMode()
        }
        return MusicService.SHUFFLE_MODE_NONE
    }

    fun getRepeatMode(): Int {
        if (musicService != null) {
            return musicService!!.getRepeatMode()
        }
        return MusicService.SHUFFLE_MODE_NONE
    }

    fun getSongDurationMillis(): Int {
        if (musicService != null) {
            return musicService!!.getSongDurationMillis().toInt()
        }
        return -1
    }

    fun getSongProgressMillis(): Long {
        if (musicService != null) {
            return musicService!!.getSongProgressMillis()
        }
        return -1L
    }

    fun seekTo(progress: Int) {
        if (musicService != null) {
            musicService!!.seek(progress.toLong())
        }
    }


    //Time sleeper
    fun cancelTimer() {
        musicService?.let {
            return it.cancelCountDown()
        }
    }

    fun timeLeft(): Long {
        musicService?.let {
            return it.timeRemaining
        }
        return 0L
    }

    fun startTimer(time: Long) {
        musicService?.startCountDown(time)
    }

    fun toggleShuffleMode() {
        musicService?.toggleShuffle()
    }
}
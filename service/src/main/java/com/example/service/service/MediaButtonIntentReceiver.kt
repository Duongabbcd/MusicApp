package com.example.service.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.view.KeyEvent

class MediaButtonIntentReceiver : BroadcastReceiver() {

    private var mWakeLock: PowerManager.WakeLock? = null
    private var mClickCounter = 0
    private var mLastClickTime = 0L

    private val handler: Handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_HEADSET_DOUBLE_CLICK_TIMEOUT) {
                    val clickCount = msg.arg1
                    val command = when (clickCount) {
                        1 -> MusicService.ACTION_TOGGLE_PAUSE
                        2 -> MusicService.ACTION_SKIP
                        3 -> MusicService.ACTION_REWIND
                        else -> null
                    }

                    if (command != null) {
                        val context = msg.obj as Context
                        startService(context, command)
                    }
                }

                releaseWakeLockIfHandlerIdle()
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    fun handleIntent(context: Context?, intent: Intent?): Boolean {
        val intentAction = intent?.action

        if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
            val event: KeyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                ?: return false

            val keyCode = event.keyCode
            val action = event.action
            val eventTime =
                if (event.eventTime != 0L) event.eventTime else System.currentTimeMillis()
            // Fallback to system time if event time was not available.
            var command = when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    MusicService.ACTION_STOP
                }

                KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    MusicService.ACTION_TOGGLE_PAUSE
                }

                KeyEvent.KEYCODE_MEDIA_NEXT -> MusicService.ACTION_SKIP
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MusicService.ACTION_REWIND
                KeyEvent.KEYCODE_MEDIA_PAUSE -> MusicService.ACTION_PAUSE
                KeyEvent.KEYCODE_MEDIA_PLAY -> MusicService.ACTION_PLAY
                else -> null
            }

            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        // Only consider the first event in a sequence, not the repeat events,
                        // so that we don't trigger in cases where the first event went to
                        // a different app (e.g. when the user ends a phone call by
                        // long pressing the headset button)

                        // The service may or may not be running, but we need to send it a command.
                        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                            keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        ) {
                            if (eventTime - mLastClickTime >= DOUBLE_CLICK) {
                                mClickCounter = 0
                            }

                            mClickCounter++
                            handler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)

                            val msg = handler.obtainMessage(
                                MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0, context
                            )
                            val delay = if (mClickCounter < 3) DOUBLE_CLICK else 0L
                            if (mClickCounter >= 3) {
                                mClickCounter = 0
                            }
                            mLastClickTime = eventTime
                            acquireWakeLockAndSendMessage(context, msg, delay)
                        } else {
                            startService(context, command)
                        }
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun startService(context: Context?, command: String) {
        val intent = Intent(context, MusicService::class.java)
        intent.action = command
        try {
            context?.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireWakeLockAndSendMessage(context: Context?, msg: Message, delay: Long) {
        if (mWakeLock == null) {
            val appContext = context?.applicationContext
            val powerManager = appContext?.getSystemService(Context.POWER_SERVICE) as PowerManager
            mWakeLock =
                powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MUSIC_PLAYER_HEADSET)
            mWakeLock?.setReferenceCounted(false)
        }
        // Make sure we don't indefinitely hold the wake lock under any circumstances
        mWakeLock?.acquire(10000)

        handler.sendMessageDelayed(msg, delay)
    }

    private fun releaseWakeLockIfHandlerIdle() {
        if (handler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
            return
        }
        if (mWakeLock != null) {
            mWakeLock?.release()
            mWakeLock = null
        }
    }

    companion object {
        private var TAG = MediaButtonIntentReceiver.javaClass.simpleName
        private const val MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2
        private const val DOUBLE_CLICK = 400L

        private const val MUSIC_PLAYER_HEADSET = ":MusicPlayer headset button"
    }
}
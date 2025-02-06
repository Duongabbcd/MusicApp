package com.example.service.service.notification

import android.app.Application.NOTIFICATION_SERVICE
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.service.service.MusicService

abstract class PlayingNotification {
    companion object {
        private const val NOTIFICATION_ID = 1

        private const val NOTIFY_MODE_FOREGROUND = 1
        private const val NOTIFY_MODE_BACKGROUND = 0
        const val NOTIFICATION_CHANNEL_ID = "playing_notification"
    }

    private var notifyMode = NOTIFY_MODE_BACKGROUND
    private lateinit var notificationManager: NotificationManager
    lateinit var service: MusicService
    var stopped = false

    fun init(service: MusicService) {
        this.service = service
        notificationManager = service.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    abstract fun update()

    fun stop() {
        synchronized(this) {
            stopped = true
            service.stopForeground(true)
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    fun updateNotifyModeAndPostNotification(notification: Notification) {
        try {
            service.startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music App",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description = "Music App"
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.setShowBadge(false)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

}
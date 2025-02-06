package com.example.service.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object Utils {

    fun getDuration(duration: Long): String {
        val hms: String
        if (duration > 3600000) {
            hms = String.format(
                Locale.US, "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(
                        duration
                    )
                ),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(
                        duration
                    )
                )
            )
        } else {
            hms = String.format(
                Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(
                        duration
                    )
                ),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(
                        duration
                    )
                )
            )
        }

        return hms
    }

    fun getPathDownload(): File {
        val rootFile: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            Environment.getExternalStorageDirectory()
        }
        return File("$rootFile/MusicDownload")
    }

    fun showFileSize(size: Long): String {
        val sizeString : String
        if (size < 1024000) {
            sizeString = (size / 1000).toString() + "kb"
        } else {
            val mmm = ((size / 1024).toString() + "").toInt()
            val f = DecimalFormat("#,###,###")
            sizeString = f.format(mmm.toLong()) + " MB"
        }
        return sizeString
    }

    fun shareVideoOrAudio(context: Context, title: String?, path: String?) {
        MediaScannerConnection.scanFile(context, arrayOf(path), null) { _: String, uri: Uri ->
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("*/*")
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
            shareIntent.putExtra(Intent.EXTRA_TITLE, title)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            if (context is Activity) {
                context.startActivity(Intent.createChooser(shareIntent, null))
            }
        }
    }

    fun shareAudioList(context: Context, paths: ArrayList<String>) {
        val uriList = arrayListOf<Uri>()

        //Convert path into Uri
        for (path in paths) {
            val uri = Uri.fromFile(File(path))
            uriList.add(uri)
        }

        //Create intent to share list sound files
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.setType("audio/*")
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (context is Activity) {
            context.startActivity(Intent.createChooser(shareIntent, "Share audio file by"))
        }
    }

    const val ODER_SORT_FILE_DEFAULT = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
    const val ODER_SORT_TITLE_ASC = MediaStore.Audio.Media.TITLE + " ASC"

    const val ACTION_FINISH_DOWNLOAD = "ACTION_FINISH_DOWNLOAD"
}

enum class DisplayMode() {
    ARTIST,
    ALBUM,
    AUDIO,
    PLAYLIST,
    PLAYING_SONG,
    DETAIL
}
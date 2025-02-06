package com.example.service.utils

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.service.model.Audio
import com.example.service.service.MusicPlayerRemote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class RemoveOrRenameFile() {
    fun deleteAudio(activity: Activity, id: Int) {
        try {
            val uri =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong())
            activity.contentResolver.delete(uri, null, null)

            val asyncTask = AsyncTaskDeleteSong(activity)
            asyncTask.execute()
        } catch (e: Exception) {

        }
    }

    fun renameAudio(activity: Activity, id: Int, pathSong: String, newName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            renameInMediaStoreO(activity, newName, id, pathSong)
        } else {
            renameInMediaStore(activity, newName, pathSong)
        }
    }

    private fun renameInMediaStore(activity: Activity, newName: String, pathSong: String) {
        try {
            val resolver = activity.contentResolver
            val values = ContentValues()
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
            resolver.update(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                values,
                MediaStore.Audio.Media.DATA + "='" + pathSong + "'",
                null
            )
            refreshFileMediaStore(activity, pathSong)
            activity.sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
        } catch (ex: Exception) {
        }
    }

    private fun renameInMediaStoreO(
        activity: Activity,
        newName: String,
        id: Int,
        pathSong: String
    ) {
        try {
            val resolver = activity.contentResolver
            val uri =
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong())
            val values = ContentValues()
            values.put(MediaStore.Audio.Media.DATA, pathSong)
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
            resolver.update(uri, values, null, null)
            activity.sendBroadcast(Intent(Utils.ACTION_FINISH_DOWNLOAD))
        } catch (ex: SecurityException) {
            requestRenameAudio(activity, ex)
        }
    }

    private fun requestRenameAudio(activity: Activity, ex: SecurityException) {
        startIntentSenderForResult(activity, ex, REQUEST_CODE_RENAME)
    }

    private fun startIntentSenderForResult(
        activity: Activity,
        ex: SecurityException,
        requestCode: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val sender =
                    (ex as RecoverableSecurityException).userAction.actionIntent.intentSender
                activity.startIntentSenderForResult(sender, requestCode, null, 0, 0, 0, null)
                MusicPlayerRemote.checkAfterDeletePlaying()
            } catch (e: Exception) {
                Log.d("DEBUG22", e.toString())
            }
        }
    }

    private fun refreshFileMediaStore(context: Context, path: String) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(File(path))
        intent.setData(contentUri)
        context.sendBroadcast(intent)
    }


    class AsyncTaskDeleteSong(private val context: Context) : AsyncTask<Void?, Void?, Boolean?>() {
        private var songId = ""
        override fun doInBackground(vararg params: Void?): Boolean? {
          try{
              var intNum = 0
              val cacheSongs =
                  FileUtils.read(Keys.KEY_AUDIO_CACHE, context) as java.util.ArrayList<*>?
              if (!cacheSongs.isNullOrEmpty()) {
                  for (value in cacheSongs.size - 1 downTo 0) {
                      val audio = cacheSongs[value]
                      if ((audio as Audio).mediaObject?.id == songId) {
                          cacheSongs.remove(audio)
                          intNum++
                      }
                  }
              }

              if (intNum > 0) {
                  FileUtils.write(Keys.KEY_AUDIO_CACHE, context, cacheSongs as Any)
              }

              //change in cache recently
              intNum = 0
              val cachedRecently =
                  FileUtils.read(Keys.KEY_AUDIO_CACHE, context = context) as ArrayList<Audio>
              if (cachedRecently != null && cachedRecently.isNotEmpty()) {
                  for (value in cacheSongs!!.size - 1 downTo 0) {
                      val audio = cachedRecently[value]
                      if (audio.mediaObject?.id == songId) {
                          cachedRecently.remove(audio)
                          intNum++
                      }
                  }
              }

              if (intNum > 0) {
                  FileUtils.write(Keys.KEY_AUDIO_CACHE, context, cacheSongs as Any)
              }

              //modify in playlist
              CoroutineScope(Dispatchers.IO).launch {
                  val playlists = PlaylistUtils.getInstance(context)?.getPlaylists()
                  playlists?.let {
                      for (i in 0 until it.size) {
                          val playlist = playlists[i]
                          intNum = 0
                          for (j in playlist.tracks.size - 1 downTo 0) {
                              val audio = playlist.tracks[j]
                              if (audio.mediaObject!!.id == songId) {
                                  playlist.tracks.remove(audio)
                                  intNum++
                              }
                          }
                          if (intNum > 0) {
                              PlaylistUtils.getInstance(context)?.savePlaylist(playlist)
                          }
                      }

                      //modify in playing
                      val playingList = MusicPlayerRemote.getPlayingQueue()
                      if (playingList != null && playingList.isNotEmpty()) {
                          for (value in playingList.size - 1 downTo 0) {
                              val audio = playingList[value] as Audio
                              if (audio.mediaObject?.id == songId) {
                                  MusicPlayerRemote.removeFromQueue(value)
                              }
                          }
                          MusicPlayerRemote.checkAfterDeletePlaying()
                      }
                  }
              }

              return null
          } catch (e: Exception) {
              e.printStackTrace()
              return null
          }
        }
    }

    companion object {
        const val REQUEST_CODE_RENAME = 301
        const val REQUEST_CODE_DELETE = 300
    }
}
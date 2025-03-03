package com.example.service.utils

import android.content.Context
import com.example.service.database.AppDatabase
import com.example.service.model.Audio
import com.example.service.model.Playlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class PlaylistUtils(private val context: Context) {
    private val appDatabase = AppDatabase.getInstance(context)
    private val playlistDao = appDatabase.playlistDao()

    suspend fun getPlaylists(): List<Playlist> {
        val result = CoroutineScope(Dispatchers.IO).async {
            playlistDao.getAllPlaylists()
        }
        return result.await()
    }

    suspend fun getPlaylistById(id: Int): Playlist {
        val result = CoroutineScope(Dispatchers.IO).async {
            playlistDao.getPlaylistById(id)
        }
        return result.await()
    }

    /**
     * create new playlist and save to file.
     *
     * @param audio  the audio file will be added into playlist.
     * @param playlist playlist.
     */
    fun addSongToPlaylist(audio: Audio?, playlist: Playlist?): Boolean {
        try {
            playlist?.tracks?.let {
                for (i in 0 until it.size) {
                    if (playlist.tracks[i] == audio) {
                        return false
                    }
                }
                playlist.tracks.add(audio!!)
                CoroutineScope(Dispatchers.IO).launch {
                    playlistDao.updatePlaylist(playlist)
                }
                return true
            }
            return false
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    suspend fun addListSongToPlaylistOne(
        dataList: MutableList<Audio>,
        playlist: Playlist?,
    ) {
        try {
            playlist?.let {
                it.tracks.addAll(dataList)
                playlistDao.updatePlaylist(playlist)
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * Save playlist to file
     *
     * @param playlist playlist to saved
     */
    suspend fun savePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist)
    }

    /**
     * remove a playlist and save to file
     */
    fun dropPlaylist(playlist: Playlist) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                playlistDao.deletePlaylistById(playlist.id!!)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    /**
     * create new playlist and save to file.
     *
     * @param title  title of new playlist
     * @param tracks list song of new playlist.
     */
    suspend fun newPlaylist(title: String, listData: List<Audio>, context: Context) {
        val playlist = Playlist()
        playlist.title = title
        if (listData.isNotEmpty()) {
            playlist.tracks = ArrayList(listData)
        }
        playlistDao.insertPlaylist(playlist).also {
            println("addPlaylist: successfully")
        }

    }

    /**
     * check if a song is favourite or not
     */
    fun checkIsFavourite(audio: Audio): Boolean {
        val favPlaylist = playlistDao.getFavouritePlaylist()
        if (favPlaylist.tracks.isNotEmpty()) {
            for (i in 0 until favPlaylist.tracks.size) {
                println("checkIsFavourite: ${favPlaylist.tracks[i] == audio}")
                if (favPlaylist.tracks[i] == audio) {
                    return true
                }
            }
        }
        return false
    }


    fun addToFavourite(currentSong: Audio?) {
        val favPlaylist = playlistDao.getFavouritePlaylist()
        currentSong?.let {
            favPlaylist.tracks.add(it)
        }
        playlistDao.updatePlaylist(favPlaylist)
    }

    /**
     * remove a song from favorite playlist , then save playlist to file
     */
    fun removeFromFavourite(audio: Audio) {
        val favPlaylist = playlistDao.getFavouritePlaylist()
        for (i in favPlaylist.tracks.size - 1 downTo 0) {
            if (favPlaylist.tracks[i] == audio) {
                favPlaylist.tracks.removeAt(i)
            }
        }
        playlistDao.updatePlaylist(favPlaylist)
    }


    suspend fun checkNameExists(input: String): Boolean {
        try {
            getPlaylists().let {
                if (it.isNotEmpty()) {
                    for (i in 0 until it.size) {
                        println("checkNameExists: $i and ${it[i]}")
                        val playlist = it[i]
                        if (playlist.title == input)
                            return true
                    }
                }
            }
        } catch (ex: Exception) {
            return false
        }

        return false
    }

    companion object {
        const val FAVOURITE_SONGS = "FavouriteSongs"
        const val FAVOURITE = "FAVOURITES"

        private var sInstance: PlaylistUtils? = null
        private var favouriteId: Int = -1
        fun getInstance(context: Context): PlaylistUtils? {
            if (sInstance == null) {
                sInstance = PlaylistUtils(context.applicationContext)
            }
            return sInstance
        }

        fun favouriteId() = favouriteId
    }
}
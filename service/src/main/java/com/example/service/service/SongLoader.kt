package com.example.service.service

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import com.example.service.model.Album
import com.example.service.model.Artist
import com.example.service.model.Audio
import com.example.service.model.Genres
import com.example.service.model.MediaObject
import com.example.service.utils.PreferenceUtil
import com.example.service.utils.Utils

object SongLoader {

    private const val AUDIO_SELECTION =
        Media.IS_MUSIC + "!= 0 AND " + Media.MIME_TYPE + " LIKE 'audio/%'"

    private val AUDIO_PROJECTION = arrayOf(
        BaseColumns._ID, // 0
        Media.TITLE, // 1,
        Media.TRACK, // 2,
        Media.YEAR, // 3,
        Media.DURATION, // 4,
        Media.DATA, // 5,
        Media.DATE_MODIFIED, // 6,
        Media.ALBUM_ID, // 7,
        Media.ALBUM, // 8,
        Media.ARTIST_ID, // 9,
        Media.ARTIST, // 10
    )
    private const val UNKNOWN = "Unknown"


    fun getAllSongs(context: Context): ArrayList<Audio> {
        val cursor = makeSongCursor(context, null, null)
        return getSongs(cursor)
    }

    fun getAllSongsDownload(context: Context): ArrayList<Audio> {
        val cursor = makeSongCursorDownload(
            context,
            null,
            null,
            PreferenceUtil.getInstance(context)?.getSongSortOrder()
        )
        return getSongs(cursor)
    }

    private fun makeSongCursorDownload(
        context: Context,
        selection: String?,
        selectionValue: Array<String>?,
        songSortOrder: String?
    ): Cursor? {
        val currentSelection = if (selection != null && selection.trim() != "") {
            AUDIO_SELECTION + " AND " + selection + " AND " + Media.DATA + " LIKE ?"
        } else {
            AUDIO_SELECTION + " AND " + Media.DATA + " LIKE ?"
        }
        var newSelectionValue = arrayOf<String>()
        if (selectionValue != null) {

            newSelectionValue = Array(selectionValue.size + 1) { "" }
            System.arraycopy(selectionValue, 0, newSelectionValue, 0, selectionValue.size)
            newSelectionValue[selectionValue.size] = "%" + Utils.getPathDownload() + "%"


        } else {
            newSelectionValue = arrayOf("%" + Utils.getPathDownload() + "%")
        }
        try {
            return context.contentResolver.query(
                Media.EXTERNAL_CONTENT_URI,
                AUDIO_PROJECTION,
                currentSelection, newSelectionValue, songSortOrder
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            return null
        }
    }

    fun getSongs(cursor: Cursor?): ArrayList<Audio> {
        val songs = arrayListOf<Audio>()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val song = getSongFromCursorImpl(cursor)
                if(song.timeSong != "00:00") {
                    songs.add(song)
                }
            } while (cursor.moveToNext())
        }

        cursor?.close()
        return songs
    }

    private fun getSongFromCursorImpl(cursor: Cursor): Audio {
        val id = cursor.getString(0)
        val title = cursor.getString(1)
        val trackNumber = cursor.getInt(2)
        val year = cursor.getInt(3)
        val duration = cursor.getLong(4)
        val data = cursor.getString(5)
        val dateModified = cursor.getLong(6)
        val albumId = cursor.getInt(7)
        val albumName = cursor.getString(8)
        val artistId = cursor.getInt(9)
        val artistName = cursor.getString(10)

        return Audio(
            artist = artistName,
            timeSong = Utils.getDuration(duration),
            albumName = albumName,
            timeCount = duration,
            mediaObject = MediaObject(id.toString(), title, "", data)
        )
    }

    fun makeSongCursor(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?
    ): Cursor? {
        return makeSongCursor(
            context,
            selection,
            selectionValues,
            PreferenceUtil.getInstance(context)?.getSongSortOrder()
        )
    }

    private fun makeSongCursor(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val customSelection = if (selection != null && selection.trim() != "") {
            "$AUDIO_SELECTION AND $selection"
        } else AUDIO_SELECTION

        return try {
            context.contentResolver.query(
                Media.EXTERNAL_CONTENT_URI,
                AUDIO_PROJECTION, customSelection, selectionValues, sortOrder
            )
        } catch (e: SecurityException) {
            null
        }
    }

    private fun makeAlbumSongCursorById(albumId: Long, context: Context): Cursor? {
        val selection = StringBuilder()
        selection.append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=1")
        selection.append(" AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''")
        selection.append(" AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + "=" + albumId)

        return context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI, null, selection.toString(),
            null, Media.TRACK + ", " +  PreferenceUtil.getInstance(context)?.getSongSortOrder()
        )
    }

    fun getSongAlbumById(albumId: Long, context: Context): ArrayList<Audio> {
        val songList = arrayListOf<Audio>()
        try {
            val audioCursor = makeAlbumSongCursorById(albumId, context)
            if (audioCursor != null && audioCursor.moveToFirst()) {

                val id = audioCursor.getColumnIndexOrThrow(BaseColumns._ID)
                val nameIndex = audioCursor.getColumnIndexOrThrow(Media.DISPLAY_NAME)
                val artistIndex = audioCursor.getColumnIndexOrThrow(Media.ARTIST)
                val uriIndex = audioCursor.getColumnIndexOrThrow(Media.DATA)
                val durationIndex = audioCursor.getColumnIndexOrThrow(Media.DURATION)
                val sizeIndex = audioCursor.getColumnIndexOrThrow(Media.SIZE)
                val dateIndex = audioCursor.getColumnIndexOrThrow(Media.DATE_ADDED)
                val albumIndex = audioCursor.getColumnIndexOrThrow(Media.ALBUM_ID)
                val albumNameIndex = audioCursor.getColumnIndexOrThrow(Media.ALBUM)

                do {
                    val audioId = audioCursor.getInt(id).toString()
                    val path = audioCursor.getString(uriIndex)
                    val title = audioCursor.getString(nameIndex)
                    val artist = audioCursor.getString(artistIndex)
                    val imageThumb = audioCursor.getString(albumIndex)
                    val timeSong = Utils.getDuration(audioCursor.getLong(durationIndex))
                    val timeCount = audioCursor.getLong(durationIndex)
                    val fileSize = audioCursor.getLong(sizeIndex)
                    val timeModified = audioCursor.getLong(dateIndex)
                    val albumName = audioCursor.getString(albumNameIndex)
                    val audio = Audio(
                        artist = artist,
                        timeSong = timeSong,
                        albumName = albumName,
                        timeCount = timeCount,
                        mediaObject = MediaObject(id.toString(), title, "", path)
                    )
                    if(timeSong != "00:00") {
                        songList.add(audio)
                    }

                } while (audioCursor.moveToNext())
                audioCursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songList
    }

    fun getAlbumLocal(context: Context): List<Album> {
        val list = arrayListOf<Album>()
        try {
            val audioCursor = context.contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
            )
            if (audioCursor != null) {
                audioCursor.moveToFirst()
                val albumId = audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumName = audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val songCount =
                    audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                val artistKey = audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                do {
                    val album = Album(
                        audioCursor.getString(albumId),
                        audioCursor.getString(albumName),
                        audioCursor.getString(songCount),
                        audioCursor.getString(artistKey)
                    )
                    list.add(album)
                } while (audioCursor.moveToNext())
                audioCursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getArtistLocal(context: Context): List<Artist> {
        val list = arrayListOf<Artist>()
        try {
            val artistCursor = context.contentResolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
            )
            if (artistCursor != null) {
                artistCursor.moveToFirst()
                val artistId = artistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val artistName = artistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val numberOfTracks =
                    artistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
                val numberOfAlbum =
                    artistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                do {
                    val artist = Artist(
                        artistCursor.getString(artistId),
                        artistCursor.getString(artistName),
                        artistCursor.getString(numberOfTracks),
                        artistCursor.getString(numberOfAlbum),
                    )
                    list.add(artist)
                } while (artistCursor.moveToNext())
                artistCursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getSongByArtistId(artistId: Long, context: Context): List<Audio> {
        val artistList = arrayListOf<Audio>()
        try {
            val artistCursor = makeArtistAlbumCursor(artistId, context)
            if (artistCursor != null && artistCursor.moveToFirst()) {

                val id = artistCursor.getColumnIndexOrThrow(BaseColumns._ID)
                val nameIndex = artistCursor.getColumnIndexOrThrow(Media.DISPLAY_NAME)
                val artistIndex = artistCursor.getColumnIndexOrThrow(Media.ARTIST)
                val uriIndex = artistCursor.getColumnIndexOrThrow(Media.DATA)
                val durationIndex = artistCursor.getColumnIndexOrThrow(Media.DURATION)
                val sizeIndex = artistCursor.getColumnIndexOrThrow(Media.SIZE)
                val dateIndex = artistCursor.getColumnIndexOrThrow(Media.DATE_ADDED)
                val albumIndex = artistCursor.getColumnIndexOrThrow(Media.ALBUM_ID)
                val albumNameIndex = artistCursor.getColumnIndexOrThrow(Media.ALBUM)

                do {
                    val audioId = artistCursor.getInt(id).toString()
                    val path = artistCursor.getString(uriIndex)
                    val title = artistCursor.getString(nameIndex)
                    val artist = artistCursor.getString(artistIndex)
                    val imageThumb = artistCursor.getString(albumIndex)
                    val timeSong = Utils.getDuration(artistCursor.getLong(durationIndex))
                    val timeCount = artistCursor.getLong(durationIndex)
                    val fileSize = artistCursor.getLong(sizeIndex)
                    val timeModified = artistCursor.getLong(dateIndex)
                    val albumName = artistCursor.getString(albumNameIndex)
                    val audio = Audio(
                        artist = artist,
                        timeSong = timeSong,
                        albumName = albumName,
                        timeCount = timeCount,
                        mediaObject = MediaObject(id.toString(), title, "", path)
                    )
                    artistList.add(audio)
                } while (artistCursor.moveToNext())
                artistCursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return artistList
    }

    private fun makeArtistAlbumCursor(artistId: Long, context: Context): Cursor? {
        try {
            val selection = StringBuilder()
            selection.append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=1")
            selection.append(" AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''")
            selection.append(" AND " + MediaStore.Audio.AudioColumns.ARTIST_ID + "=" + artistId)

            return context.contentResolver.query(
                Media.EXTERNAL_CONTENT_URI, null, selection.toString(),
                null, PreferenceUtil.getInstance(context)?.getSongSortOrder()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getGenresLocal(context: Context): List<Genres> {
        val artistList = arrayListOf<Genres>()

        try {
            val audioCursor = context.contentResolver.query(
                Media.EXTERNAL_CONTENT_URI, null, null,
                null, Media.DEFAULT_SORT_ORDER
            )
            if (audioCursor != null) {
                val genresId = audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val genresName = audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)

                if (audioCursor.moveToFirst()) {
                    do {
                        val genres = Genres(
                            audioCursor.getLong(genresId), audioCursor.getString(genresName)
                        )
                        artistList.add(genres)
                    } while (audioCursor.moveToNext())
                }
                audioCursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return artistList
    }

    fun getSongByGenresId(genresId: Long, context: Context): List<Audio> {
        val songList = arrayListOf<Audio>()
        try {
            val uri = MediaStore.Audio.Genres.Members.getContentUri("external", genresId)
            val audioCursor = context.contentResolver.query(uri, null, null, null, null)

            if (audioCursor != null && audioCursor.moveToFirst()) {
                val id = audioCursor.getColumnIndexOrThrow(Media._ID)
                val nameIndex = audioCursor.getColumnIndexOrThrow(Media.DISPLAY_NAME)
                val artistIndex = audioCursor.getColumnIndexOrThrow(Media.ARTIST)
                val uriIndex = audioCursor.getColumnIndexOrThrow(Media.DATA)
                val durationIndex = audioCursor.getColumnIndexOrThrow(Media.DURATION)
                val sizeIndex = audioCursor.getColumnIndexOrThrow(Media.SIZE)
                val dateIndex = audioCursor.getColumnIndexOrThrow(Media.DATE_ADDED)
                val albumIndex = audioCursor.getColumnIndexOrThrow(Media.ALBUM_ID)
                val albumNameIndex = audioCursor.getColumnIndexOrThrow(Media.ALBUM)

                do {
                    val audioId = audioCursor.getInt(id).toString()
                    val path = audioCursor.getString(uriIndex)
                    val title = audioCursor.getString(nameIndex)
                    val artist = audioCursor.getString(artistIndex)
                    val imageThumb = audioCursor.getString(albumIndex)
                    val timeSong = Utils.getDuration(audioCursor.getLong(durationIndex))
                    val timeCount = audioCursor.getLong(durationIndex)
                    val fileSize = audioCursor.getLong(sizeIndex)
                    val timeModified = audioCursor.getLong(dateIndex)
                    val albumName = audioCursor.getString(albumNameIndex)

                    val audio = Audio(
                        artist = artist,
                        timeSong = timeSong,
                        albumName = albumName,
                        timeCount = timeCount,
                        mediaObject = MediaObject(id.toString(), title, "", path)
                    )
                    songList.add(audio)
                } while (audioCursor.moveToNext())
                audioCursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songList
    }

}
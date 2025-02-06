package com.example.service.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.service.model.Playlist.Companion.PLAYLIST_TABLE_NAME
import com.example.service.utils.Converters
import java.io.Serializable
import java.util.UUID

@Entity(tableName = PLAYLIST_TABLE_NAME)
@TypeConverters(Converters::class)
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = PLAYLIST_TABLE_ID)
    var id: Int? = null ,
    @ColumnInfo(name = PLAYLIST_TABLE_TITLE)
    var title: String = "",
    @ColumnInfo(name = PLAYLIST_TABLE_ARTIST)
    var artists: String = "",
    @ColumnInfo(name = PLAYLIST_TABLE_TRACKS)
    var tracks: ArrayList<Audio> = arrayListOf(),
    @ColumnInfo(name = PLAYLIST_TABLE_THUMB_URI)
    var thumbUri: String = ""
) : Serializable {
    fun getTracks1() : ArrayList<Audio> {
        if(tracks == null) {
            return arrayListOf()
        }

        val list = arrayListOf<Audio>()
        for( value in 0..tracks.size) {
            val audio = tracks[value] as Audio
            list.add(audio)
        }
        return list
    }

    fun add(track: Audio) {
        if(!this.tracks.isNullOrEmpty()) {
            this.tracks = arrayListOf()
        }
        this.tracks.add(track)
    }

    companion object {
        const val PLAYLIST_TABLE_NAME = "Playlist"
        const val PLAYLIST_TABLE_ID = "id"
        const val PLAYLIST_TABLE_TITLE = "title"
        const val PLAYLIST_TABLE_ARTIST = "artist"
        const val PLAYLIST_TABLE_TRACKS = "tracks"
        const val PLAYLIST_TABLE_THUMB_URI = "thumbUri"
    }
}
package com.example.service.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.service.model.Playlist

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaylist(playlist: Playlist)

    @Update
    fun updatePlaylist(playlist: Playlist)

    @Query("DELETE FROM ${Playlist.PLAYLIST_TABLE_NAME} WHERE ${Playlist.PLAYLIST_TABLE_ID} = -1")
    fun deleteAllPlaylists()

    @Query("DELETE FROM ${Playlist.PLAYLIST_TABLE_NAME} WHERE ${Playlist.PLAYLIST_TABLE_ID} = :playlistId")
    fun deletePlaylistById(playlistId: Int)

    @Query("SELECT * FROM ${Playlist.PLAYLIST_TABLE_NAME}")
    suspend fun getAllPlaylists(): List<Playlist>

    @Query("SELECT * FROM ${Playlist.PLAYLIST_TABLE_NAME}  WHERE ${Playlist.PLAYLIST_TABLE_ID} = :playlistId")
    suspend fun getPlaylistById(playlistId: Int): Playlist

    @Query("SELECT * FROM ${Playlist.PLAYLIST_TABLE_NAME}  WHERE ${Playlist.PLAYLIST_TABLE_ID} = -1")
    fun getFavouritePlaylist(): Playlist

}
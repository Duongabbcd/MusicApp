package com.example.service.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.service.dao.PlaylistDao
import com.example.service.model.Playlist

@Database(entities = [Playlist::class], version = 1)
abstract class AppDatabase(): RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var instance : AppDatabase ?= null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "MusicAppDatabase"
                ).addCallback(roomCallback(context)).build().also {
                    instance = it
                }
        }

        private fun roomCallback(context: Context) = object: RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Thread {
                    val defaultPlaylist = Playlist()
                    defaultPlaylist.id = -1
                    defaultPlaylist.title = "My Favourite"
                    getInstance(context).playlistDao().insertPlaylist(defaultPlaylist)
                }.start()
            }
        }
    }
}
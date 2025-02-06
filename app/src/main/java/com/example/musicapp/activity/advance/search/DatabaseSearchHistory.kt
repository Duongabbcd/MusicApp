package com.example.musicapp.activity.advance.search

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseSearchHistory(
    context: Context,
    factory: SQLiteDatabase.CursorFactory? = null
) : SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        Log.i(TAG, "MyDatabaseHelper.onCreate ...")
        //Script create table
        val script = "CREATE TABLE $TABLE_HISTORY($COLUMN_HISTORY_TITLE TEXT)"
        //Execute the table creating
        db?.execSQL(script)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "MyDatabaseHelper.onCreate ...")
        //Drop the old table if it exists
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")

        //Recreate
        onCreate(db)
    }

    fun addHistoryNotExist(history: String) {
        val db = this.writableDatabase

        val cursor = db.query(
            TABLE_HISTORY,
            null,
            COLUMN_HISTORY_TITLE + "=?",
            arrayOf(history.lowercase().trim()),
            null,
            null,
            "1"
        )

        if (cursor.moveToFirst()) {
            cursor.close()
            return
        }

        val values = ContentValues()
        values.put(COLUMN_HISTORY_TITLE, history)
        db.insert(TABLE_HISTORY, null, values)
        cursor.close()
        if (db.isOpen) {
            db.close()
        }
    }

    fun getHistoriesFromString(keyWord: String): ArrayList<String> {
        val histories = arrayListOf<String>()
        try {
            val db = this.writableDatabase
            val cursor = db.query(
                TABLE_HISTORY,
                null,
                COLUMN_HISTORY_TITLE + " like ? ",
                arrayOf("%" + keyWord.lowercase().trim() + "%"),
                null,
                null,
                "1"
            )

            while(cursor.moveToNext()) {
                val index = cursor.getColumnIndexOrThrow(COLUMN_HISTORY_TITLE)
                histories.add(cursor.getString(index))
            }
            cursor.close()
            if(db.isOpen) {
                db.close()
            }
        } catch (ex:Exception) {
            ex.printStackTrace()
        }
        return histories
    }

    companion object {

        private val TAG = DatabaseSearchHistory.javaClass.simpleName

        private const val DATABASE_NAME = "History_Manager"
        private const val DATABASE_VERSION = 1

        private const val TABLE_HISTORY = "History"
        private const val COLUMN_HISTORY_TITLE = "History_Title"
    }
}
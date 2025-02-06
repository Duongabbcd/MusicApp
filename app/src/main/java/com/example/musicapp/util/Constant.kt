package com.example.musicapp.util


import android.provider.MediaStore

object Constant {
    const val UNKNOWN_STRING = "unknown"
    const val ACTION_FINISH_DOWNLOAD = "ACTION_FINISH_DOWNLOAD"

    const val ODER_SORT_TITLE_DESC = MediaStore.Audio.Media.TITLE + " DESC"
    const val ODER_SORT_TITLE_ASC = MediaStore.Audio.Media.TITLE + " ASC"
    const val ODER_SORT_DURATION_DESC = MediaStore.Audio.Media.DURATION + " DESC"
    const val ODER_SORT_DURATION_ASC = MediaStore.Audio.Media.DURATION + " ASC"
    const val ODER_SORT_DATE_ADD_DESC = MediaStore.Audio.Media.DATE_ADDED + " DESC"
    const val ODER_SORT_DATE_ADD_ASC = MediaStore.Audio.Media.DATE_ADDED + " ASC"
    const val ODER_SORT_FILE_SIZE_DESC = MediaStore.Audio.Media.SIZE + " DESC"
    const val ODER_SORT_FILE_SIZE_ASC = MediaStore.Audio.Media.SIZE + " ASC"
    const val ODER_SORT_FILE_DEFAULT = MediaStore.Audio.Media.DEFAULT_SORT_ORDER

}



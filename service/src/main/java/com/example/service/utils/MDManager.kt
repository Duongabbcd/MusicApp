package com.example.service.utils

import android.content.Context
import android.widget.Toast

class MDManager(context: Context) {

    fun showMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    companion object {
        private var sInstance: MDManager? = null
        fun getInstance(context: Context): MDManager? {
            if (sInstance == null) {
                sInstance = MDManager(context.applicationContext)
            }
            return sInstance
        }
    }
}
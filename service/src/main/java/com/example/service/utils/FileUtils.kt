package com.example.service.utils

import android.content.Context
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object FileUtils {
    fun write(fileName: String, context: Context, obj: Any): Boolean {
        var success = false
        try {
            val fos: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            os.writeObject(obj)
            fos.flush()
            os.close()
            success = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return success
    }

    fun read(fileName: String, context: Context): Any? {
        var obj: Any? = null
        try {
            val fis: FileInputStream = context.openFileInput(fileName)
            val os = ObjectInputStream(fis)
            obj = os.readObject()
            fis.close()
            os.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return obj
    }
}
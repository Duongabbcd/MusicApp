package com.example.service.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Arrays


object PermissionUtils {
    fun havePermission(context: Context): Boolean {
        for (permission in getNeedPermissions()) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }

        }
        return true
    }

    private fun getNeedPermissions(): Array<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun getOptionalPermissions(): Array<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            return arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        }
        return arrayOf()
    }

    fun requestPermission(activity: Activity) {
        if (!havePermission(activity)) {
            val allPerms: ArrayList<Array<String>> = ArrayList(listOf(getNeedPermissions()))
            allPerms.addAll(listOf(getOptionalPermissions()))

            activity.requestPermissions(allPerms.flatMap { it.asList() }.toTypedArray(), 0)
        }
    }
}
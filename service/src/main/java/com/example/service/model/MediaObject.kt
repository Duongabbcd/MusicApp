package com.example.service.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class MediaObject(
    var id: String = "",
    var title: String = "",
    var imageThumb: String = "",
    var path: String = "",
    var timeModified: Long = 0L,
    var fileSize: Long = 0L,
    var isAddPlaylist: Boolean = false,
    var playedTime: Int = 0,
    var uuid: String = ""
) : Parcelable, Serializable, Cloneable {
    companion object {
        var  serialVersionUID = 1L
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }


    fun equals(obj: Any?, includeUUID: Boolean) : Boolean {
        if(obj == null) {
            return false
        }
        val path = id
        var objPath = ""
        if(obj is MediaObject) {
            objPath = (obj as MediaObject).id
        } else return false

        if(path == objPath) {
            isAddPlaylist =  true
            return true
        }
        return false
    }

}
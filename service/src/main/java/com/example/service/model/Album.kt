package com.example.service.model

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class Album(
    var albumId: String = "",
    var albumName: String = "",
    var numberSong: String = "",
    var artistKey: String = "",
) : Serializable, Parcelable, Cloneable {
    constructor(parcel: Parcel) : this(
        albumId = parcel.readString() ?: "",
        albumName = parcel.readString() ?: "",
        numberSong = parcel.readString() ?: "",
        artistKey = parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(albumId)
        parcel.writeString(albumName)
        parcel.writeString(numberSong)
        parcel.writeString(artistKey)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Album> {
        override fun createFromParcel(parcel: Parcel): Album {
            return Album(parcel)
        }

        override fun newArray(size: Int): Array<Album?> {
            return arrayOfNulls(size)
        }
    }
}
package com.example.service.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable


@Parcelize
data class Artist(
    val artistId: String,
    val nameArtist: String,
    val numberSong: String,
    val numberOfAlbum: String,
): Parcelable, Serializable, Cloneable
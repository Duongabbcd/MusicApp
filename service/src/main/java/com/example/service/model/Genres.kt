package com.example.service.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class Genres(
    val genresId: Long,
    val genresName: String
): Parcelable, Serializable, Cloneable
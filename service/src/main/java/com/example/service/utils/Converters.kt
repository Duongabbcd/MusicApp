package com.example.service.utils

import androidx.room.TypeConverter
import com.example.service.model.Audio
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    @TypeConverter
    fun fromAudioEntityList(tracks :ArrayList<Audio>?) : String {
        if(tracks == null) {
            return ""
        }
        return Gson().toJson(tracks)
    }

    @TypeConverter
    fun toAudioEntityList(tracks: String?) : ArrayList<Audio> {
        if(tracks == null) {
            return arrayListOf()
        }
        val listType = object : TypeToken<ArrayList<Audio>> (){}.type
        return Gson().fromJson(tracks, listType)
    }
}
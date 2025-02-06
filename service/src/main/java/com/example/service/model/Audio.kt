package com.example.service.model

import java.io.Serializable

data class Audio(
    var artist: String = "",
    var timeSong: String = "",
    var albumName: String = "",
    var timeCount: Long = -1L,
    var strLinkDownload: String = "",
    var taskId: Int = 0,
    var filePath: String = "",
    var totalLength: Long = -1L,
    var redirect: Boolean = false,
    var licence: String = "",
    var downloadAble: Boolean = false,
    var mediaObject: MediaObject? = null,
    var videoType: String = "",
    var videoEntity: Any? = null,
    var ccmixterReferrer: String = "",
    var isOnline: Boolean = false,
    var genres: String = ""
): Serializable {
    companion object {
        var EMPTY_SONG = Audio(
            "-1", "", "",
            -1L, "", 0, "", -1L, false, "", false, null, "", Video(),"", genres = ""
        )
    }



}
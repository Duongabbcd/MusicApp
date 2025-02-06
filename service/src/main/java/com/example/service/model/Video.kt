package com.example.service.model

import java.io.Serializable

data class Video (
    var redirect: Boolean = false,
    var duration: Int = 0,
    var durationString: String = "",
    var licence: String = "",
    var downloadAble: Boolean = false,
    var imageLink: String = "",
    var streamLink: String = "",
    var artistName: String = "",
    var videoType: String = "",
    var videoTitle: String = "",
    var videoId: String = "",
    var taskId: Int = 0,
    var filePath: String = "",
    var totalLength: Long = -1L,
    var isPlayed: Boolean = false,
    var ccmixterReferrer: String = "",
): Serializable {
}

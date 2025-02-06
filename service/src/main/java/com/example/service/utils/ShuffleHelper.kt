package com.example.service.utils

import com.example.service.model.Audio

object ShuffleHelper {

    fun makeShuffleList(listToShuffle: ArrayList<Any>, current: Int) {
        if (listToShuffle.isEmpty()) return
        if (current >= 0) {
           val obj = listToShuffle[current].also {
               listToShuffle.removeAt(current)
           }
            if (obj is Audio) {
                listToShuffle.shuffle()
                listToShuffle.add(0, obj)
            }
        } else {
            listToShuffle.shuffle()
        }
    }

}
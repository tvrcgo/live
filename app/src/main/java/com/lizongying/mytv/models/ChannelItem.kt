package com.lizongying.mytv.models

import com.lizongying.mytv.TV

class ChannelItem(private val tv: TV) {

    var rowPosition: Int = 0
    var itemPosition: Int = 0

    fun getTV(): TV {
        return tv
    }

    fun currentUrl(): String {
        return tv.videoUrl.lastOrNull().orEmpty()
    }
}

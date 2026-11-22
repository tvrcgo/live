package com.chacha.live

import java.io.Serializable

data class TV(
    var id: Int = 0,
    var title: String,
    var videoUrl: List<String>,
    var channel: String = "",
    // 源里 m3u 的 tvg-logo，按名字取不到 CDN 台标时用它兜底
    var sourceLogo: String = "",
    var sourceName: String = "",

    ) : Serializable {

    override fun toString(): String {
        return "TV{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", logo='" + sourceLogo + '\'' +
                ", sourceName='" + sourceName + '\'' +
                '}'
    }
}

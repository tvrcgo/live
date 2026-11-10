package com.lizongying.mytv

import java.io.Serializable

data class TV(
    var id: Int = 0,
    var title: String,
    var videoUrl: List<String>,
    var channel: String = "",
    // 最终要显示的台标地址：按频道名匹配到的 CDN 地址，否则回退源里的 tvg-logo，都没有为空
    var logo: String = "",
    var sourceName: String = "",

    ) : Serializable {

    override fun toString(): String {
        return "TV{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", logo='" + logo + '\'' +
                ", sourceName='" + sourceName + '\'' +
                '}'
    }
}

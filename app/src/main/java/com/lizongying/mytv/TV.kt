package com.lizongying.mytv

import java.io.Serializable

data class TV(
    var id: Int = 0,
    var title: String,
    var videoUrl: List<String>,
    var channel: String = "",
    // 按频道名推导的首选台标地址；取不到依次退到 logoAlt、sourceLogo
    var logo: String = "",
    var logoAlt: String = "",
    // 源里 m3u 的 tvg-logo
    var sourceLogo: String = "",
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

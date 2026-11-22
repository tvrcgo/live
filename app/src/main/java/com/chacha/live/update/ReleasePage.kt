package com.chacha.live.update

object ReleasePage {

    private const val GITHUB_URL = "https://github.com"
    private const val TAG_PATH = "/releases/tag/"
    private const val APK_SUFFIX = ".apk"
    private const val APK_NAME_PREFIX = "live-"
    private val hrefPattern = Regex("href=\"([^\"]+)\"")

    // tag 段是资产页路径的一部分，保留 v 前缀
    fun tagFromRedirect(location: String): String? {
        val index = location.lastIndexOf(TAG_PATH)
        if (index < 0) {
            return null
        }
        return location.substring(index + TAG_PATH.length).trim('/').ifEmpty { null }
    }

    // 资产 href 是仓库内相对路径，需补域名；旧版本资产名为 my-tv-，live- 只作优先项
    fun apkUrl(html: String): String? {
        val links = hrefPattern.findAll(html)
            .map { it.groupValues[1] }
            .filter { it.endsWith(APK_SUFFIX) }
            .toList()
        val link = links.firstOrNull { it.substringAfterLast('/').startsWith(APK_NAME_PREFIX) }
            ?: links.firstOrNull()
            ?: return null
        return if (link.startsWith("http")) link else GITHUB_URL + link
    }
}

package com.chacha.live.logo

/**
 * 台标解析结果的本地记忆：成功记住文件名，失败记住时间。
 * 失败 7 天内不再重试，成功 30 天后重新推导，两者的取舍见方案文档。
 */
class LogoCache {

    data class Entry(val file: String = "", val failed: Boolean = false, val at: Long = 0L)

    data class Stored(val version: Long = 0L, val entries: Map<String, Entry>? = null)

    private val entries = LinkedHashMap<String, Entry>()

    fun file(title: String, now: Long): String? {
        val entry = entries[title] ?: return null
        if (entry.failed || now - entry.at > OK_TTL_MILLIS) {
            return null
        }
        return entry.file.takeIf { it.isNotEmpty() }
    }

    fun isFailed(title: String, now: Long): Boolean {
        val entry = entries[title] ?: return false
        return entry.failed && now - entry.at <= FAIL_TTL_MILLIS
    }

    fun recordSuccess(title: String, file: String, now: Long) {
        entries[title] = Entry(file, false, now)
    }

    fun recordFailure(title: String, now: Long) {
        entries[title] = Entry("", true, now)
    }

    fun clearFailures() {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.failed) {
                iterator.remove()
            }
        }
    }

    fun load(stored: Stored?, version: Long) {
        entries.clear()
        if (stored == null || stored.version != version) {
            return
        }
        stored.entries.orEmpty().forEach { (title, entry) ->
            if (title.isNotEmpty() && (entry.failed || entry.file.isNotEmpty())) {
                entries[title] = entry
            }
        }
    }

    fun toStored(version: Long): Stored = Stored(version, entries.toMap())

    companion object {
        const val OK_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val FAIL_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}

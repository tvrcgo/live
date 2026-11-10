package com.lizongying.mytv.logo

import android.content.Context

/**
 * 台标库的文件名清单与频道名归一化，只回答"用哪个文件名"。
 */
class LogoCatalog private constructor(files: Set<String>) {

    private val index = files.associateBy { it.lowercase() }
    private val satellite = files.filter { it.contains(SATELLITE) }.sortedByDescending { it.length }

    fun fileFor(title: String): String? {
        for (candidate in candidates(title)) {
            index[candidate.lowercase()]?.let { return it }
        }
        return null
    }

    private fun candidates(title: String): Set<String> {
        val result = LinkedHashSet<String>()
        for (base in listOf(squash(title), squash(stripQuality(BRACKET.replace(title, ""))))) {
            if (base.isEmpty()) {
                continue
            }
            result.add(base)
            ULTRA.find(base)?.let { result.add("CCTV" + it.groupValues[1].uppercase()) }
            CCTV.find(base)?.let { match ->
                val plus = match.groupValues[2]
                result.add("CCTV" + match.groupValues[1] + plus)
                if (plus.isEmpty()) {
                    result.add("CCTV" + match.groupValues[1] + "+")
                }
            }
            if (base.contains(SATELLITE)) {
                satellite.firstOrNull { base.contains(it) }?.let { result.add(it) }
            }
            val head = base.trimEnd { it.isCjk() }
            if (head.isNotEmpty() && head != base) {
                result.add(head)
                result.add(head.replace("-", "").replace("_", ""))
            }
            if (base.contains('-') || base.contains('_')) {
                result.add(base.replace("-", "").replace("_", ""))
            }
        }
        return result
    }

    private fun squash(text: String): String =
        text.replace("－", "-").replace("＋", "+").replace("\u3000", " ").filterNot { it.isWhitespace() }

    private fun stripQuality(text: String): String =
        QUALITY.fold(text) { acc, word -> acc.replace(word, "", ignoreCase = true) }
            .trim(' ', '-', '_', '·', '|')

    private fun Char.isCjk(): Boolean = this in '\u4E00'..'\u9FFF'

    companion object {
        private const val ASSET_NAME = "logo-names.txt"
        private const val SATELLITE = "卫视"

        // 不能包含 4k/8k/3d，否则 CCTV-4K 会被削成错名
        private val QUALITY = listOf(
            "高清", "超清", "蓝光", "标清", "原画", "源码", "备用", "线路",
            "hd", "fhd", "uhd", "1080p", "720p", "ipv6", "ipv4",
        )
        private val BRACKET = Regex("[【\\[（(「『][^】\\]）)」』]*[】\\]）)」』]")
        private val ULTRA = Regex("CCTV[-\\s]?(4K|8K|3D)", RegexOption.IGNORE_CASE)
        private val CCTV = Regex("CCTV[-\\s]?(\\d{1,2})(\\+)?", RegexOption.IGNORE_CASE)

        fun parse(text: String): LogoCatalog =
            LogoCatalog(text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet())

        fun load(context: Context): LogoCatalog =
            parse(context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() })
    }
}

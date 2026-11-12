package com.chacha.live.logo

/**
 * 由频道名推导台标文件名候选。不做存在性判断，命中与否交给请求结果。
 * 央视命名可确定，只给一个；其余按「规则名 → 原样 → 去尾部中文」排两个，供请求时依次兜底。
 */
object LogoName {

    private const val MAX_CANDIDATES = 2

    fun candidates(title: String): List<String> {
        val result = LinkedHashSet<String>()
        val noBracket = squash(BRACKET.replace(title, ""))
        for (base in listOf(stripQuality(noBracket), noBracket).distinct()) {
            if (base.isEmpty() || result.size >= MAX_CANDIDATES) {
                continue
            }
            val cctv = cctvName(base)
            if (cctv != null) {
                result.add(cctv)
                continue
            }
            // 原名优先：库里常带 4K 等变体，先按原名取，取不到再用归一化的卫视名
            result.add(base)
            satelliteName(base)?.let { result.add(it) }
            if (base.contains('-') || base.contains('_')) {
                result.add(base.replace("-", "").replace("_", ""))
            }
        }
        return result.filter { it.isNotEmpty() }.take(MAX_CANDIDATES)
    }

    private fun cctvName(base: String): String? {
        ULTRA.find(base)?.let { return "CCTV" + it.groupValues[1].uppercase() }
        CCTV.find(base)?.let { return "CCTV" + it.groupValues[1] + it.groupValues[2] }
        return null
    }

    private fun satelliteName(base: String): String? {
        if (!base.contains(SATELLITE)) {
            return null
        }
        return SATELLITE_PREFIXES.firstOrNull { base.contains(it + SATELLITE) }?.plus(SATELLITE)
    }

    private fun squash(text: String): String =
        text.replace("－", "-").replace("＋", "+").replace("\u3000", " ").filterNot { it.isWhitespace() }

    private fun stripQuality(text: String): String =
        QUALITY.fold(text) { acc, word -> acc.replace(word, "", ignoreCase = true) }
            .trim(' ', '-', '_', '·', '|')

    private const val SATELLITE = "卫视"

    // 不能包含 4k/8k/3d，否则 CCTV-4K 会被削成错名
    private val QUALITY = listOf(
        "高清", "超清", "蓝光", "标清", "原画", "源码", "备用", "线路",
        "hd", "fhd", "uhd", "1080p", "720p", "ipv6", "ipv4",
    )
    private val BRACKET = Regex("[【\\[（(「『][^】\\]）)」』]*[】\\]）)」』]")
    private val ULTRA = Regex("CCTV[-\\s]?(4K|8K|3D)", RegexOption.IGNORE_CASE)
    private val CCTV = Regex("CCTV[-\\s]?(\\d{1,2})(\\+)?", RegexOption.IGNORE_CASE)

    // 卫视台的省名与特殊叫法，按长度优先匹配，避免「上海东方卫视」被截成「上海卫视」
    private val SATELLITE_PREFIXES = listOf(
        "内蒙古", "黑龙江", "北京", "天津", "上海", "重庆", "河北", "山西", "辽宁", "吉林",
        "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南", "广东",
        "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海", "广西", "西藏", "宁夏",
        "新疆", "深圳", "厦门", "苏州", "青岛", "大连", "宁波", "延边", "三沙",
        "凤凰", "澳亚", "星空", "中华", "东方", "南方",
    ).sortedByDescending { it.length }
}

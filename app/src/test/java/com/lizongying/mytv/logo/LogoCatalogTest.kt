package com.lizongying.mytv.logo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogoCatalogTest {

    private val catalog = LogoCatalog.parse(
        """
        CCTV1
        CCTV3
        CCTV4
        CCTV4K
        CCTV5+
        CCTV6
        CCTV17
        CGTN
        CGTN法语
        湖南卫视
        东方卫视
        浙江卫视
        江苏卫视
        凤凰中文
        睛彩广场舞
        CHC动作电影
        """.trimIndent()
    )

    @Test
    fun normalize() {
        val cases = mapOf(
            "CCTV-1 综合" to "CCTV1",
            "CCTV1" to "CCTV1",
            "cctv6" to "CCTV6",
            "CCTV5+体育赛事" to "CCTV5+",
            "CCTV-4K 超高清" to "CCTV4K",
            "CCTV-17 农业农村" to "CCTV17",
            "湖南卫视高清" to "湖南卫视",
            "上海东方卫视" to "东方卫视",
            "东方卫视HD" to "东方卫视",
            "江苏卫视4K" to "江苏卫视",
            "浙江卫视-蓝光" to "浙江卫视",
            "睛彩广场舞「移动」" to "睛彩广场舞",
            "CGTN法语" to "CGTN法语",
        )
        for ((title, expected) in cases) {
            assertEquals(title, expected, catalog.fileFor(title))
        }
    }

    @Test
    fun missReturnsNull() {
        assertNull(catalog.fileFor("春晚1983"))
        assertNull(catalog.fileFor("央视怀旧剧场"))
        assertNull(catalog.fileFor(""))
    }
}

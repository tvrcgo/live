package com.chacha.live.logo

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoNameTest {

    @Test
    fun cctv() {
        assertEquals(listOf("CCTV1"), LogoName.candidates("CCTV-1 综合"))
        assertEquals(listOf("CCTV1"), LogoName.candidates("cctv1"))
        assertEquals(listOf("CCTV6"), LogoName.candidates("CCTV-6 电影 高清"))
        assertEquals(listOf("CCTV17"), LogoName.candidates("CCTV-17 农业农村"))
        assertEquals(listOf("CCTV5+"), LogoName.candidates("CCTV5+体育赛事"))
        assertEquals(listOf("CCTV4K"), LogoName.candidates("CCTV-4K 超高清"))
    }

    @Test
    fun satellite() {
        assertEquals(listOf("湖南卫视", "湖南卫视高清"), LogoName.candidates("湖南卫视高清"))
        assertEquals(listOf("上海东方卫视", "东方卫视"), LogoName.candidates("上海东方卫视"))
        assertEquals(listOf("江苏卫视4K", "江苏卫视"), LogoName.candidates("江苏卫视4K"))
        assertEquals(listOf("北京卫视4K", "北京卫视"), LogoName.candidates("北京卫视4K"))
        assertEquals(listOf("浙江卫视", "浙江卫视-蓝光"), LogoName.candidates("浙江卫视-蓝光"))
    }

    @Test
    fun alias() {
        assertEquals(listOf("CGTN阿拉伯语", "CGTN阿语"), LogoName.candidates("CGTN阿拉伯语"))
        assertEquals(listOf("CGTN纪录片", "CGTN纪录"), LogoName.candidates("CGTN纪录片"))
        assertEquals(listOf("CGTN记录片", "CGTN记录"), LogoName.candidates("CGTN记录片"))
        assertEquals(listOf("CGTN西语", "CGTN"), LogoName.candidates("CGTN西语"))
    }

    @Test
    fun keepsOriginalNameThenFamily() {
        assertEquals(listOf("CGTN法语", "CGTN"), LogoName.candidates("CGTN法语"))
        assertEquals(listOf("CHC动作电影", "CHC"), LogoName.candidates("CHC动作电影"))
        assertEquals(listOf("CGTN英语", "CGTN"), LogoName.candidates("CGTN英语"))
        assertEquals(listOf("睛彩广场舞"), LogoName.candidates("睛彩广场舞「移动」"))
        assertEquals(listOf("春晚1983"), LogoName.candidates("春晚1983"))
    }

    @Test
    fun emptyTitle() {
        assertEquals(emptyList<String>(), LogoName.candidates(""))
        assertEquals(emptyList<String>(), LogoName.candidates("   "))
    }
}

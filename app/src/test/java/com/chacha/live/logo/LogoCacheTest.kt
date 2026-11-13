package com.chacha.live.logo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoCacheTest {

    private val day = 24L * 60 * 60 * 1000

    @Test
    fun successExpiresInThirtyDays() {
        val cache = LogoCache()
        cache.recordSuccess("湖南卫视", "湖南卫视", 0L)

        assertEquals("湖南卫视", cache.file("湖南卫视", 29 * day))
        assertNull(cache.file("湖南卫视", 31 * day))
    }

    @Test
    fun failureBlocksRetryForSevenDays() {
        val cache = LogoCache()
        cache.recordFailure("春晚1983", 0L)

        assertTrue(cache.isFailed("春晚1983", 6 * day))
        assertNull(cache.file("春晚1983", 6 * day))
        assertFalse(cache.isFailed("春晚1983", 8 * day))
    }

    @Test
    fun successReplacesFailure() {
        val cache = LogoCache()
        cache.recordFailure("CGTN英语", 0L)
        cache.recordSuccess("CGTN英语", "CGTN", day)

        assertFalse(cache.isFailed("CGTN英语", day))
        assertEquals("CGTN", cache.file("CGTN英语", day))
    }

    @Test
    fun clearFailuresKeepsSuccesses() {
        val cache = LogoCache()
        cache.recordFailure("春晚1983", 0L)
        cache.recordSuccess("CCTV1", "CCTV1", 0L)

        cache.clearFailures()

        assertFalse(cache.isFailed("春晚1983", 0L))
        assertEquals("CCTV1", cache.file("CCTV1", 0L))
    }

    @Test
    fun versionChangeDropsEverything() {
        val cache = LogoCache()
        cache.load(LogoCache.Stored(7L, mapOf("CCTV1" to LogoCache.Entry("CCTV1", false, 0L))), 7L)
        assertEquals("CCTV1", cache.file("CCTV1", 0L))

        cache.load(LogoCache.Stored(7L, mapOf("CCTV1" to LogoCache.Entry("CCTV1", false, 0L))), 8L)
        assertNull(cache.file("CCTV1", 0L))
    }
}

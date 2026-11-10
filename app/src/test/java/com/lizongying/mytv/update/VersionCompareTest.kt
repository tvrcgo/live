package com.lizongying.mytv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test
    fun `same version with v prefix equals`() {
        assertEquals(0, VersionCompare.compare("1.2611.1", "v1.2611.1"))
    }

    @Test
    fun `bigger build id is newer`() {
        assertTrue(VersionCompare.compare("1.2611.1", "1.2611.2") > 0)
    }

    @Test
    fun `bigger yymm is newer even with smaller build id`() {
        assertTrue(VersionCompare.compare("1.2611.9", "1.2612.1") > 0)
    }

    @Test
    fun `bigger major is newer`() {
        assertTrue(VersionCompare.compare("1.2612.9", "2.2601.1") > 0)
    }

    @Test
    fun `older latest is negative`() {
        assertTrue(VersionCompare.compare("1.2611.2", "1.2611.1") < 0)
    }

    @Test
    fun `non numeric segment treated as zero`() {
        assertEquals(0, VersionCompare.compare("1.2611.x", "1.2611.0"))
    }

    @Test
    fun `missing segment treated as zero`() {
        assertTrue(VersionCompare.compare("1.2611", "1.2611.1") > 0)
    }
}

package com.chacha.live.models

import com.chacha.live.TV
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelSelectionTest {

    private fun tv(id: Int, title: String): TV =
        TV(id = id, title = title, videoUrl = listOf("http://example.com/$id"))

    private val groups = listOf(
        ChannelGroup("央视", listOf(tv(0, "CCTV-1"), tv(1, "CCTV-2"))),
        ChannelGroup("卫视", listOf(tv(2, "湖南卫视"), tv(3, "浙江卫视"))),
    )

    @Test
    fun initialGroupIndex_found() {
        assertEquals(1, ChannelSelection.initialGroupIndex(groups, 3))
    }

    @Test
    fun initialGroupIndex_missing_fallsBackToFirst() {
        assertEquals(0, ChannelSelection.initialGroupIndex(groups, 99))
    }

    @Test
    fun channelIndexForGroup_found() {
        assertEquals(1, ChannelSelection.channelIndexForGroup(groups[1], 3))
    }

    @Test
    fun channelIndexForGroup_missing_fallsBackToFirst() {
        assertEquals(0, ChannelSelection.channelIndexForGroup(groups[1], 0))
    }
}

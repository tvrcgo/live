package com.chacha.live.models

import com.chacha.live.TV
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelItemTest {

    private fun item(vararg urls: String): ChannelItem =
        ChannelItem(TV(id = 0, title = "CCTV-1", videoUrl = urls.toList()))

    @Test
    fun currentUrl_singleUrl() {
        assertEquals("http://example.com/1", item("http://example.com/1").currentUrl())
    }

    @Test
    fun currentUrl_multipleUrls_takesLast() {
        assertEquals("http://example.com/2", item("http://example.com/1", "http://example.com/2").currentUrl())
    }

    @Test
    fun currentUrl_emptyUrls_returnsEmpty() {
        assertEquals("", item().currentUrl())
    }
}

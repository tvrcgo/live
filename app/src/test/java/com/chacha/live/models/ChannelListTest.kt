package com.chacha.live.models

import com.chacha.live.TV
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ChannelListTest {

    private fun item(id: Int): ChannelItem =
        ChannelItem(TV(id = id, title = "频道$id", videoUrl = listOf("http://example.com/$id")))

    private fun listOf(size: Int): ChannelList {
        val list = ChannelList()
        repeat(size) { list.add(item(it)) }
        return list
    }

    @Test
    fun get_inBounds_returnsItem() {
        val list = listOf(3)
        assertSame(list.get(1), list.get(1))
        assertEquals(1, list.get(1)?.getTV()?.id)
    }

    @Test
    fun get_outOfBounds_returnsNull() {
        val list = listOf(2)
        assertNull(list.get(-1))
        assertNull(list.get(2))
    }

    @Test
    fun get_emptyList_returnsNull() {
        assertNull(ChannelList().get(0))
        assertEquals(0, ChannelList().size())
    }

    @Test
    fun getCurrent_followsSetCurrent() {
        val list = listOf(3)
        list.setCurrent(2)
        assertEquals(2, list.getCurrent()?.getTV()?.id)
    }
}

package com.chacha.live.models

class ChannelList {

    private val items = mutableListOf<ChannelItem>()

    private var itemPositionCurrent = 0

    fun add(channelItem: ChannelItem) {
        items.add(channelItem)
    }

    fun get(id: Int): ChannelItem? {
        return if (id in items.indices) items[id] else null
    }

    fun getCurrent(): ChannelItem? {
        return get(itemPositionCurrent)
    }

    fun setCurrent(position: Int) {
        itemPositionCurrent = position
    }

    fun size(): Int {
        return items.size
    }
}

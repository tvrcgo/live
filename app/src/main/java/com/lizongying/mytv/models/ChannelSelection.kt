package com.lizongying.mytv.models

object ChannelSelection {

    fun initialGroupIndex(groups: List<ChannelGroup>, currentId: Int): Int {
        val index = groups.indexOfFirst { group -> group.channels.any { it.id == currentId } }
        return if (index >= 0) index else 0
    }

    fun channelIndexForGroup(group: ChannelGroup, currentId: Int): Int {
        val index = group.channels.indexOfFirst { it.id == currentId }
        return if (index >= 0) index else 0
    }
}

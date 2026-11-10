package com.lizongying.mytv.models

import com.lizongying.mytv.TV

data class ChannelGroup(
    val title: String,
    val channels: List<TV>,
)

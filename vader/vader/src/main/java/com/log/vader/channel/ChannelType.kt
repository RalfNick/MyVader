package com.log.vader.channel

enum class ChannelType constructor(val type: Int) {
    REAL_TIME(0),
    HIGH_FREQ(1),
    NORMAL(2)
}
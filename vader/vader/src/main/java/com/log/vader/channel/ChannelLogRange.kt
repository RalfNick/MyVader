package com.log.vader.channel

data class ChannelLogRange(val channelType: ChannelType, val lower: Long, val upper: Long) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChannelLogRange

        if (channelType != other.channelType) return false
        if (lower != other.lower) return false
        if (upper != other.upper) return false

        return true
    }

    override fun hashCode(): Int {
        var result = channelType.hashCode()
        result = 31 * result + lower.hashCode()
        result = 31 * result + upper.hashCode()
        return result
    }
}

package com.log.vader.database.data

import androidx.room.TypeConverter
import com.log.vader.channel.ChannelType

object ChannelConverter {

    @TypeConverter
    @JvmStatic
    fun convertToChannel(type: Int): ChannelType {
        return when (type) {
            ChannelType.NORMAL.type -> ChannelType.NORMAL
            ChannelType.HIGH_FREQ.type -> ChannelType.HIGH_FREQ
            ChannelType.REAL_TIME.type -> ChannelType.REAL_TIME
            else -> throw IllegalArgumentException("Unknown channel status: $type")
        }
    }

    @TypeConverter
    @JvmStatic
    fun convertToType(channel: ChannelType) = channel.type
}
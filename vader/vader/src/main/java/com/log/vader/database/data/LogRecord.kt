package com.log.vader.database.data

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.log.vader.channel.ChannelType

@Keep
@Entity
data class LogRecord(
    @PrimaryKey val seqId: Long,
    @ColumnInfo val channelSeqId: Long,
    @ColumnInfo
    val channelType: ChannelType,
    @ColumnInfo val customType: String,
    @ColumnInfo val customSeqId: Long,
    @ColumnInfo val clientTimestamp: Long,
    @ColumnInfo val payload: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LogRecord

        if (seqId != other.seqId) return false
        if (channelSeqId != other.channelSeqId) return false
        if (channelType != other.channelType) return false
        if (customType != other.customType) return false
        if (customSeqId != other.customSeqId) return false
        if (clientTimestamp != other.clientTimestamp) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = seqId
        result = 31 * result + channelSeqId
        result = 31 * result + channelType.hashCode()
        result = 31 * result + customType.hashCode()
        result = 31 * result + customSeqId
        result = 31 * result + clientTimestamp.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result.toInt()
    }
}

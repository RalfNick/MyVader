package com.log.vader.assembler

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.protobuf.nano.MessageNano
import com.log.vader.channel.ChannelType
import com.log.vader.database.data.LogRecord
import com.log.vader.exception.DBException
import com.log.vader.logger.LoggerExt
import com.log.vader.seqid.SeqIdWrapper

object LogDataConverter {

    /**
     * 目前只支持 ProtocolBuf 和 json
     */
    fun <T> convert(
        data: T,
        channel: ChannelType,
        customType: String,
        idWrapper: SeqIdWrapper
    ): LogRecord? {
        if (data is MessageNano) {
            return convertToLogRecord(
                MessageNano.toByteArray(data),
                channel,
                customType,
                idWrapper
            )
        } else if (data is String) {
            val element: JsonElement = JsonParser.parseString(data)
            if (!element.isJsonObject) {
                throw DBException("convert", "convert log data error")
            }
            return convertToLogRecord(
                data.toByteArray(),
                channel,
                customType,
                idWrapper
            )
        }
        LoggerExt.event("convert", "convert LogRecord error,data is $data")
        return null
    }

    private fun convertToLogRecord(
        content: ByteArray,
        channel: ChannelType,
        customType: String,
        idWrapper: SeqIdWrapper
    ): LogRecord {
        return LogRecord(
            idWrapper.seqId,
            idWrapper.channelSeqId,
            channel,
            customType,
            idWrapper.customSeqId,
            System.currentTimeMillis(),
            content
        )
    }
}
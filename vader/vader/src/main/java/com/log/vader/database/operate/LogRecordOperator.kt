package com.log.vader.database.operate

import com.log.vader.channel.ChannelType
import com.log.vader.database.data.LogRecord

interface LogRecordOperator {

    fun getLogCount(): Long

    fun getMaxLogSeqId(): Long

    fun getMinLogSeqId(): Long

    fun getOldestLogTimestamp(): Long

    fun getAll(): List<LogRecord>

    fun getLogs(count: Int): List<LogRecord>

    // get channel logs in range [lb, ub)
    fun getChannelLogsBetween(c: ChannelType, lb: Long, ub: Long, maxCount: Long): List<LogRecord>

    fun addLogs(logs: List<LogRecord>)

    fun addLog(log: LogRecord)

    fun deleteLogs(logs: List<LogRecord>)

    fun deleteLog(log: LogRecord)

    fun evictOutdatedLog(lowerBound: Long): Int

    fun getMaxSeqId(): Long

    fun getMaxChannelSeqId(channel: ChannelType): Long

    fun getMaxCustomTypeSeqId(customType: String): Long

    fun clear()

    fun close()
}
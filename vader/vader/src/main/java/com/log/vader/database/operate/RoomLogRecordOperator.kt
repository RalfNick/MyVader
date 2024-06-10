package com.log.vader.database.operate

import com.log.vader.channel.ChannelType
import com.log.vader.database.data.LogRecord
import com.log.vader.database.room.LogRecordDatabase

/**
 * RoomDatabase 数据库操作
 */
class RoomLogRecordOperator constructor(private val database: LogRecordDatabase) :
    LogRecordOperator {

    override fun getLogCount(): Long {
        return database.logRecordDao().getLogCount()
    }

    override fun getMaxLogSeqId(): Long {
        return database.logRecordDao().getMaxLogSeqId()
    }

    override fun getMinLogSeqId(): Long {
        return database.logRecordDao().getMinLogSeqId()
    }

    override fun getOldestLogTimestamp(): Long {
        return database.logRecordDao().getOldestLogTimestamp()
    }

    override fun getAll(): List<LogRecord> {
        return database.logRecordDao().getAll() ?: emptyList()
    }

    override fun getLogs(count: Int): List<LogRecord> {
        return database.logRecordDao().getLogs(count) ?: emptyList()
    }

    override fun getChannelLogsBetween(
        c: ChannelType,
        lb: Long,
        ub: Long,
        maxCount: Long
    ): List<LogRecord> {
        return database.logRecordDao().getChannelLogsBetween(c, lb, ub, maxCount) ?: emptyList()
    }

    override fun addLogs(logs: List<LogRecord>) {
        database.logRecordDao().addLogs(logs)
    }

    override fun addLog(log: LogRecord) {
        database.logRecordDao().addLog(log)
    }

    override fun deleteLogs(logs: List<LogRecord>) {
        database.logRecordDao().deleteLogs(logs)
    }

    override fun deleteLog(log: LogRecord) {
        database.logRecordDao().deleteLog(log)
    }

    override fun evictOutdatedLog(lowerBound: Long): Int {
        return database.logRecordDao().evictOutdatedLog(lowerBound)
    }

    override fun getMaxSeqId(): Long {
        return database.logRecordDao().getMaxSeqId()
    }

    override fun getMaxChannelSeqId(channel: ChannelType): Long {
        return database.logRecordDao().getMaxChannelSeqId(channel)
    }

    override fun getMaxCustomTypeSeqId(customType: String): Long {
        return database.logRecordDao().getMaxCustomTypeSeqId(customType)
    }

    override fun clear() {
        return database.logRecordDao().clearTable()
    }

    override fun close() {
        return database.close()
    }
}
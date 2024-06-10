package com.log.vader.database.room

import androidx.room.*
import com.log.vader.channel.ChannelType
import com.log.vader.database.data.ChannelConverter
import com.log.vader.database.data.LogRecord

@Dao
interface LogRecordDao {

    @Query("SELECT count(*) from LogRecord")
    fun getLogCount(): Long

    @Query("SELECT max(seqId) from LogRecord")
    fun getMaxLogSeqId(): Long

    @Query("SELECT min(seqId) from LogRecord")
    fun getMinLogSeqId(): Long

    @Query("SELECT min(clientTimestamp) from LogRecord")
    fun getOldestLogTimestamp(): Long

    @Query("SELECT * FROM LogRecord")
    fun getAll(): List<LogRecord>?

    @Query("SELECT * FROM LogRecord LIMIT :count")
    fun getLogs(count: Int): List<LogRecord>?

    // get channel logs in range [lb, ub)
    @TypeConverters(ChannelConverter::class)
    @Query("SELECT * FROM LogRecord WHERE channelType = :c AND channelSeqId >= :lb AND channelSeqId < :ub LIMIT :maxCount")
    fun getChannelLogsBetween(c: ChannelType, lb: Long, ub: Long, maxCount: Long): List<LogRecord>?

    @Insert
    fun addLogs(logs: List<LogRecord>)

    @Insert
    fun addLog(logs: LogRecord)

    @Delete
    fun deleteLogs(logs: List<LogRecord>)

    @Delete
    fun deleteLog(logs: LogRecord)

    @Query("DELETE FROM LogRecord WHERE clientTimestamp <= :lowerBound")
    fun evictOutdatedLog(lowerBound: Long): Int

    @Query("SELECT max(seqId) FROM LogRecord")
    fun getMaxSeqId(): Long

    @TypeConverters(ChannelConverter::class)
    @Query("SELECT max(channelSeqId) FROM LogRecord WHERE channelType = :channel")
    fun getMaxChannelSeqId(channel: ChannelType): Long

    @Query("SELECT max(customType) FROM LogRecord WHERE customType = :customType")
    fun getMaxCustomTypeSeqId(customType: String): Long

    @Query("DELETE FROM LogRecord")
    fun clearTable()
}
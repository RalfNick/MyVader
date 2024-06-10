package com.log.vader.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.log.vader.database.data.ChannelConverter
import com.log.vader.database.data.LogRecord

@Database(entities = [LogRecord::class], version = 1)
@TypeConverters(value = [ChannelConverter::class])
abstract class LogRecordDatabase : RoomDatabase() {

    abstract fun logRecordDao(): LogRecordDao

}
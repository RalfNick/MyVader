package com.log.vader.database.operate

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import androidx.room.RoomDatabase
import com.log.vader.database.room.LogRecordDatabase
import com.log.vader.logger.LoggerExt
import java.io.File

class RoomLogRecordOperatorFactory : LogRecordOperatorFactory {

    companion object {
        // 1MB
        private const val LOWER_STORAGE_THRESHOLD = (1 shl 20).toLong()
    }

    override fun createLogRecordOperator(
        context: Context,
        databaseName: String
    ): LogRecordOperator {
        return RoomLogRecordOperator(getDatabase(context, databaseName))
    }

    @VisibleForTesting
    fun createMemoryDatabaseOperator(context: Context): LogRecordOperator {
        return RoomLogRecordOperator(
            Room.inMemoryDatabaseBuilder(
                context,
                LogRecordDatabase::class.java
            ).build()
        )
    }

    private fun getDatabase(context: Context, databaseName: String): LogRecordDatabase {
        val mode: RoomDatabase.JournalMode = getSuggestedMode(context, databaseName)
        LoggerExt.event("use_mode", "mode: $mode")
        return Room.databaseBuilder(context, LogRecordDatabase::class.java, databaseName)
            .setJournalMode(mode)
            .build()
    }

    private fun getSuggestedMode(context: Context, databaseName: String): RoomDatabase.JournalMode {
        var mode = RoomDatabase.JournalMode.AUTOMATIC

        // 如果在Android 4.4及以下，并且内部存储空间完全耗尽情况下，不使用WAL模式。因为SQLite的一个bug在手机上
        // 调用mmap抛出SIGBUS(BUS_ADRERR)可能导致app crash，这个错误也无法在Java层catch
        // Source: https://android.googlesource.com/platform/external/sqlite/+/1eb051d%5E!/。
        // 这里直接使用Truncate模式绕开这个问题。如果要彻底解决，最好自带最新的sqlite3，但是会增加额外的包大小。
        // 所以直接采用了改动最小的办法。
        if (shouldHackFix()) {
            mode = RoomDatabase.JournalMode.TRUNCATE
            // 如果hack fix时候使用truncate模式还要删除之前的shm, wal文件(会丢数据),
            // 避免sqlite在转换数据库类型时候依然使用mmap引发崩溃。
            val parentPath = context.getDatabasePath(databaseName).absolutePath
            val shmFile = File("$parentPath-shm")
            val walFile = File("$parentPath-wal")
            val shmOk = !shmFile.exists() || shmFile.exists() && shmFile.delete()
            val walOk = !walFile.exists() || walFile.exists() && walFile.delete()
            LoggerExt.event("force_delete_wal_files", "shm: $shmOk, wal: $walOk")
        }
        return mode
    }

    private fun shouldHackFix(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            getDirectoryAvailableSpace(Environment.getDataDirectory()) < LOWER_STORAGE_THRESHOLD
        } else false
    }

    @Suppress("DEPRECATION")
    private fun getDirectoryAvailableSpace(dir: File): Long {
        val stat = StatFs(dir.path)
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.blockSize.toLong() * stat.availableBlocks.toLong()
        } else {
            stat.blockSizeLong * stat.availableBlocksLong
        }
    }

}
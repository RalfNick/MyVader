package com.log.vader.database.operate

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.log.vader.database.data.DatabaseType
import com.log.vader.exception.DBException

object LogRecordOperatorExt {

    @JvmStatic
    fun createDatabase(
        context: Context,
        databaseType: DatabaseType,
        databaseName: String
    ): LogRecordOperator {
        if (databaseType == DatabaseType.ROOM) {
            return RoomLogRecordOperatorFactory().createLogRecordOperator(context, databaseName)
        } else {
            throw DBException("create", "unkown data base type")
        }
    }

    @VisibleForTesting
    @JvmStatic
    fun createMemoryDatabase(context: Context): LogRecordOperator {
        return RoomLogRecordOperatorFactory().createMemoryDatabaseOperator(context)
    }
}

interface LogRecordOperatorFactory {
    fun createLogRecordOperator(context: Context, databaseName: String): LogRecordOperator
}
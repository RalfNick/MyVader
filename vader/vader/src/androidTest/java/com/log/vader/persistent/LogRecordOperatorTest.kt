package com.log.vader.persistent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.log.vader.channel.ChannelType
import com.log.vader.database.data.DatabaseType
import com.log.vader.database.data.LogRecord
import com.log.vader.database.operate.LogRecordOperator
import com.log.vader.database.operate.LogRecordOperatorExt
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LogRecordOperatorTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }

    private val operator: LogRecordOperator by lazy {
        LogRecordOperatorExt.createDatabase(
            context,
            DatabaseType.ROOM,
            "vader-client-log"
        )
    }

    @Before
    fun createDB() {
        println("create DB")
        operator.clear()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        operator.close()
    }

    @Test
    @Throws(Exception::class)
    fun evictOutdatedLog() {
        val logs: MutableList<LogRecord> = ArrayList<LogRecord>()
        for (i in 0..99) {
            val currentTimestamp = System.currentTimeMillis() - 1000
            val log = LogRecord(
                i.toLong(), 1, ChannelType.NORMAL, "clickEvent", 1,
                currentTimestamp - TimeUnit.DAYS.toMillis(i.toLong()), byteArrayOf(0)
            )
            logs.add(log)
        }
        println("duck " + System.currentTimeMillis())
        operator.addLogs(logs)
        var logList = operator.getAll()
        Assert.assertEquals(100, logList.size.toLong())
        operator.evictOutdatedLog(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3))
        logList = operator.getAll()
        Assert.assertEquals(3, logList.size.toLong())
    }

}
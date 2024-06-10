package com.log.vader.persistent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.log.vader.channel.ChannelType
import com.log.vader.database.data.DatabaseType
import com.log.vader.database.data.DbAction
import com.log.vader.database.data.DbActionType
import com.log.vader.database.data.LogRecord
import com.log.vader.database.operate.LogRecordManager
import com.log.vader.database.operate.LogRecordOperator
import com.log.vader.database.operate.LogRecordOperatorExt
import com.log.vader.seqid.SeqIdWrapper
import com.log.vader.seqid.SequenceIdGenerator
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.ArrayList

@RunWith(AndroidJUnit4::class)
class LogRecordManagerTest {

    companion object {
        private const val N = 500L // N logs
        private const val T = 20L // test round
    }

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val operator: LogRecordOperator by lazy {
        getLogRecordOperator(context)
    }

    private val logRecordManager = LogRecordManager.getInstance().apply {
        setLogOperator(operator)
    }

    private lateinit var generator: SequenceIdGenerator

    private fun getLogRecordOperator(context: Context): LogRecordOperator {
        return LogRecordOperatorExt.createMemoryDatabase(context)
    }

    @Before
    @Throws(InterruptedException::class)
    fun createDB() {
        generator = SequenceIdGenerator.get(context, operator)
        generator.deleteAll()
        logRecordManager.clearAll()
        Thread.sleep(1000)
    }

    @Test
    @Throws(Exception::class)
    fun persistReadWrite() {
        val ids: MutableSet<Long> = HashSet()
        val s = StringBuilder()
        for (i in 0..999) {
            s.append('a')
        }
        val fakeContent = s.toString().toByteArray()
        for (i in 1..T * N) {
            val id: SeqIdWrapper = generator.generateNextSeqId(ChannelType.NORMAL, "showEvent")
            val log = LogRecord(
                id.seqId,
                id.channelSeqId,
                ChannelType.NORMAL,
                "showEvent",
                id.customSeqId,
                System.currentTimeMillis(),
                fakeContent
            )
            logRecordManager.enqueueDbAction(DbAction(log, DbActionType.ADD))
            if (i % N == 0L) {
                val future: Future<List<LogRecord>> = logRecordManager.getChannelLogs(
                    ChannelType.NORMAL, 0, Int.MAX_VALUE.toLong(), N
                )
                val logs: List<LogRecord> = future.get()
                Assert.assertEquals(N, logs.size.toLong())
                val oldSize = ids.size
                for (l in logs) {
                    ids.add(l.seqId)
                }
                val delta = ids.size - oldSize
                Assert.assertEquals(N, delta.toLong())
                logRecordManager.enqueueDbAction(DbAction(ArrayList(logs), DbActionType.DELETE))
            }
        }
        val remainingLogs: Int = logRecordManager.getChannelLogs(
            ChannelType.NORMAL, 0, Int.MAX_VALUE.toLong(), N
        ).get().size
        Assert.assertEquals(0, remainingLogs.toLong())
    }

}
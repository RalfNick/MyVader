package com.log.vader.assembler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuaishou.client.log.event.packages.nano.ClientEvent
import com.kuaishou.client.log.packages.nano.ClientLog
import com.log.vader.DataType
import com.log.vader.DatabaseConfig
import com.log.vader.VaderConfig
import com.log.vader.channel.ChannelType
import com.log.vader.channel.LogChannel
import com.log.vader.channel.LogChannelConfig
import com.log.vader.channel.NormalLogChannel
import com.log.vader.database.data.DatabaseType
import com.log.vader.database.operate.LogRecordManager
import com.log.vader.database.operate.LogRecordOperatorExt
import com.log.vader.executor.ExecutorExt
import com.log.vader.logger.StdoutLogger
import com.log.vader.seqid.SequenceIdGenerator
import com.log.vader.upload.FixedRequestCostUploader
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class AssemblerAndRealtimeLogTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val realtimeUploader = FixedRequestCostUploader(100, 0)
    private val highFreqUploader = FixedRequestCostUploader(100, 5000)
    private val normalUploader = FixedRequestCostUploader(100, 10000)
    private val channel = NormalLogChannel(
        LogChannelConfig(
            context,
            ChannelType.REAL_TIME,
            realtimeUploader,
            ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
            100L
        )
    )
    private var assembler: Assembler? = null

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun createDB() {
        val uploaderMap = mutableMapOf<ChannelType, LogChannel>().apply {
            this[ChannelType.REAL_TIME] = channel
        }
        val config = VaderConfig(
            DatabaseConfig("AssemblerAndRealtimeLogTest", DatabaseType.ROOM),
            uploaderMap,
            StdoutLogger(),
            DataType.FORMAT_PROTOCOL_BUF
        )
        val operator = LogRecordOperatorExt.createDatabase(
            context,
            config.databaseConfig.databaseType,
            config.databaseConfig.databaseName
        )
        val logRecordManager = LogRecordManager.getInstance().apply {
            setLogOperator(operator)
        }
        val generator = SequenceIdGenerator.get(context, operator)
        assembler = Assembler.getInstance(
            logRecordManager, generator, config.channels,
            "", DataType.FORMAT_PROTOCOL_BUF
        )
        logRecordManager.clearAll()
        Thread.sleep(1000)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun burstOfRealtimeLog() {
        val t: Thread = object : Thread() {
            override fun run() {
                super.run()
                for (j in 0..3999) {
                    val c: ChannelType =
                        if (j % 2 == 0) ChannelType.HIGH_FREQ else ChannelType.REAL_TIME
                    val customType = (j % 10).toString()
                    assembler!!.addLog(ClientLog.ReportEvent(), c, customType)
                }
            }
        }
        t.start()
        t.join()
        Thread.sleep((30 * 1000).toLong())
        Assert.assertEquals(
            4000, realtimeUploader.getAccumulativeLogCount()
                    + highFreqUploader.getAccumulativeLogCount()
        )
        val remainingLogs: Int = LogRecordManager.getInstance().getChannelLogs(
            ChannelType.NORMAL, 0, Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong()
        ).get().size
        Assert.assertEquals(0, remainingLogs.toLong())
    }

    fun buildMockLaunchEvent(): ClientLog.ReportEvent? {
        val event = ClientLog.ReportEvent()
        event.eventPackage = ClientEvent.EventPackage()
        event.eventPackage.launchEvent = ClientEvent.LaunchEvent()
        event.eventPackage.launchEvent.cold = true
        event.eventPackage.launchEvent.timeCost = 3000
        return event
    }

}
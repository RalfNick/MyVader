package com.log.vader.assembler

import android.content.Context
import android.util.Log
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
import com.log.vader.upload.AllSuccessUploader
import com.log.vader.upload.FixedRequestCostUploader
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class AssemblerAndHighFreqLogTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val realtimeUploader = AllSuccessUploader(0)
    private val highFreqUploader = FixedRequestCostUploader(0, 5000)
    private val normalUploader = FixedRequestCostUploader(1000, 10000)
    private val realtimeChannel = NormalLogChannel(
        LogChannelConfig(
            context,
            ChannelType.REAL_TIME,
            realtimeUploader,
            ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
            100L
        )
    )
    private val highFreqChannel = NormalLogChannel(
        LogChannelConfig(
            context,
            ChannelType.REAL_TIME,
            highFreqUploader,
            ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
            100L
        )
    )

    private val normalChannel = NormalLogChannel(
        LogChannelConfig(
            context,
            ChannelType.REAL_TIME,
            normalUploader,
            ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
            100L
        )
    )
    private var assembler: Assembler? = null

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun createDB() {

        val uploaderMap = mutableMapOf<ChannelType, LogChannel>().apply {
            this[ChannelType.REAL_TIME] = realtimeChannel
            this[ChannelType.HIGH_FREQ] = highFreqChannel
            this[ChannelType.NORMAL] = normalChannel
        }
        val config = VaderConfig(
            DatabaseConfig("AssemblerAndHighFreqLogTest", DatabaseType.ROOM),
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
        generator.deleteAll()
        logRecordManager.clearAll()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun burstOfRealtimeLog() {
        val t = Thread {
            for (j in 0..1999) {
                assembler!!.addLog(
                    ClientLog.ReportEvent(),
                    ChannelType.HIGH_FREQ,
                    "launchEvent"
                )
            }
        }
        t.start()
        for (i in 0..2) {
            Thread.sleep(1)
            assembler!!.uploadLatestLogImmediately()
            val stat = assembler!!.getVaderState()
            Log.d("duck", "stat: $stat")
        }
        t.join()
        Thread.sleep((20 * 1000).toLong())
        Assert.assertEquals(
            2000, realtimeUploader.getAccumulativeLogCount()
                    + highFreqUploader.getAccumulativeLogCount()
        )
        val remainingLogs: Int = LogRecordManager.getInstance().getChannelLogs(
            ChannelType.NORMAL, 0, Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong()
        ).get().size
        Assert.assertEquals(0, remainingLogs.toLong())
    }

    private fun buildMockLaunchEvent(): ClientLog.ReportEvent {
        val event = ClientLog.ReportEvent()
        event.eventPackage = ClientEvent.EventPackage()
        event.eventPackage.launchEvent = ClientEvent.LaunchEvent()
        event.eventPackage.launchEvent.cold = true
        event.eventPackage.launchEvent.timeCost = 3000
        return event
    }

}
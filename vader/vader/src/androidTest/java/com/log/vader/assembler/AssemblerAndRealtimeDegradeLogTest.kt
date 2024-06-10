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
import com.log.vader.upload.AlwaysDegradeUploader
import com.log.vader.upload.FixedRequestCostUploader
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class AssemblerAndRealtimeDegradeLogTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val realtimeUploader = FixedRequestCostUploader(100, 0)
    private val highFreqUploader = FixedRequestCostUploader(100, 5000)
    private val normalUploader = FixedRequestCostUploader(100, 10000)
    private val alwaysDegradeUploader = AlwaysDegradeUploader(100)

    private val realtimeChannel = NormalLogChannel(
        LogChannelConfig(
            context,
            ChannelType.REAL_TIME,
            alwaysDegradeUploader,
            ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
            100L
        )
    )
    private val highFreqChannel = NormalLogChannel(
        LogChannelConfig(
            context,
            ChannelType.REAL_TIME,
            alwaysDegradeUploader,
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
    private val uploaderMap = mutableMapOf<ChannelType, LogChannel>().apply {
        this[ChannelType.REAL_TIME] = realtimeChannel
        this[ChannelType.HIGH_FREQ] = highFreqChannel
        this[ChannelType.NORMAL] = normalChannel
    }
    private val config = VaderConfig(
        DatabaseConfig("AssemblerAndRealtimeDegradeLogTest", DatabaseType.ROOM),
        uploaderMap,
        StdoutLogger(),
        DataType.FORMAT_PROTOCOL_BUF
    )
    val operator = LogRecordOperatorExt.createDatabase(
        context,
        config.databaseConfig.databaseType,
        config.databaseConfig.databaseName
    )
    private val logRecordManager = LogRecordManager.getInstance().apply {
        setLogOperator(operator)
    }
    private val generator = SequenceIdGenerator.get(context, operator)
    private var assembler: Assembler? = null

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun createDB() {
        assembler = Assembler.getInstance(
            logRecordManager, generator, config.channels,
            "", DataType.FORMAT_PROTOCOL_BUF
        )
        generator.deleteAll()
        logRecordManager.clearAll()
        Thread.sleep(1000)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun logDegradeAndRetransmit() {
        val t = Thread {
            for (j in 0..3999) {
                val c: ChannelType =
                    if (j % 2 == 0) ChannelType.HIGH_FREQ else ChannelType.REAL_TIME
                val customType = (j % 10).toString()
                assembler!!.addLog(
                    ClientLog.ReportEvent(),
                    c,
                    customType
                )
            }
        }
        t.start()
        t.join()
        Thread.sleep((10 * 1000).toLong())
        Assert.assertNotEquals(4000, alwaysDegradeUploader.getAccumulativeLogCount())

        // 重启之后
        assembler = Assembler.getInstanceTest(
            logRecordManager,
            generator,
            getChannelMap(),
            "",
            DataType.FORMAT_PROTOCOL_BUF

        )
        for (j in 0..3999) {
            val c: ChannelType = if (j % 2 == 0) ChannelType.HIGH_FREQ else ChannelType.REAL_TIME
            val customType = (j % 10).toString()
            if (j <= 10) {
                println("MaxChannel.  add log")
            }
            assembler!!.addLog(ClientLog.ReportEvent(), c, customType)
        }
        Thread.sleep((25 * 1000).toLong())
        Assert.assertEquals(
            8000,
            alwaysDegradeUploader.getAccumulativeLogCount()
                    + realtimeUploader.getAccumulativeLogCount()
                    + highFreqUploader.getAccumulativeLogCount()
        )
        val remainingLogs: Int = logRecordManager.getChannelLogs(
            ChannelType.NORMAL, 0, Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong()
        ).get().size
        Assert.assertEquals(0, remainingLogs.toLong())
    }

    fun buildMockLaunchEvent(): ClientLog.ReportEvent {
        val event = ClientLog.ReportEvent()
        event.eventPackage = ClientEvent.EventPackage()
        event.eventPackage.launchEvent = ClientEvent.LaunchEvent()
        event.eventPackage.launchEvent.cold = true
        event.eventPackage.launchEvent.timeCost = 3000
        return event
    }

    private fun getChannelMap(): Map<ChannelType, LogChannel> {
        val realtimeChannel = NormalLogChannel(
            LogChannelConfig(
                context,
                ChannelType.REAL_TIME,
                realtimeUploader,
                ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
                100L
            )
        )
        val highFreqChannel = NormalLogChannel(
            LogChannelConfig(
                context,
                ChannelType.HIGH_FREQ,
                highFreqUploader,
                ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
                100L
            )
        )

        val normalChannel = NormalLogChannel(
            LogChannelConfig(
                context,
                ChannelType.REAL_TIME,
                normalUploader,
                ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
                100L
            )
        )
        return mutableMapOf<ChannelType, LogChannel>().apply {
            this[ChannelType.REAL_TIME] = realtimeChannel
            this[ChannelType.HIGH_FREQ] = highFreqChannel
            this[ChannelType.NORMAL] = normalChannel
        }
    }

}
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
class AssemblerTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val realtimeUploader = FixedRequestCostUploader(100, 10)
    private val highFreqUploader = FixedRequestCostUploader(100, 50)
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
            DatabaseConfig("AssemblerTest", DatabaseType.ROOM),
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
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun burstOfLargeLogs() {
        val event = buildMockLaunchEvent()
        val t: Thread = object : Thread() {
            override fun run() {
                for (j in 0..19) {
                    val c: ChannelType =
                        if (j % 2 == 0) ChannelType.HIGH_FREQ else ChannelType.REAL_TIME
                    val customType = (j % 10).toString()
                    assembler!!.addLog(event, c, customType)
                }
            }
        }
        t.start()
        t.join()
        Thread.sleep((20 * 1000).toLong())
        Assert.assertEquals(
            20, realtimeUploader.getAccumulativeLogCount()
                    + highFreqUploader.getAccumulativeLogCount()
        )
        val remainingLogs: Int = LogRecordManager.getInstance().getChannelLogs(
            ChannelType.NORMAL, 0, Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong()
        ).get().size
        Assert.assertEquals(0, remainingLogs.toLong())
    }

    // TODO(): fix this
//  @Test
//  public void malformedLogs() throws InterruptedException {
//    try {
//      final ClientLog.ReportEvent event = buildMalformedEvent();
//      assembler.addLog(event, Channel.NORMAL, "myType");
//      Thread.sleep(2 * 1000);
//    } catch (Exception e) {
//      Assert.assertEquals(e.getClass(), RuntimeException.class);
//
//    }
//  }

    // TODO(): fix this
    //  @Test
    //  public void malformedLogs() throws InterruptedException {
    //    try {
    //      final ClientLog.ReportEvent event = buildMalformedEvent();
    //      assembler.addLog(event, Channel.NORMAL, "myType");
    //      Thread.sleep(2 * 1000);
    //    } catch (Exception e) {
    //      Assert.assertEquals(e.getClass(), RuntimeException.class);
    //
    //    }
    //  }
    fun buildMockLaunchEvent(): ClientLog.ReportEvent {
        val event = ClientLog.ReportEvent()
        event.eventPackage = ClientEvent.EventPackage()
        event.eventPackage.launchEvent = ClientEvent.LaunchEvent()
        event.eventPackage.launchEvent.cold = true
        event.eventPackage.launchEvent.timeCost = 3000
        val sb = StringBuilder()
        for (i in 0 until (2 shl 20) + 10) {
            sb.append('a')
        }
        event.eventPackage.launchEvent.detail = sb.toString()
        return event
    }

    fun buildMalformedEvent(): ClientLog.ReportEvent? {
        val event = ClientLog.ReportEvent()
        event.eventPackage = ClientEvent.EventPackage()
        event.eventPackage.launchEvent = ClientEvent.LaunchEvent()
        event.eventPackage.launchEvent.cold = true
        event.eventPackage.launchEvent.timeCost = 3000
        event.eventPackage.launchEvent.detail = null
        return event
    }

}
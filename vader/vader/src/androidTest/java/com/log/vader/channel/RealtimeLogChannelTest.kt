package com.log.vader.channel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.log.vader.database.data.LogRecord
import com.log.vader.executor.ExecutorExt
import com.log.vader.upload.AllSuccessUploader
import com.log.vader.upload.BaseUploader
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class RealtimeLogChannelTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private var channel: LogChannel? = null
    private val uploader: BaseUploader by lazy {
        AllSuccessUploader(0)
    }

    @Before
    fun createDB() {
        channel = NormalLogChannel(
            LogChannelConfig(
                context,
                ChannelType.REAL_TIME,
                uploader,
                ExecutorExt.newSingleThreadScheduledExecutor("LogChannel_${ChannelType.REAL_TIME.name}"),
                100L
            )
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun sendPeriodically() {
        val maxq = 100
        val t = 10
        val index = AtomicInteger()
        val thread: Thread = object : Thread() {
            override fun run() {
                for (n in 0 until t) {
                    for (i in 0 until maxq) {
                        val idx = index.incrementAndGet().toLong()
                        LogRecord(
                            idx,
                            idx,
                            ChannelType.REAL_TIME,
                            "showEvent",
                            idx,
                            System.currentTimeMillis(),
                            ByteArray(0)
                        ).let {
                            channel?.enqueueLog(it)
                        }
                    }
                    try {
                        sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        thread.start()
        thread.join()
        Thread.sleep(1000)
        Assert.assertEquals(t * maxq, uploader.getAccumulativeLogCount())
    }

}
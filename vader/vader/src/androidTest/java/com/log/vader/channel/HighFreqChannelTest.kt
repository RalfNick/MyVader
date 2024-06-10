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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class HighFreqChannelTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val uploader: BaseUploader by lazy {
        AllSuccessUploader(TimeUnit.SECONDS.toMillis(5))
    }
    private var channel: LogChannel? = null

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
    fun channelLostData() {
        val maxq = 666
        val N = 10
        val index = AtomicInteger()
        val t: Thread = object : Thread() {
            override fun run() {
                for (n in 0 until N) {
                    for (i in 0 until maxq) {
                        val idx = index.incrementAndGet().toLong()
                        val log = LogRecord(
                            idx,
                            idx,
                            ChannelType.REAL_TIME,
                            "showEvent",
                            idx,
                            System.currentTimeMillis(),
                            ByteArray(0)
                        )
                        channel!!.enqueueLog(log)
                    }
                    try {
                        sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        t.start()
        t.join()
        Thread.sleep(10000)
        Assert.assertNotEquals(N * maxq, uploader.getAccumulativeLogCount())
    }

}
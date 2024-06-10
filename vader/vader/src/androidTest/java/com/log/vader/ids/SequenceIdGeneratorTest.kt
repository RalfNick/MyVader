package com.log.vader.ids

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.log.vader.channel.ChannelType
import com.log.vader.database.data.ChannelConverter
import com.log.vader.database.operate.LogRecordOperator
import com.log.vader.database.operate.LogRecordOperatorExt
import com.log.vader.seqid.SeqIdWrapper
import com.log.vader.seqid.SequenceIdGenerator
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SequenceIdGeneratorTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext()
    }
    private val operator: LogRecordOperator by lazy {
        getLogRecordOperator(context)
    }

    private lateinit var generator: SequenceIdGenerator

    @Before
    fun createGenerator() {
        operator.clear()
        generator = SequenceIdGenerator.get(context, operator)
        generator.deleteAll()
    }

    private fun getLogRecordOperator(context: Context): LogRecordOperator {
        return LogRecordOperatorExt.createMemoryDatabase(context)
    }

    @Test
    @Throws(Exception::class)
    fun sequenceId() {
        val customTypes: MutableList<String> = ArrayList()
        customTypes.add("launchEvent")
        customTypes.add("searchEvent")
        customTypes.add("showEvent")
        customTypes.add("taskEvent")
        customTypes.add("clickEvent")
        customTypes.add("deviceStatEvent")
        customTypes.add("apiCostDetailEvent")
        customTypes.add("wifiStatEvent")
        customTypes.add("videoStatEvent")
        val r = Random()
        for (i in 0..99) {
            val c: ChannelType = ChannelConverter.convertToChannel(r.nextInt(3))
            val customType = customTypes[r.nextInt(customTypes.size)]
            val idWrapper: SeqIdWrapper = generator.generateNextSeqId(c, customType)
            Log.d("sequenceId", "sequenceId = ${idWrapper.customSeqId}")
        }
        generator.awaitTerminate(5, TimeUnit.SECONDS)
        val c: ChannelType = ChannelConverter.convertToChannel(r.nextInt(3))
        val customType = customTypes[r.nextInt(customTypes.size)]
        val idWrapper: SeqIdWrapper = generator.generateNextSeqId(c, customType)
        Assert.assertEquals(101, idWrapper.seqId)
    }

}
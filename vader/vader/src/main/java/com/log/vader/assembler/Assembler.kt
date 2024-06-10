package com.log.vader.assembler

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.log.vader.ControlConfigState
import com.log.vader.DataType
import com.log.vader.VaderState
import com.log.vader.channel.ChannelType
import com.log.vader.channel.LogChannel
import com.log.vader.database.data.DbAction
import com.log.vader.database.data.DbActionType
import com.log.vader.database.operate.LogRecordManager
import com.log.vader.logger.LoggerExt
import com.log.vader.matcher.ControlConfigMatcher
import com.log.vader.seqid.SequenceIdGenerator
import com.log.vader.upload.UploadState
import java.util.*
import java.util.concurrent.Future

class Assembler private constructor(
    private val logManager: LogRecordManager,
    private val sequenceIdGenerator: SequenceIdGenerator,
    private val channels: Map<ChannelType, LogChannel>,
    private var logControlConfig: String = "",
    private val type: Int
) {

    companion object {
        private const val TAG = "Assembler"

        // 900KB
        private const val MAX_LOG_SIZE = 900 shl 10

        @Volatile
        private var assembler: Assembler? = null

        @JvmStatic
        fun getInstance(
            logManager: LogRecordManager,
            idGenerator: SequenceIdGenerator,
            channels: Map<ChannelType, LogChannel>,
            controlConfig: String,
            @DataType type: Int
        ): Assembler {
            if (assembler == null) {
                synchronized(Assembler::class) {
                    if (assembler == null) {
                        assembler =
                            Assembler(logManager, idGenerator, channels, controlConfig, type)
                    }
                }
            }
            return assembler!!
        }

        @VisibleForTesting
        @JvmStatic
        fun getInstanceTest(
            logManager: LogRecordManager,
            idGenerator: SequenceIdGenerator,
            channels: Map<ChannelType, LogChannel>,
            controlConfig: String,
            @DataType type: Int
        ): Assembler {
            return Assembler(logManager, idGenerator, channels, controlConfig, type)
        }
    }

    private var droppedLogCount: Long = 0
    private var controlConfigMatcher: ControlConfigMatcher
    private val radioRandom by lazy { Random() }

    init {
        controlConfigMatcher =
            ControlConfigMatcher.Factory.createControlConfigMatcher(type, logControlConfig)
        logManager.scheduleEvictingOutdatedLogs()
    }

    @WorkerThread
    fun <T> addLog(data: T, channelType: ChannelType, customType: String = ""): Future<*>? {
        return controlConfigMatcher.match(data!!).run {
            if (this.enableUpload()) {
                val sampleRadio = this.getSampleRadio()
                if (getRandomSampleRadio() < sampleRadio) {
                    uploadLog(data, channelType, customType)
                } else {
                    LoggerExt.event("addLog", "sample mode:sample = $sampleRadio")
                    droppedLogCount++
                    null
                }
            } else {
                LoggerExt.event("addLog", "not upload mode")
                droppedLogCount++
                null
            }
        }
    }

    fun updateLogControlConfig(newLogControlConfig: String) {
        logControlConfig = newLogControlConfig
        controlConfigMatcher =
            ControlConfigMatcher.Factory.createControlConfigMatcher(type, newLogControlConfig)
    }

    @WorkerThread
    fun getVaderState() = VaderState(
        getControlConfigState(),
        sequenceIdGenerator.getSequenceIdState(),
        logManager.getDatabaseState(),
        getUploadCountPair().run { UploadState(first, second) }
    )

    private fun getControlConfigState() = ControlConfigState(droppedLogCount, logControlConfig)

    private fun getRandomSampleRadio(): Float {
        return radioRandom.nextFloat()
    }

    private fun <T> uploadLog(data: T, channelType: ChannelType, customType: String): Future<*>? {
        var future: Future<*>? = null
        val idWrapper = sequenceIdGenerator.generateNextSeqId(channelType, customType)
        val logRecord = LogDataConverter.convert(data, channelType, customType, idWrapper)
            ?: return future
        val contentSize = logRecord.payload.size
        // 单条日志过大读取日志时候会失败，所以这里就不插入DB。大小限制每个手机都不同，大约在1~2MB之间。
        if (contentSize <= MAX_LOG_SIZE) {
            future = logManager.enqueueDbAction(DbAction(logRecord, DbActionType.ADD))
        } else {
            LoggerExt.event("uploadLog", "logRecord data is too large")
        }
        channels[channelType]?.enqueueLog(logRecord)
        return future
    }

    @WorkerThread
    fun uploadLatestLogImmediately() {
        channels.values.forEach {
            it.uploadLogImmediately()
        }
    }

    private fun getUploadCountPair(): Pair<Long, Long> {
        var successCount = 0L
        var failedCount = 0L
        channels.values.forEach {
            successCount += it.getUploadFailedCount()
            failedCount += it.getUploadFailedCount()
        }
        return Pair(successCount, failedCount)
    }

}
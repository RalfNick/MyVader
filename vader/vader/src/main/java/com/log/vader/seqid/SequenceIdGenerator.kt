package com.log.vader.seqid

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.log.vader.assembler.Assembler
import com.log.vader.channel.ChannelType
import com.log.vader.database.operate.LogRecordOperator
import com.log.vader.exception.DBException
import com.log.vader.executor.ExecutorExt
import com.log.vader.executor.LogExceptionRunnable
import com.log.vader.logger.LoggerExt
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class SequenceIdGenerator private constructor(context: Context, operator: LogRecordOperator) {

    companion object {
        private const val TAG = "SequenceIdGenerator"
        private const val SP_KEY = "SequenceId"
        private const val SEQ_ID = "SeqId"
        private const val CUSTOM_TYPE_KEY = "CustomKeys"

        @Volatile
        private var generator: SequenceIdGenerator? = null

        @JvmStatic
        fun get(context: Context, operator: LogRecordOperator): SequenceIdGenerator {
            if (generator == null) {
                synchronized(Assembler::class) {
                    if (generator == null) {
                        generator = SequenceIdGenerator(context, operator)
                    }
                }
            }
            return generator!!
        }
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
    }

    private val executor: ExecutorService by lazy {
        ExecutorExt.newSingleCoreSingleTaskExecutor(TAG)
    }
    private var initialized = false

    @Volatile
    private var nextSeqId: Long = 1
    private var generatedIdCount = 0L
    private var commitCount = 0L
    private var failedCommitCount = 0L
    private lateinit var creationChannelId: Map<ChannelType, Long>
    private val channelIds: Map<ChannelType, Long> = mutableMapOf()
    private val customIds: Map<String, Long> = mutableMapOf()

    init {
        initSequenceIdGenerator(operator)
    }

    private fun initSequenceIdGenerator(operator: LogRecordOperator) {
        if (initialized) {
            return
        }
        syncChannelIds(operator)
        syncSeqId(operator)
        syncCustomIds(operator)
        initialized = true
    }

    fun generateNextSeqId(channelType: ChannelType, customType: String): SeqIdWrapper {
        val seqId = nextSeqId++
        val channelId = channelIds[channelType] ?: 1
        channelIds[channelType] to channelId.plus(1)
        val customId = customIds[customType]?.plus(1) ?: 1
        customIds[customType] to customId
        generatedIdCount++
        executor.submit(LogExceptionRunnable { saveToSharedPreference() })
        return SeqIdWrapper(seqId, channelId, customId, System.currentTimeMillis())
    }

    fun deleteAll() {
        nextSeqId = 1
        ChannelType.values().forEachIndexed { _, channel ->
            channelIds[channel] to 1
        }
        customIds.forEach { entry ->
            customIds[entry.key] to 1
        }
        saveToSharedPreference()
    }

    fun getSequenceIdState(): SequenceIdState {
        return SequenceIdState.seqId(nextSeqId)
            .channelId(channelIds.toMap())
            .customId(customIds.toMap())
            .generatedIdCount(generatedIdCount)
            .commitCount(commitCount)
            .failedCommitCount(failedCommitCount)
            .build()
    }

    fun getCreationChannelMaxSeqId(channelType: ChannelType) = creationChannelId[channelType] ?: 0

    @Synchronized
    private fun saveToSharedPreference() {
        sharedPreferences.edit().let {
            it.putLong(SEQ_ID, nextSeqId)
            ChannelType.values().forEachIndexed { _, channel ->
                it.putLong(channel.name, channelIds[channel] ?: 1)
            }
            customIds.forEach { entry ->
                it.putLong(entry.key, entry.value)
            }
            val result = it.commit()
            commitCount++
            if (!result) {
                failedCommitCount++
                LoggerExt.exception(DBException("saveIdsToDisk", "commit error"))
            }
        }
    }

    private fun syncCustomIds(operator: LogRecordOperator) {
        sharedPreferences.getStringSet(CUSTOM_TYPE_KEY, mutableSetOf())?.forEach {
            var customId = sharedPreferences.getLong(it, 1)
            val customDbId = try {
                operator.getMaxCustomTypeSeqId(it)
            } catch (e: Exception) {
                LoggerExt.exception(DBException("syncCustomIds", "getMaxCustomTypeSeqId error ", e))
                1
            }
            if (customDbId > customId) {
                LoggerExt.event(
                    "customId_mismatch",
                    "customId : $customId customDbId: $customDbId"
                )
                customId = customDbId
            }
            customIds[it] to customId
        }
    }

    private fun syncSeqId(operator: LogRecordOperator) {
        nextSeqId = sharedPreferences.getLong(SEQ_ID, 1)
        val nextDbSeqId = try {
            operator.getMaxSeqId() + 1
        } catch (e: Exception) {
            LoggerExt.exception(DBException("syncSeqId", "getMaxSeqId error ", e))
            1
        }
        if (nextDbSeqId > nextSeqId) {
            LoggerExt.event(
                "nextSeqId_mismatch",
                "nextSeqId : $nextSeqId nextDbSeqId: $nextDbSeqId"
            )
            nextSeqId = nextDbSeqId
        }

    }

    private fun syncChannelIds(operator: LogRecordOperator) {
        ChannelType.values().forEach {
            var nextChannelId = sharedPreferences.getLong(it.name, 1)
            val nextDbChannelId = try {
                operator.getMaxChannelSeqId(it) + 1
            } catch (e: Exception) {
                LoggerExt.exception(DBException("syncChannelIds", "getMaxChannelSeqId error ", e))
                1
            }
            if (nextDbChannelId > nextChannelId) {
                LoggerExt.event(
                    "nextChannelId_mismatch",
                    "nextChannelId : $nextChannelId nextDbChannelId: $nextDbChannelId"
                )
                nextChannelId = nextDbChannelId
            }
            channelIds[it] to nextChannelId

        }
        creationChannelId = channelIds.toMap()
    }

    @VisibleForTesting
    @Throws(InterruptedException::class)
    fun awaitTerminate(timeout: Int, unit: TimeUnit) {
        executor.awaitTermination(timeout.toLong(), unit)
    }
}
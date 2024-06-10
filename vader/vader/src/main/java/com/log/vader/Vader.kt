package com.log.vader

import android.content.Context
import androidx.annotation.AnyThread
import com.log.vader.assembler.Assembler
import com.log.vader.channel.ChannelType
import com.log.vader.channel.DegradeStateChecker
import com.log.vader.database.operate.LogRecordManager
import com.log.vader.database.operate.LogRecordOperatorExt
import com.log.vader.exception.DBException
import com.log.vader.executor.ExecutorExt
import com.log.vader.executor.LogExceptionCallable
import com.log.vader.executor.LogExceptionRunnable
import com.log.vader.logger.LoggerExt
import com.log.vader.seqid.SequenceIdGenerator
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class Vader private constructor(
    context: Context,
    config: VaderConfig,
    logControlConfig: String
) {
    companion object {
        private const val TAG = "Vader"

        @JvmStatic
        fun getVader(context: Context, config: VaderConfig, logControlConfig: String): Vader {
            LoggerExt.setLogger(config.logger)
            return Vader(context, config, logControlConfig)
        }
    }

    private val executor: ExecutorService by lazy { ExecutorExt.newSingleThreadExecutor(TAG) }
    private val assembler: Assembler

    init {
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
            logControlConfig, DataType.FORMAT_PROTOCOL_BUF
        )
        config.channels.forEach {
            if (DegradeStateChecker.hasDelayLogs(context, it.key)) {
                DegradeStateChecker.clearDelayState(context, it.key)
                it.value.updateDelaySeqId(generator.getCreationChannelMaxSeqId(it.key))
            }
        }
    }

    fun <T> addLog(data: T, channelType: ChannelType, customType: String) {
        executor.submit(LogExceptionRunnable {
            assembler.addLog(data, channelType, customType)
        })
    }

    /**
     * 添加一条日志，只有写入到数据库之后才返回。使用场景通常是应用崩溃时候，需要在UncaughtExceptionHandler同步写入
     * 崩溃的日志信息。如果不同步写入，有可还没来及写入DB，进程就crash了，导致线上崩溃偏少。
     * true表示写入动作完成。false表示不需要写入，或者等待超时等。
     * 注意：写入完成不等于写入成功，有非常小概率会出现写入失败的情况。但是API返回结果并没有体现这个状态，
     * 原因是外面即使知道写入失败，也很难操作到数据库的一些状态。而且通常场景是app已经崩溃了，这时候再做更复杂的操作
     * 也很难成功。所以这里直接建议忽略这些情况，写入失败的具体异常可以从VaderConfig的logger中找到。
     *
     * @param data 日志数据
     * @param channelType 日志发送通道
     * @param customType 日志自定义类型（例如showEvent，clickEvent）
     * @param timeoutMillis 阻塞时间，单位毫秒
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> addLogBlocking(
        data: T,
        channelType: ChannelType,
        customType: String,
        timeoutMillis: Long
    ): Boolean {
        val future = executor.submit(LogExceptionCallable {
            (assembler.addLog(data, channelType, customType) as? Future<Boolean>)?.let {
                return@LogExceptionCallable getWithTimeout(it, timeoutMillis)
            } ?: false
        })
        return future.run {
            getWithTimeout(this, timeoutMillis) ?: false
        }
    }

    private fun <V> getWithTimeout(future: Future<V>, timeoutMillis: Long): V? {
        return try {
            future[timeoutMillis, TimeUnit.MILLISECONDS]
        } catch (e: Exception) {
            LoggerExt.exception(DBException("getWithTimeout", "addLogBlocking error $e"))
            null
        }
    }

    fun <T> uploadLatestLogImmediately() {
        executor.submit(LogExceptionRunnable {
            assembler.uploadLatestLogImmediately()
        })
    }

    @AnyThread
    fun getVaderState(callback: ((vaderState: VaderState) -> Unit)) {
        executor.submit(LogExceptionCallable {
            callback.invoke(assembler.getVaderState())
        })
    }

    @AnyThread
    fun updateLogControlConfig(newConfig: String) {
        executor.submit(LogExceptionRunnable {
            assembler.updateLogControlConfig(newConfig)
        })
    }
}
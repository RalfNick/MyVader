package com.log.vader.channel

import androidx.annotation.WorkerThread
import com.log.vader.database.operate.LogRecordManager
import com.log.vader.database.data.DbAction
import com.log.vader.database.data.DbActionType
import com.log.vader.database.data.LogRecord
import com.log.vader.executor.LogExceptionRunnable
import com.log.vader.upload.UploadPolicy
import com.log.vader.upload.UploadResult
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 延时日志上传的逻辑非常简单，从DB中读取小于某一个channelSeqId的日志，然后依次上传。
 */
class DegradeLogChannel(config: LogChannelConfig) : BaseLogChannel(config) {

    companion object {
        private const val TAG = "DegradeLogChannel"
        private const val MAX_LOG_COUNT = 500L
    }

    private var maxChannelDelayedSeqId = 0L
    private val channelType = config.channelType
    private val executorService: ScheduledExecutorService = config.executorService
    private var hasMore = false

    override var uploadPolicy = UploadPolicy(true)

    @WorkerThread
    override fun prepareLogs(): List<LogRecord> {
        val result = mutableListOf<LogRecord>()
        LogRecordManager.getInstance().run {
            hasMore = getLogRecordsOfChannelRange(
                MAX_LOG_COUNT, result,
                ChannelLogRange(channelType, 0, maxChannelDelayedSeqId + 1)
            )
        }
        return result
    }

    override fun onUploadResult(logs: List<LogRecord>, logResult: UploadResult) {
        if (logResult.success) {
            LogRecordManager.getInstance().enqueueDbAction(
                DbAction(ArrayList<LogRecord>().apply {
                    addAll(logs)
                }, DbActionType.DELETE)
            )
        }
    }

    override fun onDegrade() {
        // LEFT-DO-NOTHING
    }

    override fun shouldTerminateUpload() = hasMore

    override fun scheduleNextSending(nextSendIntervalMs: Long) {
        executorService.schedule(LogExceptionRunnable {
            upload()
        }, nextSendIntervalMs, TimeUnit.MILLISECONDS)
    }

    override fun updateDelaySeqId(id: Long) {
        maxChannelDelayedSeqId = id
    }

    override fun enqueueLog(logRecord: LogRecord) {
        // LEFT-DO-NOTHING
    }

    override fun uploadLogImmediately() {
        // LEFT-DO-NOTHING
    }
}
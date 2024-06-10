package com.log.vader.channel

import android.util.Log
import com.log.vader.database.data.LogRecord
import com.log.vader.logger.LoggerExt
import com.log.vader.upload.LogPolicy
import com.log.vader.upload.UploadPolicy
import com.log.vader.upload.UploadResult

abstract class BaseLogChannel constructor(config: LogChannelConfig) : LogChannel {

    companion object {
        private const val TAG = "LogChannel"
    }

    protected abstract var uploadPolicy: UploadPolicy

    @Volatile
    protected var nextRequestIntervalMs = config.defaultRequestIntervalMs
    private val retryState by lazy { RetryState(1000) }
    private val executorService = config.executorService
    private val logUploader = config.logUploader
    private var uploadSuccessCount = 0L
    private var uploadFailedCount = 0L

    @Volatile
    private var isInit = false

    open fun init() {
        if (isInit) {
            throw ChannelException(TAG, "Shouldn't start LogChannel twice")
        }
        isInit = true
        scheduleNextSending(nextRequestIntervalMs)
    }

    abstract fun prepareLogs(): List<LogRecord>

    abstract fun onUploadResult(logs: List<LogRecord>, logResult: UploadResult)

    abstract fun onDegrade()

    abstract fun shouldTerminateUpload(): Boolean

    abstract fun scheduleNextSending(nextSendIntervalMs: Long)

    override fun getUploadFailedCount() = uploadFailedCount

    override fun getUploadSuccessCount() = uploadSuccessCount

    internal fun upload() {
        val logs = prepareLogs()
        val logResult = uploadLogs(logs)
        onUploadResult(logs, logResult)
        if (logResult.policy == LogPolicy.DELAY) {
            onDegrade()
            executorService.shutdown()
            Log.d(TAG, "upload: onDegrade and shut down")
        } else if (!shouldTerminateUpload()) {
            scheduleNextSending(logResult.nextRequestInterval)
        }
    }

    private fun uploadLogs(logs: List<LogRecord>): UploadResult {
        if (logs.isNullOrEmpty()) {
            return UploadResult(true, nextRequestIntervalMs, LogPolicy.NORMAL)
        }
        Log.d(TAG, "realSendLogs: log count ${logs.size}")
        var result: UploadResult
        try {
            logUploader.upload(logs, uploadPolicy).apply {
                nextRequestIntervalMs = nextRequestPeriodInMs
                result = UploadResult(true, nextRequestIntervalMs, logPolicy)
            }
        } catch (e: Exception) {
            LoggerExt.exception(ChannelException(TAG, "realSendLogs failed"))
            result = UploadResult(false, nextRequestIntervalMs, LogPolicy.NORMAL)
        }
        if (result.success) {
            retryState.reset()
            uploadSuccessCount++
        } else {
            uploadFailedCount++
            retryState.increaseDelay()
            Log.d(TAG, "uploadLogs: Schedule retry after : ${retryState.retryDelayMs}")
            result = UploadResult(false, retryState.retryDelayMs, result.policy)
        }
        return result
    }

}
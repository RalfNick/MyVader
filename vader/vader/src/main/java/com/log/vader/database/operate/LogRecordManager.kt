package com.log.vader.database.operate

import androidx.annotation.WorkerThread
import com.log.vader.channel.ChannelLogRange
import com.log.vader.channel.ChannelType
import com.log.vader.database.data.*
import com.log.vader.exception.DBException
import com.log.vader.executor.ExecutorExt
import com.log.vader.executor.LogExceptionCallable
import com.log.vader.executor.LogExceptionRunnable
import com.log.vader.logger.LoggerExt
import java.util.*
import java.util.concurrent.*

class LogRecordManager private constructor() {
    companion object {
        private const val TAG = "LogRecordManager"

        // 最多merge DBAction的次数，不限制每一次的数量
        private const val MAX_MERGE_COUNT = 10

        // 抛弃x天以上的日志
        private const val OUTDATED_DAYS = 15
        private val logRecordManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            LogRecordManager()
        }

        @JvmStatic
        fun getInstance(): LogRecordManager {
            return logRecordManager
        }
    }

    private val queue: LinkedBlockingQueue<DbAction> by lazy { LinkedBlockingQueue() }
    private val dbExecutor: ExecutorService by lazy { ExecutorExt.newSingleThreadExecutor(TAG) }
    private var operator: LogRecordOperator? = null

    fun setLogOperator(logOperator: LogRecordOperator) {
        operator = logOperator
    }

    @Synchronized
    fun scheduleEvictingOutdatedLogs() {
        dbExecutor.submit {
            LogExceptionRunnable {
                val lowerBound =
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(OUTDATED_DAYS.toLong())
                val count = operator?.evictOutdatedLog(lowerBound) ?: 0
                if (count > 0) {
                    LoggerExt.event("evict_logs", "Evicting total : $count logs.")
                }
            }
        }
    }

    @Synchronized
    fun enqueueDbAction(action: DbAction): Future<*> {
        queue.offer(action)
        return dbExecutor.submit(LogExceptionRunnable {
            flushToSentinel()
        })
    }

    @Synchronized
    fun getChannelLogs(
        type: ChannelType,
        lb: Long,
        ub: Long,
        count: Long
    ): Future<List<LogRecord>> {
        queue.offer(DbAction(ArrayList<LogRecord>(), DbActionType.SENTINEL))
        return dbExecutor.submit(LogExceptionCallable<List<LogRecord>> {
            flushToSentinel()
            executeChannelQueryAction(type, lb, ub, count)
        })
    }

    @Synchronized
    fun clearAll() {
        dbExecutor.submit(LogExceptionRunnable {
            operator?.clear()
        })
    }


    private fun executeChannelQueryAction(
        type: ChannelType,
        lb: Long,
        ub: Long,
        count: Long
    ): List<LogRecord> {
        try {
            return operator?.getChannelLogsBetween(type, lb, ub, count) ?: emptyList()
        } catch (e: Exception) {
            LoggerExt.exception(DBException("query", " error method executeChannelQueryAction ", e))
        }
        return emptyList()
    }

    @WorkerThread
    fun getDatabaseState(): DatabaseState {
        return operator?.run {
            val timeStamp = getOldestLogTimestamp()
            val hour =
                if (timeStamp == 0L) 0 else TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - timeStamp)
            DatabaseState(getLogCount(), getMinLogSeqId(), getMaxLogSeqId(), hour.toInt())
        } ?: DatabaseState(0, 0, 0, 0)
    }

    @WorkerThread
    private fun flushToSentinel() {
        while (true) {
            val action = queue.poll()
            if (action == null || action.type == DbActionType.SENTINEL) {
                break
            }
            var count = 0
            var next = queue.peek()
            while (next != null) {
                // 同类型 merge
                if (action.mergeDbAction(next)) {
                    queue.poll()
                    count++
                    next = queue.peek()
                    if (count > MAX_MERGE_COUNT) {
                        break
                    }
                } else {
                    break
                }
            }
            batchSaveLogs(action)
        }
    }

    @WorkerThread
    private fun batchSaveLogs(action: DbAction) {
        when (action.type) {
            DbActionType.ADD -> {
                executeAddActionWithRetry(action)
            }
            DbActionType.DELETE -> {
                executeDeleteActionWithRetry(action)
            }
            else -> {
                throw DBException("add ", "Unknown DBAction type ${action.type}")
            }
        }
    }

    @WorkerThread
    private fun executeAddActionWithRetry(action: DbAction) {
        var isError = false
        try {
            operator?.addLogs(action.logs)
        } catch (e: Exception) {
            LoggerExt.exception(DBException("batch add", " add logs error ", e))
            isError = true
        }
        if (!isError) {
            return
        }
        action.logs.forEach {
            try {
                operator?.addLog(it)
            } catch (e: Exception) {
                LoggerExt.exception(DBException("single add", " add log error ", e))
            }
        }
    }

    @WorkerThread
    private fun executeDeleteActionWithRetry(action: DbAction) {
        var isError = false
        try {
            operator?.deleteLogs(action.logs)
        } catch (e: Exception) {
            LoggerExt.exception(DBException("batch delete", " delete logs error ", e))
            isError = true
        }
        if (!isError) {
            return
        }
        action.logs.forEach {
            try {
                operator?.deleteLog(it)
            } catch (e: Exception) {
                LoggerExt.exception(DBException("single delete", " delete log error ", e))
            }
        }
    }

    fun getLogRecordsOfChannelRange(
        count: Long,
        result: MutableList<LogRecord>,
        range: ChannelLogRange
    ): Boolean {
        var lastQueryDBEmpty = false
        var logs: List<LogRecord>? = null
        getChannelLogs(range.channelType, range.lower, range.upper, count).let {
            try {
                logs = it.get(1000L, TimeUnit.MILLISECONDS)
                lastQueryDBEmpty = logs.isNullOrEmpty()
            } catch (e: TimeoutException) {
                // 如果超时是正常现象，只是说明当前DB读写任务比较多
                LoggerExt.exception(DBException(TAG, " time out ", e))
            } catch (e: Exception) {
                LoggerExt.exception(DBException(TAG, " getLogRecordsOfChannelRange error ", e))
            }

        }
        logs?.let { result.addAll(it) }
        return lastQueryDBEmpty
    }
}
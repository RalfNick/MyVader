package com.log.vader.channel

import android.util.Log
import com.log.vader.database.operate.LogRecordManager
import com.log.vader.database.data.DbAction
import com.log.vader.database.data.DbActionType
import com.log.vader.database.data.LogRecord
import com.log.vader.executor.LogExceptionRunnable
import com.log.vader.upload.UploadPolicy
import com.log.vader.upload.UploadResult
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * LogChannel设计思路如下
 * Logs flow: [>... ... ... ... ... ... ...>]，日志方向最旧的日志--> 最新的日志

 * [    database      ][  queue  ]
 *                  <--|
 * EvictingQueue里保存的日志都seqId一定是严格递增的（不一定连续，因为每次发送队列中最新的N条），
 * 并且队列中一定有最新的一批日志，Database是完整的数据。
 * 每次发送时候做一次概念上的Snapshot，把queue中的最新MAX_LOG_COUNT条日志拿出来。
 * 如果不够MAX_LOG_COUNT那么就做一次DB查询，查询所有seqId小于队列头部元素的日志（也就是不可能在队列中的日志），
 * 然后把这两部分拼起来发送。
 * 最后如果EvictingQueue中没有日志，并且DB查询的结果也是空，就停止轮询，否则计划下一次发送。
 */
class NormalLogChannel(private val config: LogChannelConfig) : BaseLogChannel(config) {

    companion object {
        private const val TAG = "NormalLogChannel"
        private const val MAX_LOG_COUNT = 500
        private val lock = Any()
    }

    private val evictingQueue: Queue<LogRecord> by lazy {
        EvictingQueue(4 * MAX_LOG_COUNT)
    }

    private var maxChannelDelayedSeqId = 0L
    private var scheduledTask = config.executorService.schedule(LogExceptionRunnable {
        // LEFT-DO-NOTHING
    }, 0, TimeUnit.MILLISECONDS)
    private var lastDBQueryEmpty = false
    private var degraded = false

    @Synchronized
    override fun init() {
        super.init()
        startDegradeLogChannelIfNeeded()
    }

    private fun startDegradeLogChannelIfNeeded() {
        if (maxChannelDelayedSeqId > 0) {
            DegradeLogChannel(config).init()
        }
    }

    override var uploadPolicy = UploadPolicy(false)

    override fun enqueueLog(logRecord: LogRecord) {
        synchronized(lock) {
            evictingQueue.add(logRecord)
            if (scheduledTask.isDone) {
                scheduleNextSending(nextRequestIntervalMs)
            }
        }
    }

    /**
     * 如果没有正在执行或者计时的任务，立即开始上传。否则不做任何行为。通常用在app切换后台时候主动触发。
     */
    override fun uploadLogImmediately() {
        synchronized(lock) {
            if (scheduledTask.isDone) {
                scheduleNextSending(0)
            } else {
                // Pending or Running.
                // Cancel的顺序要在前面。cancel返回true表示取消成功或者正在执行。继续判断Delay > 0一定就是取消成功。
                // 这样避免提交多个scheduledTask（虽然可以用改变队列大小、加自定义变量等方式去判断，但是觉得没必要)
                // 这里有一些竞争的情况，例如：delay可能是1ms，马上就要执行，但是cancel返回了true，任务已经被取消了。
                // 后面getDelay语句得到的结果<=0，这时候会少发一次数据。
                // 考虑到这种情况非常非常少，并且及时发生了也没有严重后果，所以才去这种写法。如果delay判断写在前面
                // 就可能出现delay还>0，但是cancel的时候已经开始执行了，cancel(false)对已经执行的任务也返回true。
                // 就会可能出现2个sendLog的任务。如果多次出现可能有很多网络请求，服务端肯定不希望有这种情况。
                if (scheduledTask.cancel(false) &&
                    scheduledTask.getDelay(TimeUnit.MILLISECONDS) > 0
                ) {
                    scheduleNextSending(0)
                }
            }
        }
    }

    override fun prepareLogs(): List<LogRecord> {
        synchronized(lock) {
            Log.d(TAG, "prepareLogs: queue size is ${evictingQueue.size}")
            val logs = ArrayList<LogRecord>(MAX_LOG_COUNT.coerceAtMost(evictingQueue.size))
            val skipCount = 0.coerceAtLeast(evictingQueue.size - MAX_LOG_COUNT)
            evictingQueue.iterator().let {
                // 跳过队列中旧的埋点
                var count = skipCount
                while (count > 0 && it.hasNext()) {
                    count--
                    it.next()
                }
                while (it.hasNext()) {
                    logs.add(it.next())
                }
            }
            // 批量不足，则从数据库中读取
            ChannelLogRange(
                config.channelType,
                maxChannelDelayedSeqId + 1,
                evictingQueue.peek()?.channelSeqId ?: Int.MAX_VALUE.toLong()
            ).let {
                lastDBQueryEmpty = LogRecordManager.getInstance()
                    .getLogRecordsOfChannelRange(MAX_LOG_COUNT.toLong(), logs, it)
            }
            return logs
        }
    }

    override fun onUploadResult(logs: List<LogRecord>, logResult: UploadResult) {
        if (logResult.success) {
            synchronized(lock) {
                evictingQueue.removeAll(logs)
            }
            LogRecordManager.getInstance()
                .enqueueDbAction(DbAction(ArrayList(logs), DbActionType.DELETE))
        }
    }

    override fun onDegrade() {
        degraded = true
        DegradeStateChecker.setHasDelayLogs(config.context, config.channelType)
    }

    @Synchronized
    override fun shouldTerminateUpload(): Boolean {
        synchronized(lock) {
            // 内存中有数据，或者DB中还有待发的数据，就计划下一次发送。
            val terminate = evictingQueue.size == 0 && lastDBQueryEmpty
            Log.d(TAG, "shouldTerminateUpload = $terminate")
            return terminate
        }
    }

    override fun scheduleNextSending(nextSendIntervalMs: Long) {
        synchronized(lock) {
            if (degraded) {
                return
            }
            scheduledTask = config.executorService.schedule(LogExceptionRunnable {
                upload()
            }, nextSendIntervalMs, TimeUnit.MILLISECONDS)
        }
    }

    override fun updateDelaySeqId(id: Long) {
        maxChannelDelayedSeqId = id
    }
}
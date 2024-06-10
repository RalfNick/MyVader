package com.log.vader.executor

import android.os.Process
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(name: String) : ThreadFactory {

    companion object {
        @JvmStatic
        private val sPoolNumber = AtomicInteger(1)
    }

    private val threadNumber by lazy { AtomicInteger(1) }
    private var threadGroup: ThreadGroup? = null
    private var namePrefix: String? = null

    init {
        System.getSecurityManager()?.let {
            threadGroup = it.threadGroup
        } ?: let {
            threadGroup = Thread.currentThread().threadGroup
        }
        namePrefix = "$name-${sPoolNumber.getAndIncrement()}-"
    }

    override fun newThread(r: Runnable?): Thread {
        val runnable = r ?: Runnable {
            // LEFT-DO-NOTHING
        }
        return Thread(
            threadGroup,
            RevisePriorityRunnable(Process.THREAD_PRIORITY_BACKGROUND, runnable),
            namePrefix + threadNumber.getAndIncrement(),
            0L
        ).apply {
            if (isDaemon) {
                isDaemon = false
            }
            if (priority != Thread.NORM_PRIORITY) {
                priority = Thread.NORM_PRIORITY
            }
        }
    }

    private class RevisePriorityRunnable(
        private val priority: Int,
        private val r: Runnable
    ) : Runnable {

        override fun run() {
            Process.setThreadPriority(priority)
            r.run()
        }

    }
}
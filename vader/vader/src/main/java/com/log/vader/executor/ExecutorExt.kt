package com.log.vader.executor

import java.util.concurrent.*

object ExecutorExt {

    fun newSingleThreadExecutor(poolName: String): ExecutorService {
        return Executors.newSingleThreadExecutor(NamedThreadFactory(poolName))
    }

    fun newSingleThreadScheduledExecutor(poolName: String): ScheduledExecutorService {
        return Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(poolName))
    }

    fun newSingleCoreSingleTaskExecutor(poolName: String): ExecutorService {
        return ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            LinkedBlockingDeque(1),
            NamedThreadFactory(poolName),
            ThreadPoolExecutor.DiscardPolicy()
        )
    }
}
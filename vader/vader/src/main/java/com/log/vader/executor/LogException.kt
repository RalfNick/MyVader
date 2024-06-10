package com.log.vader.executor

import com.log.vader.logger.LoggerExt
import java.util.concurrent.Callable

internal class LogExceptionRunnable(private val runnable: () -> Unit) : Runnable {

    override fun run() {
        try {
            runnable.invoke()
        } catch (e: Exception) {
            LoggerExt.exception(e)
        }
    }

}

internal class LogExceptionCallable<V>(private val callable: () -> V) : Callable<V> {

    override fun call(): V? {
        try {
            return callable.invoke()
        } catch (e: Exception) {
            LoggerExt.exception(e)
        }
        return null
    }

}
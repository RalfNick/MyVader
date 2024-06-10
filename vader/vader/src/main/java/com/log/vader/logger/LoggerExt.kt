package com.log.vader.logger

import android.util.Log

object LoggerExt : Logger {

    private const val TAG = "LoggerExt"

    private var logger: Logger? = null

    fun setLogger(logger: Logger) {
        this.logger = logger
    }

    override fun event(key: String, value: String) {
        logger?.event(key, value) ?: defaultEvent(key, value)
    }

    override fun exception(e: Exception?) {
        logger?.exception(e) ?: defaultException(e)
    }

    private fun defaultEvent(key: String, value: String) {
        Log.e(key, value)
    }

    private fun defaultException(e: Exception?) {
        Log.e(TAG, e?.toString() ?: "unkown error")
    }

}
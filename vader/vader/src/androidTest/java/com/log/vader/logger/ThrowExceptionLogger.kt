package com.log.vader.logger

class ThrowExceptionLogger : Logger {

    override fun event(key: String, value: String) {
        println("key: $key, value: $value")

    }

    override fun exception(e: Exception?) {
        e?.printStackTrace()
    }
}
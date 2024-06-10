package com.log.vader.logger

interface Logger {

    fun event(key: String, value: String)

    fun exception(e: Exception?)
}
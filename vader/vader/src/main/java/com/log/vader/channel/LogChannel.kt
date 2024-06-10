package com.log.vader.channel

import com.log.vader.database.data.LogRecord

interface LogChannel {

    fun updateDelaySeqId(id: Long)

    fun enqueueLog(logRecord: LogRecord)

    /**
     *  如果没有正在执行或者计时的任务，立即开始上传。否则不做任何行为;通常用在app切换后台时候主动触发。
     */
    fun uploadLogImmediately()

    fun getUploadSuccessCount(): Long

    fun getUploadFailedCount(): Long
}
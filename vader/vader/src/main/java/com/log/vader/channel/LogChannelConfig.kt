package com.log.vader.channel

import android.content.Context
import com.log.vader.upload.LogUploader
import java.util.concurrent.ScheduledExecutorService

data class LogChannelConfig(
    val context: Context,
    val channelType: ChannelType,
    val logUploader: LogUploader,
    val executorService: ScheduledExecutorService,
    val defaultRequestIntervalMs: Long
)

package com.log.vader.upload

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class UploadPolicy(val degrade: Boolean)

data class UploadResult(val success: Boolean, val nextRequestInterval: Long, val policy: LogPolicy)

data class UploadResponse(
    @SerializedName("nextRequestPeriodInMs")
    val nextRequestPeriodInMs: Long,
    @SerializedName("logPolicy")
    val logPolicy: LogPolicy
)

data class UploadState(val totalCount: Long, val failedCount: Long)

@Keep
enum class LogPolicy {
    @SerializedName("NORMAL")
    NORMAL,

    @SerializedName("DELAY")
    DELAY,

    @SerializedName("DISCARD")
    DISCARD
}

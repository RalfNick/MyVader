package com.log.vader.database.data

data class DatabaseState(
    val stashedLogCount: Long,
    val maxStashedLogId: Long,
    val minStashedLogId: Long,
    val longestStashedDurationInHour: Int
)

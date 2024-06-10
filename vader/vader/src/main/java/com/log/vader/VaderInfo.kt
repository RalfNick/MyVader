package com.log.vader

import androidx.annotation.IntDef
import com.log.vader.channel.ChannelType
import com.log.vader.channel.LogChannel
import com.log.vader.database.data.DatabaseState
import com.log.vader.database.data.DatabaseType
import com.log.vader.logger.Logger
import com.log.vader.seqid.SequenceIdState
import com.log.vader.upload.UploadState

data class VaderConfig(
    val databaseConfig: DatabaseConfig,
    val channels: Map<ChannelType, LogChannel>,
    val logger: Logger,
    @DataType
    val dataType: Int
)

data class DatabaseConfig(val databaseName: String, val databaseType: DatabaseType)

data class VaderState(
    val controlConfigState: ControlConfigState,
    val sequenceIdState: SequenceIdState,
    val databaseState: DatabaseState,
    val uploadState: UploadState
)

data class ControlConfigState(val droppedLogCount: Long, val config: String)

@IntDef(
    DataType.FORMAT_PROTOCOL_BUF,
    DataType.FORMAT_JSON
)
@Retention(AnnotationRetention.SOURCE)
annotation class DataType {
    companion object {
        const val FORMAT_PROTOCOL_BUF = 0
        const val FORMAT_JSON = 1
    }
}

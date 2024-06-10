package com.log.vader.seqid

import com.log.vader.channel.ChannelType

data class SeqIdWrapper(
    val seqId: Long,
    val channelSeqId: Long,
    val customSeqId: Long,
    val clientTimestamp: Long
)

data class SequenceIdState(
    val seqId: Long,
    val channelId: Map<ChannelType, Long>,
    val customId: Map<String, Long>,
    val generatedIdCount: Long,
    val commitCount: Long,
    val failedCommitCount: Long
) {

    private constructor(builder: Builder) : this(
        builder.seqId,
        builder.channelId,
        builder.customId,
        builder.generatedIdCount,
        builder.commitCount,
        builder.failedCommitCount
    )

    companion object Builder {

        private var seqId: Long = 0L
        private var channelId: Map<ChannelType, Long> = emptyMap()
        private var customId: Map<String, Long> = emptyMap()
        private var generatedIdCount: Long = 0L
        private var commitCount: Long = 0L
        private var failedCommitCount: Long = 0L

        fun seqId(seqId: Long): Builder = apply {
            this.seqId = seqId
        }

        fun channelId(channelId: Map<ChannelType, Long>): Builder = apply {
            this.channelId = channelId
        }

        fun customId(customId: Map<String, Long>): Builder = apply {
            this.customId = customId
        }

        fun generatedIdCount(generatedIdCount: Long): Builder = apply {
            this.generatedIdCount = generatedIdCount
        }

        fun commitCount(commitCount: Long): Builder = apply {
            this.commitCount = commitCount
        }

        fun failedCommitCount(failedCommitCount: Long): Builder = apply {
            this.failedCommitCount = failedCommitCount
        }

        fun build() = SequenceIdState(this)

    }
}
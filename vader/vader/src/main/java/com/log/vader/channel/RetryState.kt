package com.log.vader.channel

class RetryState(private val initDelayMs: Long) {

    companion object {
        private const val MAX_DURATION_MS = 10000L
    }

    var retryDelayMs = initDelayMs

    fun reset() {
        retryDelayMs = initDelayMs
    }

    fun increaseDelay() {
        retryDelayMs = MAX_DURATION_MS.coerceAtMost(retryDelayMs.times(2))
    }
}
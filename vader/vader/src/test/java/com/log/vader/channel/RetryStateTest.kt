package com.log.vader.channel

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit

class RetryStateTest {

    @Test
    fun initialState() {
        val time = TimeUnit.SECONDS.toMillis(1)
        val retryState = RetryState(time)
        Assert.assertEquals(time, retryState.retryDelayMs)
    }

    @Test
    fun failedOnce() {
        val retryState = RetryState(TimeUnit.SECONDS.toMillis(1))
        retryState.increaseDelay()
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(2), retryState.retryDelayMs)
    }

    @Test
    fun failedManyTimes() {
        val retryState = RetryState(TimeUnit.SECONDS.toMillis(1))
        for (i in 0..9) {
            retryState.increaseDelay()
        }
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(10), retryState.retryDelayMs)
    }

}
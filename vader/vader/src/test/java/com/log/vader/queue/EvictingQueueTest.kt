package com.log.vader.queue

import com.log.vader.channel.EvictingQueue
import org.junit.Assert
import org.junit.Test

class EvictingQueueTest {

    @Test
    fun addElement() {
        val queue: EvictingQueue<String> = EvictingQueue(1000)
        for (i in 0..1999) {
            queue.add(i.toString())
        }
        val it: Iterator<String> = queue.iterator()
        for (i in 1000..1999) {
            Assert.assertEquals(it.next(), i.toString())
        }
        for (i in 1000..1999) {
            Assert.assertEquals(i.toString(), queue.poll())
        }
    }

}
package com.log.vader.channel

import java.util.*

/**
 * 包装一下，当超过容量时，移除队列头部元素，保留最新添加的元素
 */
class EvictingQueue<E> constructor(queueSize: Int) : Queue<E> {

    companion object {
        private const val DEFAULT_SIZE = 10
    }

    private var maxSize = queueSize
    private var delegateQueue: Queue<E>

    init {
        if (queueSize < 1) {
            maxSize = DEFAULT_SIZE
        }
        delegateQueue = ArrayDeque(maxSize)

    }

    override fun add(element: E): Boolean {
        if (maxSize == size) {
            delegateQueue.remove()
        }
        return delegateQueue.add(element)
    }

    override val size: Int
        get() = delegateQueue.size

    override fun addAll(elements: Collection<E>): Boolean {
        var result = false
        elements.forEach {
            result = result or add(it)
        }
        return result
    }

    override fun clear() {
        delegateQueue.clear()
    }

    override fun iterator() = delegateQueue.iterator()

    override fun remove(): E = delegateQueue.remove()

    override fun contains(element: E) = delegateQueue.contains(element)

    override fun containsAll(elements: Collection<E>) = delegateQueue.containsAll(elements)

    override fun isEmpty() = delegateQueue.isEmpty()

    override fun remove(element: E) = delegateQueue.remove(element)

    override fun removeAll(elements: Collection<E>) = delegateQueue.removeAll(elements)

    override fun retainAll(elements: Collection<E>) = delegateQueue.retainAll(elements)

    override fun offer(e: E) = add(e)

    override fun poll(): E? = delegateQueue.poll()

    override fun element(): E = delegateQueue.element()

    override fun peek(): E? = delegateQueue.peek()
}
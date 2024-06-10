package com.log.vader.seqid

import com.log.vader.exception.BaseException

abstract class ValueOrException<T> {

    companion object {

        @JvmStatic
        fun <V> ofValue(v: V): ValueOrException<V> {
            return ValueImpl(v)
        }

        @JvmStatic
        fun <V> ofException(exception: BaseException): ValueOrException<V> {
            return ExceptionImpl(exception)
        }
    }

    abstract fun isValue(): Boolean

    abstract fun getValue(): T

    abstract fun getException(): BaseException
}

private abstract class BaseValueOrException<T> : ValueOrException<T>() {

    override fun getValue(): T {
        throw SeqIdException("BaseValueOrException", "getValue, is value " + isValue())
    }

    override fun getException(): BaseException {
        throw SeqIdException("BaseValueOrException", "getException, is value " + isValue())
    }
}

private class ValueImpl<T>(private val v: T) : BaseValueOrException<T>() {

    override fun isValue() = true

    override fun getValue() = v
}

private class ExceptionImpl<T>(private val exception: BaseException) : BaseValueOrException<T>() {

    override fun isValue() = false

    override fun getException() = this.exception
}


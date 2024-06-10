package com.log.vader.matcher.data

import com.google.protobuf.nano.MessageNano

interface DataTypeAdapter<T : DataValue> {

    fun adapt(data: Any?): T?
}

class BooleanAdapter : DataTypeAdapter<BooleanValue> {

    override fun adapt(data: Any?) = (data as? Boolean)?.let { BooleanValue(it) }
}

class NoneAdapter : DataTypeAdapter<NoneValue> {

    override fun adapt(data: Any?) = if (data == null) NoneValue() else null

}

class NumberAdapter : DataTypeAdapter<NumberValue> {

    override fun adapt(data: Any?) = (data as? Number)?.let { NumberValue(it) }
}

class MessageNanoAdapter : DataTypeAdapter<MessageNanoValue> {

    override fun adapt(data: Any?) = (data as? MessageNano)?.let { MessageNanoValue(it) }
}

class StringOrJsonAdapter : DataTypeAdapter<StringOrJsonValue> {

    override fun adapt(data: Any?) = (data as? String)?.let { StringOrJsonValue(it) }
}
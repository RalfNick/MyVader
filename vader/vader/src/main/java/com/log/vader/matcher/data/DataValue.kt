package com.log.vader.matcher.data

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.protobuf.nano.MessageNano
import com.log.vader.logger.LoggerExt
import com.log.vader.matcher.Operator
import kotlin.math.abs

interface Validator {

    fun validate(operator: Operator, target: String): Boolean
}

interface DataValue : Validator {

    @Throws(DataValueException::class)
    fun parse(fieldName: String): DataValue
}

class NoneValue : DataValue {

    companion object {
        private val noneValue = NoneValue()
    }

    override fun parse(fieldName: String) = noneValue

    override fun validate(operator: Operator, target: String) = false

}

class BooleanValue(value: Boolean) : DataValue {

    private val booleanStr = if (value) "true" else "false"

    override fun parse(fieldName: String): DataValue {
        throw DataValueException(
            "BooleanValue",
            "BoolValue does not contain any field. Request field: $fieldName"
        )
    }

    override fun validate(operator: Operator, target: String): Boolean {
        if (operator == Operator.EQ) {
            return booleanStr.equals(target, true)
        } else {
            throw DataValueException(
                "BooleanValue", "Unsupported operator : $operator"
            )
        }
    }
}

class NumberValue(private val value: Number) : DataValue {

    companion object {
        private const val EPS = 1e-5f
    }

    override fun parse(fieldName: String): DataValue {
        throw DataValueException(
            "NumberValue",
            "NumberValue does not contain any field. Request field: $fieldName"
        )
    }

    override fun validate(operator: Operator, target: String): Boolean {
        if (operator == Operator.EQ) {
            val clazz = value::class.java
            return if (clazz == Int::class.java
                || clazz == Byte::class.java
                || clazz == Short::class.java
                || clazz == Long::class.java
            ) {
                target.toLong() == value
            } else {
                abs(target.toDouble().minus(value.toDouble())) < EPS
            }
        } else {
            throw DataValueException(
                "NumberValue", "Unsupported operator : $operator"
            )
        }
    }
}

class MessageNanoValue(private val messageNano: MessageNano) : DataValue {

    override fun parse(fieldName: String): DataValue {
        val field = try {
            messageNano.javaClass.getField(fieldName)
        } catch (e: Exception) {
            LoggerExt.exception(e)
            return DataValueFactory.buildDataValue(null)
        }
        field.let {
            it.isAccessible = true
            return DataValueFactory.buildDataValue(it.get(messageNano))
        }
    }

    override fun validate(operator: Operator, target: String) = true
}

class StringOrJsonValue(private val str: String) : DataValue {

    override fun parse(fieldName: String): DataValue {
        val element = JsonParser.parseString(str)
        if (!element.isJsonObject) {
            return NoneValue()
        }
        val nextElement = element.asJsonObject.get(fieldName) ?: return NoneValue()
        return when {
            nextElement.isJsonObject -> StringOrJsonValue(nextElement.toString())
            nextElement.isJsonPrimitive -> parsePrimitive(nextElement.asJsonPrimitive)
            nextElement.isJsonArray -> throw DataValueException(
                "StringOrJsonValue",
                "JsonArray is not supported yet. Request field : $fieldName"
            )
            nextElement.isJsonNull -> throw DataValueException(
                "StringOrJsonValue",
                ("JsonNull does not have more fields. Request field : $fieldName")
            )
            else -> throw DataValueException("StringOrJsonValue", "Unknown type : $nextElement")
        }
    }

    private fun parsePrimitive(primitive: JsonPrimitive) = when {
        primitive.isBoolean -> BooleanValue(primitive.asBoolean)
        primitive.isNumber -> NumberValue(primitive.asNumber)
        primitive.isString -> StringOrJsonValue(primitive.asString)
        else -> throw DataValueException(
            "StringOrJsonValue",
            "Unknown json primitive : $primitive"
        )
    }

    override fun validate(operator: Operator, target: String): Boolean {
        when (operator) {
            Operator.EQ -> JsonParser.parseString(str)?.run {
                return if (isJsonObject) asJsonObject == JsonParser.parseString(target).asJsonObject else str == target
            } ?: return false
            Operator.REGEX -> return str.matches(Regex(target))
            else -> throw DataValueException("StringOrJsonValue", "Unknown operator : $operator")
        }
    }

}
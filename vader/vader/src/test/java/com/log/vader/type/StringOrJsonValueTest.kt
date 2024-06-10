package com.log.vader.type

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.log.vader.matcher.Operator
import com.log.vader.matcher.data.DataValueException
import com.log.vader.matcher.data.NoneValue
import com.log.vader.matcher.data.StringOrJsonValue
import org.junit.Assert
import org.junit.Test

class StringOrJsonValueTest {

    @Test
    @Throws(Exception::class)
    fun stringOrJsonValueConstructor() {
        val value = StringOrJsonValue("")
        Assert.assertTrue(value.parse("non_exist") is NoneValue)
    }

    @Test
    fun stringOrJsonValueSingleValue() {
        val value = StringOrJsonValue("oooooook")
        Assert.assertTrue(value.validate(Operator.EQ, "oooooook"))
        Assert.assertTrue(value.validate(Operator.REGEX, "o.*k"))
    }

    @Test
    @Throws(Exception::class)
    fun stringOrJsonValueIntValue() {
        val value = StringOrJsonValue(buildMockJsonElement().toString())
        Assert.assertTrue(value.parse("field_a").validate(Operator.EQ, "100"))
    }

    @Test
    @Throws(Exception::class)
    fun stringOrJsonValueBooleanValue() {
        val value = StringOrJsonValue(buildMockJsonElement().toString())
        Assert.assertTrue(value.parse("field_b").validate(Operator.EQ, "true"))
    }

    @Test
    @Throws(DataValueException::class)
    fun stringOrJsonValueBooleanValueIgnoreCase() {
        val value = StringOrJsonValue(buildMockJsonElement().toString())
        Assert.assertTrue(value.parse("field_b").validate(Operator.EQ, "TruE"))
    }

    @Test
    @Throws(DataValueException::class)
    fun stringOrJsonValueArray() {
        val value = StringOrJsonValue(buildMockJsonElement().toString())
        Assert.assertTrue(value.parse("field_d").parse("1") is NoneValue)
    }

    @Test
    @Throws(DataValueException::class)
    fun stringOrJsonValueArrayRegex() {
        val value = StringOrJsonValue(buildMockJsonElement().toString())
        Assert.assertTrue(value.parse("field_d").validate(Operator.REGEX, ".*3.14.*"))
    }

    @Test
    @Throws(DataValueException::class)
    fun stringOrJsonValueChar() {
        val value = StringOrJsonValue(buildMockJsonElement().toString())
        Assert.assertTrue(value.parse("field_f").validate(Operator.EQ, "c"))
    }

    @Test
    @Throws(DataValueException::class)
    fun stringOrJsonValueObjectEquals() {
        val value = StringOrJsonValue(buildSimpleJsonObject().toString())
        Assert.assertTrue(
            value.validate(
                Operator.EQ,
                buildSimpleReversedJsonObjectString()
            )
        )
    }

    @Test
    @Throws(DataValueException::class)
    fun stringOrJsonValueNestedJson() {
        val value = StringOrJsonValue(buildMockDeepNestedJsonString()!!)
        Assert.assertTrue(
            value.parse("field_d")
                .parse("field_d")
                .parse("field_d")
                .parse("field_d")
                .parse("field_d")
                .parse("field_d")
                .parse("field_d")
                .parse("field_d")
                .parse("field_d")
                .validate(Operator.REGEX, ".*duck.*")
        )
    }

    private fun buildMockJsonElement(): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("field_a", 100)
        jsonObject.addProperty("field_b", true)
        jsonObject.addProperty("field_c", "duck")
        val array = JsonArray()
        array.add(true)
        array.add(100)
        array.add('c')
        array.add(3.14f)
        jsonObject.addProperty("field_d", array.toString())
        jsonObject.addProperty("field_e", jsonObject.toString())
        jsonObject.addProperty("field_f", 'c')
        return jsonObject
    }

    private fun buildMockDeepNestedJsonString(): String? {
        var tmpValue = ""
        for (i in 0..9) {
            val jsonObject = JsonObject()
            jsonObject.addProperty("field_a", 100)
            jsonObject.addProperty("field_b", true)
            jsonObject.addProperty("field_c", "duck")
            jsonObject.addProperty("field_d", tmpValue)
            tmpValue = jsonObject.toString()
        }
        return tmpValue
    }

    private fun buildSimpleJsonObject(): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("field_a", 100)
        jsonObject.addProperty("field_b", true)
        println(jsonObject)
        return jsonObject
    }

    private fun buildSimpleReversedJsonObjectString() =
        """{"field_b": true,"field_a": 100}"""

}
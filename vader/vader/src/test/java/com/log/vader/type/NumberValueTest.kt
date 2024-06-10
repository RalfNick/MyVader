package com.log.vader.type

import com.log.vader.matcher.Operator
import com.log.vader.matcher.data.DataValueException
import com.log.vader.matcher.data.NumberValue
import org.junit.Assert
import org.junit.Test

class NumberValueTest {
    @Test
    fun integerValueConstructor() {
        val numberValue = NumberValue(1)
        Assert.assertTrue(numberValue.validate(Operator.EQ, "1"))
        Assert.assertFalse(numberValue.validate(Operator.EQ, "0"))
    }

    @Test
    fun longValueConstructor() {
        val numberValue = NumberValue(1e20)
        Assert.assertTrue(numberValue.validate(Operator.EQ, "100000000000000000000"))
        Assert.assertFalse(numberValue.validate(Operator.EQ, "-100000000000000000000"))
    }

    @Test
    fun floatValueConstructor() {
        val numberValue = NumberValue(1.0f)
        Assert.assertTrue(numberValue.validate(Operator.EQ, "1"))
        Assert.assertFalse(numberValue.validate(Operator.EQ, "0"))
    }

    @Test
    fun doubleValueConstructor() {
        val numberValue = NumberValue(1234567.8910)
        Assert.assertTrue(numberValue.validate(Operator.EQ, "1234567.8910"))
        Assert.assertTrue(numberValue.validate(Operator.EQ, "1234567.89100001"))
        Assert.assertFalse(numberValue.validate(Operator.EQ, "1234567.89102"))
    }

    @Test(expected = DataValueException::class)
    @Throws(Exception::class)
    fun numberValueParseThrows() {
        val numberValue = NumberValue(1.0f)
        numberValue.parse("some_field_name")
    }

    @Test(expected = DataValueException::class)
    fun numberValueValidateThrows() {
        val numberValue = NumberValue(1.0f)
        numberValue.validate(Operator.EQ, "NoANumber")
    }

    @Test
    fun numberValueExpStyle() {
        val numberValue = NumberValue(1e5f)
        Assert.assertTrue(numberValue.validate(Operator.EQ, "1e5"))
    }
}
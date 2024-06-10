package com.log.vader.type

import com.log.vader.matcher.Operator
import com.log.vader.matcher.data.BooleanValue
import com.log.vader.matcher.data.DataValueException
import org.junit.Assert
import org.junit.Test

class BooleanValueTest {

    @Test
    fun boolValueConstructor() {
        val boolValue = BooleanValue(true)
        Assert.assertTrue(boolValue.validate(Operator.EQ, "true"))
        Assert.assertFalse(boolValue.validate(Operator.EQ, "false"))
        Assert.assertFalse(boolValue.validate(Operator.EQ, "0"))
    }

    @Test
    fun boolValueCaseSensitive() {
        val boolValue = BooleanValue(true)
        Assert.assertTrue(boolValue.validate(Operator.EQ, "TrUe"))
        Assert.assertTrue(boolValue.validate(Operator.EQ, "TRUE"))
    }

    @Test(expected = DataValueException::class)
    fun boolValueOperatorRegex() {
        val boolValue = BooleanValue(true)
        Assert.assertFalse(boolValue.validate(Operator.REGEX, "TrUe"))
        Assert.assertTrue(boolValue.validate(Operator.REGEX, ".rue"))
    }

    @Test(expected = DataValueException::class)
    @Throws(Exception::class)
    fun boolValueParseThrows() {
        val boolValue = BooleanValue(true)
        boolValue.parse("next_level_name")
    }
}
package com.log.vader.type

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.protobuf.nano.MessageNano
import com.kuaishou.client.log.content.packages.nano.ClientContent.SearchResultPackage
import com.kuaishou.client.log.event.packages.nano.ClientEvent
import com.kuaishou.client.log.packages.nano.ClientLog
import com.log.vader.matcher.Operator
import com.log.vader.matcher.data.DataValueException
import com.log.vader.matcher.data.MessageNanoValue
import com.log.vader.matcher.data.NoneValue
import org.junit.Assert
import org.junit.Test

class MessageNanoValueTest {
    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueConstructor() {
        val messageValue = MessageNanoValue(buildMockLaunchEvent())
        Assert.assertNotNull(messageValue)
        Assert.assertNotNull(messageValue.parse("eventPackage"))
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueParse() {
        val messageValue = MessageNanoValue(buildMockLaunchEvent())
        Assert.assertNotNull(
            messageValue.parse("eventPackage")
                .parse("launchEvent")
                .parse("timeCost")
        )
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueValidation() {
        val messageValue = MessageNanoValue(buildMockLaunchEvent())
        val value = messageValue.parse("eventPackage").parse("launchEvent")
        Assert.assertTrue(value.validate(Operator.EQ, ""))
        Assert.assertTrue(value.validate(Operator.REGEX, ""))
        Assert.assertTrue(value.validate(Operator.NOOP, ""))
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueIntValidation() {
        val messageValue = MessageNanoValue(buildMockLaunchEvent())
        val value = messageValue.parse("eventPackage").parse("launchEvent").parse("timeCost")
        Assert.assertTrue(value.validate(Operator.EQ, "100"))
    }

    @Test(expected = DataValueException::class)
    @Throws(DataValueException::class)
    fun messageNanoValueIntRegexThrows() {
        val messageValue = MessageNanoValue(buildMockLaunchEvent())
        val value = messageValue.parse("eventPackage").parse("launchEvent").parse("timeCost")
        Assert.assertTrue(value.validate(Operator.REGEX, "100"))
    }

    @Test(expected = Exception::class)
    @Throws(DataValueException::class)
    fun messageNanoValueNonExistPath() {
        val messageValue = MessageNanoValue(buildMockLaunchEvent())
        val value = messageValue.parse("eventPackage").parse("someNonExistPackage")
        Assert.assertNull(value)
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueFieldExistButNoValue() {
        val messageValue = MessageNanoValue(buildMockLaunchEvent())
        val value = messageValue.parse("eventPackage").parse("searchEvent")
        Assert.assertTrue(value is NoneValue)
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueWithJsonField() {
        val messageValue = MessageNanoValue(buildMockLaunchEventWithJsonDetails())
        val value = messageValue.parse("eventPackage").parse("launchEvent").parse("detail")
            .parse("field_a")
        Assert.assertTrue(value.validate(Operator.EQ, "100"))
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueWithJsonFieldNonExistPath() {
        val messageValue = MessageNanoValue(buildMockLaunchEventWithJsonDetails())
        val value = messageValue.parse("eventPackage").parse("launchEvent").parse("detail")
            .parse("field_z").parse("field_y")
        Assert.assertTrue(value is NoneValue)
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueWithJsonBoolean() {
        val messageValue = MessageNanoValue(buildMockLaunchEventWithJsonDetails())
        val value = messageValue.parse("eventPackage").parse("launchEvent").parse("detail")
            .parse("field_b")
        Assert.assertTrue(value.validate(Operator.EQ, "true"))
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueWithJsonString() {
        val messageValue = MessageNanoValue(buildMockLaunchEventWithJsonDetails())
        val value = messageValue.parse("eventPackage").parse("launchEvent").parse("detail")
            .parse("field_c")
        Assert.assertTrue(value.validate(Operator.EQ, "duck"))
        Assert.assertFalse(value.validate(Operator.EQ, "duck0"))
        Assert.assertTrue(value.validate(Operator.REGEX, ".u.k"))
        Assert.assertTrue(value.validate(Operator.REGEX, ".*k"))
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueWithNestedJson() {
        val messageValue = MessageNanoValue(buildMockLaunchEventWithJsonDetails())
        val value = messageValue.parse("eventPackage").parse("launchEvent").parse("detail")
            .parse("field_e").parse("field_a")
        Assert.assertTrue(value.validate(Operator.EQ, "100"))
    }

    @Test(expected = DataValueException::class)
    @Throws(DataValueException::class)
    fun messageNanoValueArrayThrows() {
        val messageValue = MessageNanoValue(buildMockSearchEvent())
        messageValue.parse("eventPackage").parse("searchEvent")
            .parse("searchResultPackage")
    }

    @Test
    @Throws(DataValueException::class)
    fun messageNanoValueCustomEvent() {
        val messageValue = MessageNanoValue(buildMockCustomEvent())
        Assert.assertTrue(
            messageValue.parse("eventPackage").parse("customEvent")
                .parse("value").validate(Operator.REGEX, ".*launch_event.*")
        )
    }

    private fun buildMockLaunchEvent(): MessageNano {
        val reportEvent = ClientLog.ReportEvent()
        reportEvent.eventPackage = ClientEvent.EventPackage()
        reportEvent.eventPackage.launchEvent = ClientEvent.LaunchEvent()
        reportEvent.eventPackage.launchEvent.timeCost = 100
        return reportEvent
    }

    private fun buildMockLaunchEventWithJsonDetails(): MessageNano {
        val reportEvent = ClientLog.ReportEvent()
        reportEvent.eventPackage = ClientEvent.EventPackage()
        reportEvent.eventPackage.launchEvent = ClientEvent.LaunchEvent()
        reportEvent.eventPackage.launchEvent.timeCost = 100
        reportEvent.eventPackage.launchEvent.detail = buildMockJsonElement().toString()
        return reportEvent
    }

    private fun buildMockJsonElement(): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("field_a", 100)
        jsonObject.addProperty("field_b", true)
        jsonObject.addProperty("field_c", "duck")
        jsonObject.addProperty("field_d", "duck")
        jsonObject.addProperty("field_e", jsonObject.toString())
        return jsonObject
    }

    private fun buildMockSearchEvent(): MessageNano {
        val reportEvent = ClientLog.ReportEvent()
        reportEvent.eventPackage = ClientEvent.EventPackage()
        reportEvent.eventPackage.searchEvent = ClientEvent.SearchEvent()
        reportEvent.eventPackage.searchEvent.searchResultPackage = arrayOfNulls(4)
        reportEvent.eventPackage.searchEvent.searchResultPackage[0] = SearchResultPackage()
        reportEvent.eventPackage.searchEvent.searchResultPackage[0].count = 100
        return reportEvent
    }

    private fun buildMockCustomEvent(): MessageNano {
        val reportEvent = ClientLog.ReportEvent()
        reportEvent.eventPackage = ClientEvent.EventPackage()
        reportEvent.eventPackage.customEvent = ClientEvent.CustomEvent()
        reportEvent.eventPackage.customEvent.key = "duck"
        reportEvent.eventPackage.customEvent.value = "This is a customed launch_event."
        return reportEvent
    }
}
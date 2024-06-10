package com.log.vader.config

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.log.vader.matcher.LogControlConfig
import com.log.vader.matcher.Operator
import com.log.vader.upload.LogPolicy
import com.log.vader.upload.UploadResponse
import org.junit.Assert
import org.junit.Test

class ConfigTest {

    private val gson = Gson()

    @Test
    fun logControlRulesConstructor() {
        val content = """{
  "rules": [
    {
      "matchingConditions": [
        {
          "path": [
            "statPackage",
            "deviceStatEvent"
          ]
        }
      ],
      "action": {},
      "sampleRatio": "1"
    }
  ]
}"""
        val config: LogControlConfig = gson.fromJson(content, LogControlConfig::class.java)
        Assert.assertNotNull(config)
        Assert.assertEquals(1, config.rules.size)
        val paths: List<String> = config.rules[0].matchingConditions?.get(0)?.paths ?: emptyList()
        Assert.assertNotNull(paths)
        Assert.assertEquals(2, paths.size.toLong())
        Assert.assertEquals("statPackage", paths[0])
        Assert.assertEquals("deviceStatEvent", paths[1])
    }

    @Test
    fun logControlRulesMoreConstructor() {
        val contnet = """{
  "rules": [
    {
      "matchingConditions": [
        {
          "path": [
            "eventPackage",
            "customEvent",
            "value"
          ],
          "op": "regex",
          "value": ".*launch_event.*"
        }
      ],
      "action": {
        "sampleRatio": "0.2"
      }
    },
    {
      "matchingConditions": [
        {
          "path": [
            "eventPackage",
            "customEvent"
          ]
        }
      ],
      "action": {
        "sampleRatio": "0.5"
      }
    },
    {
      "matchingConditions": [
        {
          "path": [
            "statPackage",
            "deviceStatEvent"
          ]
        }
      ],
      "action": {
        "sampleRatio": "0.1"
      }
    }
  ]
}"""
        val config: LogControlConfig = gson.fromJson(contnet, LogControlConfig::class.java)
        Assert.assertNotNull(config)
        Assert.assertEquals(3, config.rules.size)
        val firstPath: List<String> = config.rules[0].matchingConditions?.get(0)?.paths ?: emptyList()
        val secondPath: List<String> = config.rules[1].matchingConditions?.get(0)?.paths ?: emptyList()
        val thirdPath: List<String> = config.rules[2].matchingConditions?.get(0)?.paths ?: emptyList()
        Assert.assertNotNull(firstPath)
        Assert.assertNotNull(secondPath)
        Assert.assertNotNull(thirdPath)
        Assert.assertEquals(3, firstPath.size.toLong())
        Assert.assertEquals(2, secondPath.size.toLong())
        Assert.assertEquals(2, thirdPath.size.toLong())
        Assert.assertEquals("eventPackage", firstPath[0])
        Assert.assertEquals("customEvent", firstPath[1])
        Assert.assertEquals("value", firstPath[2])
    }

    @Test(expected = JsonSyntaxException::class)
    fun logControlRulesMalformed() {
        val contnet = "{\"rules\": [{\"path\": [\"statPackage\"," +
                "\"deviceStatEvent\"],\""
        val config: LogControlConfig = gson.fromJson(contnet, LogControlConfig::class.java)
    }

    @Test
    fun logControlRulesMissingOpField() {
        val content = """{
  "rules": [
    {
      "matchingConditions": [
        {
          "path": [
            "eventPackage",
            "customEvent",
            "value"
          ],
          "value": ".*launch_event.*"
        }
      ],
      "action": {
        "sampleRatio": "0.2"
      }
    }
  ]
}"""
        val config: LogControlConfig = gson.fromJson(content, LogControlConfig::class.java)
        Assert.assertEquals(
            Operator.NOOP,
            config.rules[0].matchingConditions?.get(0)?.operator
        )
    }

    @Test
    fun logControlRulesMissingActionField() {
        val content = """{"rules": [
    {
      "path": [
        "eventPackage",
        "customEvent",
        "value"
      ],
      "op": "regex",
      "value": ".*launch_event.*"
    }
  ]}"""
        val config: LogControlConfig = gson.fromJson(content, LogControlConfig::class.java)
        Assert.assertEquals(1f, config.rules[0].action.getSampleRadio(), 1e-5f)
    }

    @Test
    fun logControlRulesEmptyRules() {
        val content = """{"rules": []}"""
        val config: LogControlConfig = gson.fromJson(content, LogControlConfig::class.java)
        Assert.assertTrue(config.rules.isEmpty())
    }

    @Test
    fun logControlConfigWrongOperatorString() {
        val content = """{
  "rules": [
    {
      "matchingConditions": [
        {
          "path": [
            "eventPackage",
            "customEvent",
            "value"
          ],
          "op": "noop",
          "value": ".*launch_event.*"
        }
      ],
      "action": {
        "sampleRatio": "0.2"
      }
    }
  ]}"""
        val config: LogControlConfig = gson.fromJson(content, LogControlConfig::class.java)
        Assert.assertEquals(
            Operator.NOOP,
            config.rules[0].matchingConditions?.get(0)?.operator
        )
    }

    @Test
    fun delayedLogPolicy() {
        val responseString = """{
	"nextRequestPeriodInMs": 0,
	"logPolicy": "DELAY",
	"connected": true,
	"result": 1}"""
        val response: UploadResponse = Gson().fromJson(responseString, UploadResponse::class.java)
        Assert.assertEquals(LogPolicy.DELAY, response.logPolicy)
    }

    @Test
    fun illegalLogPolicy() {
        val responseString = """{
	"nextRequestPeriodInMs": 0,
	"logPolicy": "DISCARD",
	"connected": true,
	"result": 1}"""
        val response: UploadResponse = Gson().fromJson(responseString, UploadResponse::class.java)
        Assert.assertEquals(LogPolicy.DISCARD, response.logPolicy)
    }

}
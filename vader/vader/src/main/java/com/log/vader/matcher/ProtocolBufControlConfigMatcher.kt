package com.log.vader.matcher

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.protobuf.nano.MessageNano
import com.log.vader.logger.LoggerExt
import com.log.vader.matcher.data.DataValue
import com.log.vader.matcher.data.DataValueException
import com.log.vader.matcher.data.DataValueFactory

class ProtocolBufControlConfigMatcher(config: String? = "") : ControlConfigMatcher {

    private val defaultAction by lazy { ControlAction.ofNormalControlAction() }
    private var logControlConfig: LogControlConfig?

    init {
        logControlConfig = try {
            if (config.isNullOrEmpty()) {
                null
            } else {
                Gson().fromJson(config, LogControlConfig::class.java)
            }
        } catch (e: JsonParseException) {
            LoggerExt.exception(DataValueException("ProtocolBufMatcher", "json parse error", e))
            LogControlConfig(emptyList())
        }
    }

    override fun match(data: Any): ControlAction {
        if (data !is MessageNano) {
            return defaultAction
        }
        logControlConfig?.rules?.forEach {
            if (matchControlRule(it, data)) {
                return it.action
            }
        }
        return defaultAction
    }

    private fun matchControlRule(controlRule: ControlRule, data: Any): Boolean {
        var result = false
        var value: DataValue = DataValueFactory.buildDataValue(data)
        controlRule.matchingConditions?.forEach {
            it.paths?.forEach { path ->
                value = value.parse(path)
            }
            if (value.validate(it.operator, it.value)) {
                result = true
                return@forEach
            }
        }

        return result
    }
}
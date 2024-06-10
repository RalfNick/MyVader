package com.log.vader.matcher

import com.google.gson.Gson

class JsonControlConfigMatcher(config: String? = "") : ControlConfigMatcher {

    private val defaultAction by lazy { ControlAction.ofNormalControlAction() }
    private val logControlConfig = Gson().fromJson(config, LogControlConfig::class.java)

    override fun match(data: Any): ControlAction {
        return ControlAction.ofNormalControlAction()
    }
}
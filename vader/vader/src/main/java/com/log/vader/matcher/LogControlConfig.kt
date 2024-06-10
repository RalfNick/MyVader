package com.log.vader.matcher

import com.google.gson.annotations.SerializedName

data class LogControlConfig(val rules: List<ControlRule> = emptyList())

data class ControlRule(
    @SerializedName("matchingConditions")
    var matchingConditions: List<MatchCondition>? = emptyList(),
    @SerializedName("action")
    var action: ControlAction = ControlAction.ofNormalControlAction()
)

data class MatchCondition(
    @SerializedName("path")
    var paths: List<String>? = emptyList(),
    @SerializedName("op")
    var operator: Operator = Operator.NOOP,
    @SerializedName("value")
    var value: String = ""
)

enum class Operator {
    @SerializedName("eq")
    EQ,
    @SerializedName("regex")
    REGEX,
    @SerializedName("noop")
    NOOP
}

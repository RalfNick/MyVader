package com.log.vader.matcher

import com.log.vader.DataType

interface ControlConfigMatcher {

    /**
     * ProtocolBuf 数据匹配 - json string 数据匹配
     */
    fun match(data: Any): ControlAction

    class Factory {
        companion object {

            @JvmStatic
            fun createControlConfigMatcher(
                @DataType type: Int,
                config: String? = ""
            ): ControlConfigMatcher {
                return when (type) {
                    DataType.FORMAT_JSON -> JsonControlConfigMatcher(config)
                    DataType.FORMAT_PROTOCOL_BUF -> ProtocolBufControlConfigMatcher(config)
                    else -> throw IllegalArgumentException("unkown data type:$type")
                }
            }
        }
    }
}

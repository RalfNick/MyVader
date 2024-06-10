package com.log.vader.channel

import android.content.Context

object DegradeStateChecker {

    private const val SP_KEY = "ChannelDelayedState"

    fun hasDelayLogs(context: Context, channelType: ChannelType): Boolean {
        return context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).run {
            getBoolean(channelType.name, false)
        }
    }

    fun setHasDelayLogs(context: Context, channelType: ChannelType) {
        context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(channelType.name, true)
            .apply()
    }

    fun clearDelayState(context: Context, channelType: ChannelType) {
        context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
            .edit()
            .remove(channelType.name)
            .apply()
    }
}
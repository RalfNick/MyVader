package com.log.vader.matcher

import com.google.gson.annotations.SerializedName

open class ControlAction(
    @SerializedName("sampleRatio")
    private var sampleRadio: Float = 1f,
    private val enabled: Boolean = true
) {

    companion object {

        @JvmStatic
        fun ofNormalControlAction(enableUpload: Boolean = true): ControlAction {
            return NormalControlAction(enableUpload)
        }

        @JvmStatic
        fun ofSampleControlAction(sampleRadio: Float): ControlAction {
            return SampleControlAction(sampleRadio)
        }
    }

    open fun enableUpload(): Boolean = enabled

    open fun getSampleRadio(): Float = sampleRadio
}

private class NormalControlAction(private val enable: Boolean) : ControlAction() {

    override fun enableUpload() = enable
}

private class SampleControlAction(private val sampleRadio: Float) : ControlAction() {

    override fun getSampleRadio() = sampleRadio
}

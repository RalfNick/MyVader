package com.log.vader.upload

import com.log.vader.database.data.LogRecord
import java.io.IOException
import java.util.*

class AllSuccessUploader constructor(nextInterval: Long) : BaseUploader(nextInterval) {

    private val r = Random()

    @Throws(IOException::class)
    override fun upload(logs: List<LogRecord>, uploadPolicy: UploadPolicy): UploadResponse {
        try {
            Thread.sleep(r.nextInt(1000).toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return super.upload(logs, uploadPolicy)
    }
}

class AlwaysDegradeUploader constructor(nextInterval: Long) : BaseUploader(nextInterval) {

    @Throws(IOException::class)
    override fun upload(logs: List<LogRecord>, uploadPolicy: UploadPolicy): UploadResponse {
        val response: UploadResponse = super.upload(logs, uploadPolicy)
        return UploadResponse(response.nextRequestPeriodInMs, LogPolicy.DELAY)
    }
}

class FixedRequestCostUploader constructor(private val requestCostMs: Long, nextInterval: Long) :
    BaseUploader(nextInterval) {

    @Throws(IOException::class)
    override fun upload(logs: List<LogRecord>, uploadPolicy: UploadPolicy): UploadResponse {
        try {
            Thread.sleep(requestCostMs)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return super.upload(logs, uploadPolicy)
    }
}
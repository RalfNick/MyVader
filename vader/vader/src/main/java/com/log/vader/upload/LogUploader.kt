package com.log.vader.upload

import com.log.vader.database.data.LogRecord
import java.io.IOException

interface LogUploader {

    @Throws(IOException::class)
    fun upload(logs: List<LogRecord>, uploadPolicy: UploadPolicy): UploadResponse
}
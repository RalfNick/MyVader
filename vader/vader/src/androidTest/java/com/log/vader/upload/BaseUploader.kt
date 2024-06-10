package com.log.vader.upload

import android.util.Log
import com.log.vader.database.data.LogRecord
import java.io.IOException
import java.util.*

open class BaseUploader constructor(nextInterval: Long) : LogUploader {

    private var nextRequestInterval = nextInterval
    private var logPolicy: LogPolicy = LogPolicy.NORMAL
    private val sentLogIds: MutableSet<Long> = HashSet()

    @Throws(IOException::class)
    override fun upload(logs: List<LogRecord>, uploadPolicy: UploadPolicy): UploadResponse {
        if (logs.isNotEmpty()) {
            for (l in logs) {
                sentLogIds.add(l.seqId)
            }
            logs.sortedWith { o1, o2 -> o1.seqId.compareTo(o2.seqId) }
            Log.d(
                "Uploader", "size : " + logs.size
                        + ".[" + logs[0].seqId
                        + ", " + logs[logs.size - 1].seqId
                        + "]"
            )
        }
        return UploadResponse(nextRequestInterval, logPolicy)
    }

    open fun getAccumulativeLogCount(): Int {
        Log.d("Uploader", "accumulative size : " + sentLogIds.size)
        return sentLogIds.size
    }


}
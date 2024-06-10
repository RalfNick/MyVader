package com.log.vader.channel

import com.log.vader.exception.BaseException

class ChannelException : BaseException {

    constructor(operateTag: String? = "", message: String) : super(operateTag, message)

    constructor(operateTag: String? = "", message: String, cause: Throwable)
            : super(operateTag, message, cause)
}
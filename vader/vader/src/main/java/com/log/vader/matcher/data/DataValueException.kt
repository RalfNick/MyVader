package com.log.vader.matcher.data

import com.log.vader.exception.BaseException

class DataValueException : BaseException {

    constructor(tag: String? = "", message: String) : super(tag, message)

    constructor(tag: String? = "", message: String, cause: Throwable)
            : super(tag, message, cause)
}
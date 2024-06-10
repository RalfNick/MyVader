package com.log.vader.exception

class DBException : BaseException {

    constructor(operateTag: String? = "", message: String) : super(operateTag, message)

    constructor(operateTag: String? = "", message: String, cause: Throwable)
            : super(operateTag, message, cause)
}
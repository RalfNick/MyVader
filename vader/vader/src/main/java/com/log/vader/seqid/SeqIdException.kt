package com.log.vader.seqid

import com.log.vader.exception.BaseException

class SeqIdException : BaseException {

    constructor(tag: String? = "", message: String) : super(tag, message)

    constructor(tag: String? = "", message: String, cause: Throwable)
            : super(tag, message, cause)
}
package com.log.vader.exception

open class BaseException : Exception {

    private var tagName: String? = null

    constructor() : super()

    constructor(className: String?, message: String) : super(message) {
        tagName = className
    }

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(className: String?, message: String, cause: Throwable) : super(message, cause) {
        tagName = className
    }

    constructor(cause: Throwable) : super(cause)

    override val message: String?
        get() = if (tagName.isNullOrBlank()) super.message else "$tagName-${super.message}"
}
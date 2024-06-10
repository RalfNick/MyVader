package com.log.vader.database.data

data class DbAction constructor(val logs: ArrayList<LogRecord>, val type: DbActionType) {
    constructor(
        logRecord: LogRecord,
        type: DbActionType
    ) : this(arrayListOf<LogRecord>(logRecord), type)
}

enum class DbActionType {
    ADD,
    DELETE,
    SENTINEL
}

/**Sentinel don't merge with anyone including itself*/
fun DbAction.mergeDbAction(action: DbAction): Boolean {
    if (type == DbActionType.SENTINEL
        || action.type == DbActionType.SENTINEL
        || type != action.type
    ) {
        return false
    }
    logs.addAll(action.logs)
    return true
}
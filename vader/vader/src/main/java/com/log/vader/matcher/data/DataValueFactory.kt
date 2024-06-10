package com.log.vader.matcher.data

class DataValueFactory private constructor() {

    companion object {

        private val adapters by lazy {
            mutableListOf<DataTypeAdapter<*>>().apply {
                add(NoneAdapter())
                add(BooleanAdapter())
                add(NumberAdapter())
                add(StringOrJsonAdapter())
                add(MessageNanoAdapter())
            }
        }

        @JvmStatic
        fun buildDataValue(data: Any?): DataValue {
            adapters.forEach {
                val value = it.adapt(data)
                if (value != null) {
                    return value
                }
            }
            throw DataValueException("DataValueFactory", "Missing adapter for object: $data")
        }


    }
}
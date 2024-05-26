package com.gee12.mytetroid.model

import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.StringsIntMask
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.LogObj

class FoundType : StringsIntMask {

    companion object {
        const val TYPE_NONE = 0
        const val TYPE_RECORD = 1
        const val TYPE_RECORD_TEXT = 2
        const val TYPE_AUTHOR = 3
        const val TYPE_URL = 4
        const val TYPE_FILE = 5
        const val TYPE_TAG = 6
        const val TYPE_NODE = 7
        const val TYPE_NODE_ID = 8
        const val TYPE_RECORD_ID = 9
        const val TYPE_FILE_ID = 10
        const val TYPE_RECORD_FOLDER_NAME = 11

        // не хранятся в mytetra.xml
        const val TYPE_IMAGE = 12
    }

    constructor() : super()

    constructor(type: Int) : super() {
        addValue(type)
    }

    /**
     * Формирование строки в виде перечисления типов объекта, хранящихся в битах переменной type.
     * Формат: "тип1, тип2, .., типN"
     */
    fun getFoundTypeString(resourcesProvider: IResourcesProvider): String {
        // пропускаем 1, т.к. пропускаем тип NONE
        return joinToString(resourcesProvider.getStringArray(R.array.found_types), 1)
    }

    fun toLogObj(): LogObj? {
        return when {
            checkValue(TYPE_NONE) -> LogObj.NONE
            checkValue(TYPE_RECORD) -> LogObj.RECORD
            checkValue(TYPE_NODE) -> LogObj.NODE
            checkValue(TYPE_TAG) -> LogObj.TAG
            checkValue(TYPE_FILE) -> LogObj.FILE
            else -> null
        }
    }

}

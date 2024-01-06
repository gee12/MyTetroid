package com.gee12.mytetroid.domain

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.StringsIntMask
import com.gee12.mytetroid.domain.provider.BuildInfoProvider

/**
 * Класс для заполнения переменной-маски для быстрого получения выбранных значений
 * из множественного списка.
 *
 * @param context
 * @param option Список выбранных значений.
 */
class RecordFieldsSelector(
    context: Context,
    buildInfoProvider: BuildInfoProvider,
    option: Set<String?>?
) : StringsIntMask(
    option,
    context.resources.getStringArray(
        if (buildInfoProvider.isFullVersion()) R.array.record_fields_in_list_entries_pro else R.array.record_fields_in_list_entries
    )
) {
    companion object {
        private const val AUTHOR_INDEX = 0
        private const val TAGS_INDEX = 1
        private const val CREATED_INDEX = 2
        private const val EDITED_INDEX = 3
    }

    fun checkIsAuthor(): Boolean {
        return checkValue(AUTHOR_INDEX)
    }

    fun checkIsTags(): Boolean {
        return checkValue(TAGS_INDEX)
    }

    fun checkIsCreatedDate(): Boolean {
        return checkValue(CREATED_INDEX)
    }

    fun checkIsEditedDate(): Boolean {
        return checkValue(EDITED_INDEX)
    }

}
package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.utils.Utils

class CommonSettingsInteractor(
    private val logger: ITetroidLogger
) {

    /**
     * Проверка строки формата даты/времени.
     * В версии приложения <= 11 введенная строка в настройках не проверялась,
     *  что могло привести к падению приложения при отображении списка.
     */
    fun checkDateFormatString(context: Context): String {
        val dateFormatString = CommonSettings.getDateFormatString(context)
        return if (Utils.checkDateFormatString(dateFormatString)) {
            dateFormatString
        } else {
            logger.logWarning(context.getString(R.string.log_incorrect_dateformat_in_settings), true)
            context.getString(R.string.def_date_format_string)
        }
    }

}
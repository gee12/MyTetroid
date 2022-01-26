package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.common.utils.Utils
import java.io.File
import java.lang.Exception

class CommonSettingsInteractor(
    private val logger: ITetroidLogger? = null
) {

    fun getDefaultTrashPath(context: Context): String {
        return "${FileUtils.getAppExternalFilesDir(context)}/${Constants.TRASH_DIR_NAME}"
    }

    fun getDefaultLogPath(context: Context): String {
        return "${FileUtils.getAppExternalFilesDir(context)}/${Constants.LOG_DIR_NAME}"
    }

    fun createDefaultFolders(context: Context) {
        createFolder(context, getDefaultTrashPath(context), Constants.TRASH_DIR_NAME)
        createFolder(context, getDefaultLogPath(context), Constants.LOG_DIR_NAME)
    }

    fun createFolder(context: Context, path: String, name: String) {
        try {
            val dir = File(path)
            when (FileUtils.createDirsIfNeed(dir)) {
                1 -> logger?.log(context.getString(R.string.log_created_folder_mask, name, path), false)
                -1 -> logger?.logError(context.getString(R.string.log_error_creating_folder_mask, name, path), false)
                else -> {}
            }
        } catch (ex: Exception) {
            logger?.logError(ex, false)
        }
    }

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
            logger?.logWarning(context.getString(R.string.log_incorrect_dateformat_in_settings), true)
            context.getString(R.string.def_date_format_string)
        }
    }

}
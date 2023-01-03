package com.gee12.mytetroid.domain.provider

import android.content.Context
import android.preference.PreferenceManager
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.getAppExternalFilesDir
import com.gee12.mytetroid.common.extensions.getExternalPublicDocsOrAppDir
import com.gee12.mytetroid.common.extensions.isFileExist
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.StringList
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.RecordFieldsSelector
import com.gee12.mytetroid.domain.SortHelper
import com.gee12.mytetroid.domain.manager.SharedPreferencesManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.enums.TagsSearchMode

class CommonSettingsManager(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val buildInfoProvider: BuildInfoProvider,
) : SharedPreferencesManager(
    context = context,
    resourcesProvider = resourcesProvider,
    buildInfoProvider = buildInfoProvider,
) {

    fun init() {
        CommonSettings.settings = getPrefs()
        PreferenceManager.setDefaultValues(context, R.xml.prefs, false)

        // стартовые значения, которые нельзя установить в xml
        if (CommonSettings.getTrashPathDef(context) == null) {
            CommonSettings.setTrashPathDef(context, getDefaultTrashPath())
        }
        if (CommonSettings.getLogPath(context) == null) {
            CommonSettings.setLogPath(context, getDefaultLogPath())
        }
        if (buildInfoProvider.isFreeVersion()) {
            // принудительно отключаем
            CommonSettings.setIsLoadFavoritesOnly(context, false)
        }

        // удаление неактуальной опции из версии 4.1
        if (isContains(R.string.pref_key_is_show_tags_in_records)) {
            val isShow = CommonSettings.isShowTagsInRecordsList(context)
            val valuesSet = CommonSettings.getRecordFieldsInList(context)
            val value = resourcesProvider.getString(R.string.title_tags)
            // включение или отключение значения списка выбора
            CommonSettings.setRecordFieldsInList(context, CommonSettings.setItemInStringSet(context, valuesSet, isShow, value))
            // удаляем значение старой опции
            removePref(R.string.pref_key_is_show_tags_in_records)
        }
    }

    fun getDefaultTrashPath(): String {
        return makePath(context.getAppExternalFilesDir().orEmpty(), Constants.TRASH_DIR_NAME)
    }

    fun getDefaultLogPath(): String {
        return makePath(context.getAppExternalFilesDir().orEmpty(), Constants.LOG_DIR_NAME)
    }

    fun getLastFolderPathOrDefault(forWrite: Boolean): String? {
        val lastFolder = CommonSettings.getLastChoosedFolderPath(context)
        return if (!lastFolder.isNullOrEmpty() && lastFolder.isFileExist()) {
            lastFolder
        } else {
            context.getExternalPublicDocsOrAppDir(forWrite)
        }
    }

    fun getLogPath(): String {
        return CommonSettings.getLogPath(context).orEmpty()
    }

    fun isWriteLogToFile(): Boolean {
        return CommonSettings.isWriteLogToFile(context)
    }

    fun getSettingsVersion(): Int {
        return CommonSettings.getSettingsVersion(context)
    }

    fun getStoragePath(): String {
        return CommonSettings.getStoragePath(context).orEmpty()
    }

    fun getMiddlePassHash(): String {
        return CommonSettings.getMiddlePassHash(context).orEmpty()
    }

    fun getQuicklyNodeId(): String {
        return CommonSettings.getQuicklyNodeId(context).orEmpty()
    }

    fun getLastNodeId(): String {
        return CommonSettings.getLastNodeId(context).orEmpty()
    }

    fun getFavorites(): StringList {
        return CommonSettings.getFavorites(context)
    }

    fun isAskPassOnStart(): Boolean {
        return CommonSettings.isAskPassOnStart(context)
    }

    /**
     * Проверка строки формата даты/времени.
     * В версии приложения <= 11 введенная строка в настройках не проверялась,
     *  что могло привести к падению приложения при отображении списка.
     */
    fun checkDateFormatString(): String {
        val dateFormatString = CommonSettings.getDateFormatString(context)
        return if (Utils.checkDateFormatString(dateFormatString)) {
            dateFormatString
        } else {
            logger.logWarning(resourcesProvider.getString(R.string.log_incorrect_dateformat_in_settings), show = false)
            resourcesProvider.getString(R.string.def_date_format_string)
        }
    }

    fun isHighlightRecordWithAttach(): Boolean {
        return CommonSettings.isHighlightRecordWithAttach(context)
    }

    fun isHighlightCryptedNodes(): Boolean {
        return CommonSettings.isHighlightEncryptedNodes(context)
    }

    fun highlightAttachColor(): Int {
        return CommonSettings.getHighlightColor(context)
    }

    fun getRecordFieldsSelector(): RecordFieldsSelector {
        return RecordFieldsSelector(context, buildInfoProvider, CommonSettings.getRecordFieldsInList(context))
    }

    fun getTagsSortOrder(): String {
        return getString(R.string.pref_key_tags_sort_mode) ?: SortHelper.byNameAsc()
    }

    fun setTagsSortOrder(value: String) {
        setString(R.string.pref_key_tags_sort_mode, value)
    }

    fun getTagsSearchMode(): TagsSearchMode {
        return TagsSearchMode.getById(getInt(R.string.pref_key_tags_search_mode, TagsSearchMode.AND.id)) ?: TagsSearchMode.AND
    }

    fun setTagsSearchMode(value: TagsSearchMode) {
        setInt(R.string.pref_key_tags_search_mode, value.id)
    }

}
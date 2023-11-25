package com.gee12.mytetroid.domain.manager

import android.content.Context
import android.preference.PreferenceManager
import com.gee12.htmlwysiwygeditor.ActionButtonSize
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.getExternalPublicDocsOrAppDir
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.data.StringList
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.RecordFieldsSelector
import com.gee12.mytetroid.domain.SortHelper
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.enums.AppTheme
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

    fun getLastStorageId(): Int {
        return CommonSettings.getLastStorageId(context)
    }

    fun getLastSelectedFolderPathOrDefault(forWrite: Boolean): String? {
        val lastFolder = CommonSettings.getLastSelectedFolderPath(context)
        return if (!lastFolder.isNullOrEmpty() /*&& lastFolder.isFileExist()*/) {
            lastFolder
        } else {
            context.getExternalPublicDocsOrAppDir(forWrite)
        }
    }

    fun setLastSelectedFolderPath(path: String) {
        CommonSettings.setLastSelectedFolder(context, path)
    }

    fun isWriteLogToFile(): Boolean {
        return CommonSettings.isWriteLogToFile(context)
    }

    fun getSettingsVersion(): Int {
        return CommonSettings.getSettingsVersion(context)
    }

    @Deprecated("Устаревшее, используется для совместимости.")
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

    fun setTheme(theme: AppTheme) {
        setString(R.string.pref_key_theme, theme.id)
    }

    fun getTheme(default: AppTheme = AppTheme.LIGHT): AppTheme {
        return AppTheme.getById(getString(R.string.pref_key_theme) ?: default.id) ?: default
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

    fun isHasShowRecordFieldsValue(): Boolean {
        return isContains(R.string.pref_key_show_record_fields)
    }

    fun getShowRecordFields(): String? {
        return getString(
            R.string.pref_key_show_record_fields,
            resourcesProvider.getString(R.string.pref_show_record_fields_no)
        )
    }

    fun isHasEditorButtonsSizeValue(): Boolean {
        return isContains(R.string.pref_key_editor_toolbar_buttons_size)
    }

    fun getEditorButtonsSize(): ActionButtonSize {
        //FIXME: ListPreference сохраняет числовое значение в String
        val stringValue = getString(R.string.pref_key_editor_toolbar_buttons_size)
        return ActionButtonSize.getById(stringValue?.toInt() ?: ActionButtonSize.MEDIUM.id)
    }

}
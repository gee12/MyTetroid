package com.gee12.mytetroid.domain.provider

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.gee12.mytetroid.BuildConfig
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
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.enums.TagsSearchMode

// TODO: CommonSettingsManager
class CommonSettingsProvider(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val buildInfoProvider: BuildInfoProvider,
) {
    private var isCopiedFromFree = false

    val settings: SharedPreferences?
        get() = CommonSettings.settings
            ?: getPrefs().also {
                CommonSettings.settings = it
            }

    //region Загрузка настроек

    /**
     * Инициализация настроек.
     */
    fun init(context: Context) {
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

    private fun getPrefs(): SharedPreferences? {
        //SettingsManager.settings = PreferenceManager.getDefaultSharedPreferences(context);
        // SecurityException: MODE_WORLD_READABLE no longer supported
        val mode = if (BuildConfig.VERSION_CODE < 24) Context.MODE_WORLD_READABLE else Context.MODE_PRIVATE
        val defAppId = BuildConfig.DEF_APPLICATION_ID

        //if (BuildConfig.DEBUG) defAppId += ".debug";
        val prefs: SharedPreferences?
        if (buildInfoProvider.isFullVersion()) {
            prefs = getPrefs(Context.MODE_PRIVATE)
            if (prefs != null && prefs.all.size == 0) {
                // настроек нет, версия pro запущена в первый раз
                try {
                    val freeContext = context.createPackageContext(defAppId, Context.CONTEXT_IGNORE_SECURITY)
                    val freePrefs = freeContext.getSharedPreferences(
                        defAppId + CommonSettings.PREFS_NAME, mode
                    )
                    if (freePrefs.all.isNotEmpty()) {
                        // сохраняем все настройки из free в pro
                        copyPrefs(freePrefs, prefs)
                        isCopiedFromFree = true
                    }
                } catch (ex: Exception) {
                    return prefs
                }
            }
            return prefs
        } else {
            // открываем доступ к чтению настроек для версии Pro
            prefs = getPrefs(mode)
        }
        return prefs
    }

    private fun getPrefs(mode: Int): SharedPreferences? {
        val prefs: SharedPreferences? = try {
            context.getSharedPreferences(BuildConfig.APPLICATION_ID + CommonSettings.PREFS_NAME, mode)
        } catch (ex: Exception) {
            PreferenceManager.getDefaultSharedPreferences(context)
        }
        return prefs
    }

    /**
     * Копирование настроек.
     * @param srcPrefs
     * @param destPrefs
     */
    private fun copyPrefs(srcPrefs: SharedPreferences, destPrefs: SharedPreferences) {
        val srcMap = srcPrefs.all
        val destEditor = destPrefs.edit()
        for ((key, value) in srcMap) {
            when (value) {
                is Boolean -> destEditor.putBoolean(key, (value as Boolean))
                is String -> destEditor.putString(key, value as String)
                is Int -> destEditor.putInt(key, (value as Int))
                is Float -> destEditor.putFloat(key, (value as Float))
                is Long -> destEditor.putLong(key, (value as Long))
                //is Set<*> -> destEditor.putStringSet(key, Set::class.java.cast(value))
                is Set<*> -> destEditor.putStringSet(key, value.map { it.toString() }.toSet())
            }
        }
        destEditor.apply()
    }

    fun isCopiedFromFree(): Boolean {
        return isCopiedFromFree
    }

    //endregion Загрузка настроек

    /**
     * Удаление опции из настроек.
     * @param prefKeyStringRes
     */
    private fun removePref(prefKeyStringRes: Int) {
        if (settings?.contains(resourcesProvider.getString(prefKeyStringRes)) == true) {
            val editor = settings?.edit()
            editor?.remove(resourcesProvider.getString(prefKeyStringRes))
            editor?.apply()
        }
    }

    /**
     * Проверка существования значения опции в настройках.
     * @param prefKeyStringRes
     * @return
     */
    fun isContains(prefKeyStringRes: Int): Boolean {
        return settings?.contains(resourcesProvider.getString(prefKeyStringRes)) == true
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

    //region Getters

    private fun getInt(id: Int, default: Int = 0): Int {
        return settings?.getInt(resourcesProvider.getString(id), default) ?: default
    }

    private fun getString(id: Int, default: String? = null): String? {
        return settings?.getString(resourcesProvider.getString(id), default) ?: default
    }

    //endregion Getters

    //region Setters

    private fun setInt(id: Int, value: Int) {
        val editor = settings?.edit()
        editor?.putInt(resourcesProvider.getString(id), value)
        editor?.apply()
    }

    private fun setString(id: Int, value: String) {
        val editor = settings?.edit()
        editor?.putString(resourcesProvider.getString(id), value)
        editor?.apply()
    }

    //endregion Setters

}
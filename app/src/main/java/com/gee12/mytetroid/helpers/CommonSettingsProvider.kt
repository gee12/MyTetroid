package com.gee12.mytetroid.helpers

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.gee12.mytetroid.*
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.interactors.CommonSettingsInteractor
import org.jsoup.internal.StringUtil
import java.io.File

class CommonSettingsProvider(
    private val context: Context,
    private val appBuildHelper: AppBuildHelper,
) {
    private var isCopiedFromFree = false

    //region Загрузка настроек

    /**
     * Инициализация настроек.
     */
    fun init(
        context: Context,
        interactor: CommonSettingsInteractor
    ) {
        CommonSettings.settings = getPrefs()
        PreferenceManager.setDefaultValues(context, R.xml.prefs, false)

        // стартовые значения, которые нельзя установить в xml
        if (CommonSettings.getTrashPathDef(context) == null) {
            CommonSettings.setTrashPathDef(context, interactor.getDefaultTrashPath(context))
        }
        if (CommonSettings.getLogPath(context) == null) {
            CommonSettings.setLogPath(context, interactor.getDefaultLogPath(context))
        }
        if (appBuildHelper.isFreeVersion()) {
            // принудительно отключаем
            CommonSettings.setIsLoadFavoritesOnly(context, false)
        }

        // удаление неактуальной опции из версии 4.1
        if (isContains(R.string.pref_key_is_show_tags_in_records)) {
            val isShow = CommonSettings.isShowTagsInRecordsList(context)
            val valuesSet = CommonSettings.getRecordFieldsInList(context)
            val value = context.getString(R.string.title_tags)
            // включение или отключение значения списка выбора
            CommonSettings.setRecordFieldsInList(context, CommonSettings.setItemInStringSet(context, valuesSet, isShow, value))
            // удаляем значение старой опции
            removePref(R.string.pref_key_is_show_tags_in_records)
        }
        App.IsHighlightAttach = CommonSettings.isHighlightRecordWithAttach(context)
        App.IsHighlightCryptedNodes = CommonSettings.isHighlightEncryptedNodes(context)
        App.HighlightAttachColor = CommonSettings.getHighlightColor(context)
        App.DateFormatString = CommonSettings.getDateFormatString(context)
        App.RecordFieldsInList = RecordFieldsSelector(context, appBuildHelper, CommonSettings.getRecordFieldsInList(context))
    }

    private fun getPrefs(): SharedPreferences? {
        //SettingsManager.settings = PreferenceManager.getDefaultSharedPreferences(context);
        // SecurityException: MODE_WORLD_READABLE no longer supported
        val mode = if (BuildConfig.VERSION_CODE < 24) Context.MODE_WORLD_READABLE else Context.MODE_PRIVATE
        val defAppId = BuildConfig.DEF_APPLICATION_ID

        //if (BuildConfig.DEBUG) defAppId += ".debug";
        val prefs: SharedPreferences?
        if (appBuildHelper.isFullVersion()) {
            prefs = getPrefs(Context.MODE_PRIVATE)
            if (prefs != null && prefs.all.size == 0) {
                // настроек нет, версия pro запущена в первый раз
                try {
                    val freeContext = context.createPackageContext(defAppId, Context.CONTEXT_IGNORE_SECURITY)
                    val freePrefs = freeContext.getSharedPreferences(
                        defAppId + CommonSettings.PREFS_NAME, mode
                    )
                    if (freePrefs.all.size > 0) {
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
        if (CommonSettings.settings == null) return
        if (CommonSettings.settings.contains(context.getString(prefKeyStringRes))) {
            val editor = CommonSettings.settings.edit()
            editor.remove(context.getString(prefKeyStringRes))
            editor.apply()
        }
    }

    /**
     * Проверка существования значения опции в настройках.
     * @param prefKeyStringRes
     * @return
     */
    fun isContains(prefKeyStringRes: Int): Boolean {
        return if (CommonSettings.settings == null) {
            false
        } else {
            CommonSettings.settings.contains(context.getString(prefKeyStringRes))
        }
    }

    /**
     * Получение настроек.
     * @return
     */
    fun getSettings(): SharedPreferences {
        if (CommonSettings.settings == null) {
            CommonSettings.settings = getPrefs()
        }
        return CommonSettings.settings
    }

    fun getLastFolderPathOrDefault(forWrite: Boolean): String? {
        val lastFolder = CommonSettings.getLastChoosedFolderPath(context)
        return if (!StringUtil.isBlank(lastFolder) && File(lastFolder).exists()) lastFolder
        else FileUtils.getExternalPublicDocsOrAppDir(context, forWrite)
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

}
package com.gee12.mytetroid.domain.manager

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider

open class SharedPreferencesManager(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val buildInfoProvider: BuildInfoProvider,
) {

    private var isCopiedFromFree = false

    val settings: SharedPreferences?
        get() = CommonSettings.settings
            ?: getPrefs().also {
                CommonSettings.settings = it
            }


    //region Init

    protected fun getPrefs(): SharedPreferences? {
        //SettingsManager.settings = PreferenceManager.getDefaultSharedPreferences(context);
        // SecurityException: MODE_WORLD_READABLE no longer supported
        val mode = if (BuildConfig.VERSION_CODE < 24) Context.MODE_WORLD_READABLE else Context.MODE_PRIVATE
        val defAppId = BuildConfig.DEF_APPLICATION_ID

        //if (BuildConfig.DEBUG) defAppId += ".debug";
        val prefs: SharedPreferences?
        if (buildInfoProvider.isFullVersion()) {
            prefs = getPrefs(Context.MODE_PRIVATE)
            if (prefs != null && prefs.all.isEmpty()) {
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
            ex.printStackTrace()
            PreferenceManager.getDefaultSharedPreferences(context)
        }
        return prefs
    }

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

    //endregion Init

    protected fun removePref(prefKeyStringRes: Int) {
        if (settings?.contains(resourcesProvider.getString(prefKeyStringRes)) == true) {
            val editor = settings?.edit()
            editor?.remove(resourcesProvider.getString(prefKeyStringRes))
            editor?.apply()
        }
    }

    fun isContains(prefKeyStringRes: Int): Boolean {
        return settings?.contains(resourcesProvider.getString(prefKeyStringRes)) == true
    }

    //region Getters

    @Deprecated("")
    protected fun getInt(id: Int, default: Int = 0): Int {
        return settings?.getInt(resourcesProvider.getString(id), default) ?: default
    }

    protected fun getInt(id: String, default: Int = 0): Int {
        return settings?.getInt(id, default) ?: default
    }

    @Deprecated("")
    protected fun getString(id: Int, default: String? = null): String? {
        return settings?.getString(resourcesProvider.getString(id), default) ?: default
    }

    protected fun getString(id: String, default: String? = null): String? {
        return settings?.getString(id, default) ?: default
    }

    //endregion Getters

    //region Setters

    @Deprecated("")
    protected fun setInt(id: Int, value: Int) {
        val editor = settings?.edit()
        editor?.putInt(resourcesProvider.getString(id), value)
        editor?.apply()
    }

    protected fun setInt(id: String, value: Int) {
        val editor = settings?.edit()
        editor?.putInt(id, value)
        editor?.apply()
    }

    @Deprecated("")
    protected fun setString(id: Int, value: String) {
        val editor = settings?.edit()
        editor?.putString(resourcesProvider.getString(id), value)
        editor?.apply()
    }

    protected fun setString(id: String, value: String) {
        val editor = settings?.edit()
        editor?.putString(id, value)
        editor?.apply()
    }

    //endregion Setters

}
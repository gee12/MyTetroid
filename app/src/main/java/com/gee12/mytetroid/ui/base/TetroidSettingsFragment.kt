package com.gee12.mytetroid.ui.base

import android.app.Application
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.anggrayudi.storage.SimpleStorageHelper
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.ui.settings.CommonSettingsViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

open class TetroidSettingsFragment :
    PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    // TODO: заюзать VM из TetroidSettingsActivity
    protected open val baseViewModel: CommonSettingsViewModel by inject()
    protected open val resourcesProvider: IResourcesProvider by inject()
    protected open val settingsManager: CommonSettingsManager by inject()

    protected val application: Application
        get() = requireContext().applicationContext as Application

    protected open val settingsActivity: TetroidSettingsActivity<*>?
        get() = activity as? TetroidSettingsActivity<*>

    protected val optionsMenu: Menu?
        get() = settingsActivity?.optionsMenu

    protected val fileStorageHelper: SimpleStorageHelper?
        get() = settingsActivity?.fileStorageHelper


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    // TODO: заюзать VM из TetroidSettingsActivity
    protected open fun initViewModel() {
        lifecycleScope.launch {
            baseViewModel.messageEventFlow.collect { message -> settingsActivity?.showMessage(message) }
        }
        lifecycleScope.launch {
            baseViewModel.eventFlow.collect { event -> onViewEvent(event) }
        }
    }

    // TODO: заюзать VM из TetroidSettingsActivity
    open fun onViewEvent(event: BaseEvent) {
        when (event) {
            is BaseEvent.ShowProgress -> settingsActivity?.setProgressVisibility(true)
            is BaseEvent.HideProgress -> settingsActivity?.setProgressVisibility(false)
            is BaseEvent.ShowProgressWithText -> settingsActivity?.showProgress(event.message)
            is BaseEvent.TaskStarted -> {
                settingsActivity?.setProgressVisibility(true, event.titleResId?.let { getString(it) })
            }
            BaseEvent.TaskFinished -> settingsActivity?.setProgressVisibility(false)
            BaseEvent.ShowMoreInLogs -> settingsActivity?.showSnackMoreInLogs()
            else -> {}
        }
    }

    /**
     * Обработчик изменения настроек.
     * Чтобы работало нужно переопределить onResume() и onPause()
     * и дописать register/unregister настроек.
     * @param sharedPreferences
     * @param key
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {}

    fun onSharedPreferenceChanged(key: String) {
        onSharedPreferenceChanged(baseViewModel.settingsManager.settings, key)
    }

    override fun onResume() {
        super.onResume()
        baseViewModel.settingsManager.settings?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.settingsManager.settings?.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * Деактивация опции, если версия приложения Free.
     * @param pref
     */
    protected fun Preference.disableIfFree() {
        if (baseViewModel.buildInfoProvider.isFullVersion()) {
            isEnabled = true
        } else {
            isEnabled = false
            // принудительно отключаем
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                baseViewModel.showMessage(R.string.title_available_in_pro)
                true
            }
            dependency = null
        }
    }

    protected fun updateSummary(@StringRes keyStringRes: Int, value: String?) {
        updateSummary(getString(keyStringRes), value)
    }

    protected fun updateSummary(key: String, value: String?) {
        if (!value.isNullOrBlank()) {
            findPreference<Preference>(key)?.summary = value
        }
    }

    protected fun updateSummary(@StringRes keyStringRes: Int, value: String?, defValue: String) {
        updateSummary(getString(keyStringRes), value, defValue)
    }

    protected fun updateSummary(key: String, value: String?, defValue: String) {
        findPreference<Preference>(key)?.summary = if (!value.isNullOrBlank()) value else defValue
    }

    protected fun updateSummaryIfContains(@StringRes keyStringRes: Int, value: String?) {
        if (baseViewModel.settingsManager.isContains(keyStringRes)) {
            updateSummary(keyStringRes, value)
        }
    }

    protected fun refreshPreferences() {
        preferenceScreen = null
        addPreferencesFromResource(R.xml.prefs)
    }

    protected fun updateOptionsMenu() {
        settingsActivity?.updateOptionsMenu()
    }

    fun setTitle(titleResId: Int, subtitle: String? = null) {
        settingsActivity?.let {
            it.setTitle(titleResId)
            it.setSubtitle(subtitle)
        }
    }

}
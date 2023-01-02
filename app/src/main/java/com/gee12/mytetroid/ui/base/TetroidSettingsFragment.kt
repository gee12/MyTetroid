package com.gee12.mytetroid.ui.base

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.ui.settings.CommonSettingsViewModel
import com.gee12.mytetroid.ui.TetroidMessage
import kotlinx.coroutines.launch
import lib.folderpicker.FolderPicker
import org.koin.android.ext.android.inject

open class TetroidSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    protected open val baseViewModel: CommonSettingsViewModel by inject()

    protected val application: Application
        get() = requireContext().applicationContext as Application

    private val settingsActivity: TetroidSettingsActivity?
        get() = activity as TetroidSettingsActivity?

    protected val optionsMenu: Menu?
        get() = settingsActivity?.optionsMenu


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    protected open fun initViewModel() {
        lifecycleScope.launch {
            baseViewModel.messageEventFlow.collect { message -> showMessage(message) }
        }
        lifecycleScope.launch {
            baseViewModel.eventFlow.collect { event -> onViewEvent(event) }
        }
    }

    open fun onViewEvent(event: BaseEvent) {
        when (event) {
            is BaseEvent.ShowProgress -> settingsActivity?.setProgressVisibility(event.isVisible)
            is BaseEvent.ShowProgressText -> settingsActivity?.showProgress(event.message)
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
        onSharedPreferenceChanged(baseViewModel.commonSettingsProvider.settings, key)
    }

    override fun onResume() {
        super.onResume()
        baseViewModel.commonSettingsProvider.settings?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.commonSettingsProvider.settings?.unregisterOnSharedPreferenceChangeListener(this)
    }

    protected fun openFolderPicker(title: String?, location: String, requestCode: Int) {
        val path = location.ifBlank { baseViewModel.getLastFolderPathOrDefault(true) }
        val intent = Intent(context, FolderPicker::class.java)
        intent.putExtra(FolderPicker.EXTRA_TITLE, title)
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path)
//        Intent intent = new Intent(getContext(), FolderChooser.class);
//        intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
//        intent.putExtra(Constants.INITIAL_DIRECTORY, path);
        requireActivity().startActivityForResult(intent, requestCode)
    }

    protected fun checkPermission(requestCode: Int): Boolean {
        return baseViewModel.checkWriteExtStoragePermission(requireActivity(), requestCode)
    }

    /**
     * Деактивация опции, если версия приложения Free.
     * @param pref
     */
    protected fun disableIfFree(pref: Preference) {
        if (baseViewModel.buildInfoProvider.isFullVersion()) {
            pref.isEnabled = true
        } else {
            pref.isEnabled = false
            // принудительно отключаем
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                baseViewModel.showMessage(R.string.title_available_in_pro)
                true
            }
            pref.dependency = null
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
        if (baseViewModel.commonSettingsProvider.isContains(keyStringRes)) {
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
        (activity as? TetroidSettingsActivity)?.let {
            it.setTitle(titleResId)
            it.setSubTitle(subtitle)
        }
    }

    private fun showMessage(message: Message) {
        TetroidMessage.show(activity, message)
    }

}
package com.gee12.mytetroid.views.fragments.settings

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import lib.folderpicker.FolderPicker
import com.gee12.mytetroid.App
import com.gee12.mytetroid.interactors.PermissionInteractor
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.viewmodels.CommonSettingsViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.views.TetroidMessage
import com.gee12.mytetroid.views.activities.TetroidSettingsActivity

open class TetroidSettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    protected open lateinit var baseViewModel: CommonSettingsViewModel
    protected open lateinit var permissionInteractor: PermissionInteractor

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
        baseViewModel = ViewModelProvider(this, TetroidViewModelFactory(requireActivity().application, false))
            .get(CommonSettingsViewModel::class.java)
        baseViewModel.messageObservable.observe(requireActivity(), { TetroidMessage.show(activity, it) })
        baseViewModel.viewEvent.observe(requireActivity(), { (event, data) -> onViewEvent(event, data) })

        permissionInteractor = PermissionInteractor(baseViewModel.logger)
    }

    open fun onViewEvent(event: Constants.ViewEvents, data: Any?) {
        when (event) {
            Constants.ViewEvents.ShowProgress -> settingsActivity?.setProgressVisibility(data as? Boolean ?: false)
            Constants.ViewEvents.ShowProgressText -> settingsActivity?.setProgressText(data as? String)
            Constants.ViewEvents.TaskStarted -> settingsActivity?.setProgressVisibility(true, data as String)
            Constants.ViewEvents.TaskFinished -> settingsActivity?.setProgressVisibility(false)
            Constants.ViewEvents.ShowMoreInLogs -> settingsActivity?.showSnackMoreInLogs()
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
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {}

    fun onSharedPreferenceChanged(key: String) {
        onSharedPreferenceChanged(CommonSettings.getSettings(context), key)
    }

    override fun onResume() {
        super.onResume()
        CommonSettings.getSettings(context).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        CommonSettings.getSettings(context).unregisterOnSharedPreferenceChangeListener(this)
    }

    protected fun openFolderPicker(title: String?, location: String, requestCode: Int) {
        val path = if (location.isNotBlank()) location
            else baseViewModel.getLastFolderPathOrDefault(true)
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
        if (App.isFullVersion()) {
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
        if (CommonSettings.isContains(context, keyStringRes)) {
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

}
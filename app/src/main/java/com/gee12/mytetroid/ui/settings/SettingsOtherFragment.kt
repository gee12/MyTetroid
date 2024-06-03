package com.gee12.mytetroid.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment
import com.gee12.mytetroid.ui.base.views.prefs.DisabledCheckBoxPreference
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.history.SelectHistoryWriteModeDialog

class SettingsOtherFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_other, rootKey)
        requireActivity().setTitle(R.string.pref_category_history_and_logs)

        findPreference<DisabledCheckBoxPreference>(getString(R.string.pref_key_is_write_history))?.also {
            it.disableIfFree()
            it.isChecked = settingsManager.isWriteHistory() && buildInfoProvider.isFullVersion()
        }

        findPreference<Preference>(getString(R.string.pref_key_history_write_mode))?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                SelectHistoryWriteModeDialog(
                    settingsManager = settingsManager,
                    resourcesProvider = resourcesProvider,
                ).showIfPossible(parentFragmentManager)
                true
            }
        }

        findPreference<Preference>(getString(R.string.pref_key_clear_search_history))?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AskDialogs.showYesDialog(
                    context = requireContext(),
                    messageResId = R.string.ask_clear_search_history,
                    onApply = {
                        baseViewModel.clearHistory()
                    },
                )
                true
            }
        }

        findPreference<Preference>(getString(R.string.pref_key_log_path))?.also {
            it.isCopyingEnabled = true
        }

        updateHistoryMaxSizeSummary()
        updateHistoryWriteModeSummary()
        updateSummary(R.string.pref_key_log_path, appPathProvider.getPathToLogsFolder().fullPath)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.pref_key_history_max_size) -> {
                updateHistoryMaxSizeSummary()
            }
            getString(R.string.pref_key_history_write_mode) -> {
                updateHistoryWriteModeSummary()
            }
        }
    }

    private fun updateHistoryMaxSizeSummary() {
        settingsManager.getHistoryMaxSize().also { maxSize ->
            resourcesProvider.getString(R.string.pref_title_history_max_size_summ_mask, maxSize).also {
                updateSummary(R.string.pref_key_history_max_size, it)
            }
        }
    }

    private fun updateHistoryWriteModeSummary() {
        settingsManager.getHistoryWriteMode().getTitle(resourcesProvider).also { modeTitle ->
            resourcesProvider.getString(R.string.pref_title_history_write_mode_summ_mask, modeTitle).also {
                updateSummary(R.string.pref_key_history_write_mode, it)
            }
        }
    }

}
package com.gee12.mytetroid.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.provider.TetroidSuggestionProvider
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment
import com.gee12.mytetroid.ui.dialogs.AskDialogs

class SettingsOtherFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_other, rootKey)
        requireActivity().setTitle(R.string.pref_category_other)

        findPreference<Preference>(getString(R.string.pref_key_clear_search_history))?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AskDialogs.showYesDialog(
                    context = requireContext(),
                    messageResId = R.string.ask_clear_search_history,
                    onApply = {
                        TetroidSuggestionProvider.clearHistory(context)
                        CommonSettings.clearSearchOptions(context)
                        baseViewModel.log(R.string.title_search_history_cleared, true)
                    },
                )
                true
            }
        }

        findPreference<Preference>(getString(R.string.pref_key_log_path))?.also {
            it.isCopyingEnabled = true
        }

        findPreference<Preference>(getString(R.string.pref_key_clear_search_history))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AskDialogs.showYesDialog(
                context = requireContext(),
                messageResId = R.string.ask_clear_search_history,
                onApply = {
                    TetroidSuggestionProvider.clearHistory(context)
                    CommonSettings.clearSearchOptions(context)
                    baseViewModel.log(R.string.title_search_history_cleared, true)
                },
            )
            true
        }

        updateSummary(R.string.pref_key_log_path, baseViewModel.appPathProvider.getPathToLogsFolder().fullPath)
    }

}
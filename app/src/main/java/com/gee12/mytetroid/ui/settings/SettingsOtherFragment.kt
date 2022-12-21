package com.gee12.mytetroid.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.TetroidSuggestionProvider
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment
import lib.folderpicker.FolderPicker

class SettingsOtherFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_other, rootKey)
        requireActivity().setTitle(R.string.pref_category_other)

        findPreference<Preference>(getString(R.string.pref_key_log_path))
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (!checkPermission(Constants.REQUEST_CODE_OPEN_LOG_PATH)) return@OnPreferenceClickListener true
            selectLogsFolder()
            true
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
        updateSummary(R.string.pref_key_log_path, CommonSettings.getLogPath(context))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_key_is_write_log)) {
            // меняем флаг
            baseViewModel.logger.init(
                path = CommonSettings.getLogPath(context).orEmpty(),
                isWriteToFile = CommonSettings.isWriteLogToFile(context)
            )
        }
    }

    fun onRequestPermissionsResult(permGranted: Boolean, requestCode: Int) {
        if (permGranted) {
            baseViewModel.log(R.string.log_write_ext_storage_perm_granted)
            when (requestCode) {
                Constants.REQUEST_CODE_OPEN_LOG_PATH -> selectLogsFolder()
            }
        } else {
            baseViewModel.logWarning(R.string.log_missing_write_ext_storage_permissions, true)
        }
    }

    fun onResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode != Activity.RESULT_OK) return

        val folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA)
        if (requestCode == Constants.REQUEST_CODE_OPEN_LOG_PATH && folderPath != null) {
            CommonSettings.setLogPath(context, folderPath)
            CommonSettings.setLastChoosedFolder(context, folderPath)
            baseViewModel.logger.setLogPath(folderPath)
            updateSummary(R.string.pref_key_log_path, folderPath)
        }
    }

    private fun selectLogsFolder() {
        openFolderPicker(
            getString(R.string.pref_log_path),
            CommonSettings.getLogPath(context),
            Constants.REQUEST_CODE_OPEN_LOG_PATH
        )
    }
}
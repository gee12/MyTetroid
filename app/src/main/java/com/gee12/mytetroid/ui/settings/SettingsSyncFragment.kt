package com.gee12.mytetroid.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment

class SettingsSyncFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_sync, rootKey)
        requireActivity().setTitle(R.string.pref_category_sync)

        updateSyncAppSummary()
        updateCommandSummary()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_key_app_for_sync)) {
            updateSyncAppSummary()
            updateCommandSummary()
        } else if (key == getString(R.string.pref_key_sync_command)) {
            updateCommandSummary()
        }
    }

    private fun updateSyncAppSummary() {
        updateSummary(
            R.string.pref_key_app_for_sync, CommonSettings.getSyncAppNameDef(context),
            getString(R.string.pref_app_for_sync_summ)
        )
    }

    private fun updateCommandSummary() {
        updateSummary(
            R.string.pref_key_sync_command, CommonSettings.getSyncCommandDef(context),
            getCommandSummBySyncApp()
        )
    }

    private fun getCommandSummBySyncApp(): String {
        val app = CommonSettings.getSyncAppNameDef(requireContext())
        val index = resources.getStringArray(R.array.sync_apps).indexOf(app)
        return resources.getStringArray(R.array.sync_apps_commands_common).getOrElse(index) {
            getString(R.string.pref_sync_command_summ)
        }
    }
}
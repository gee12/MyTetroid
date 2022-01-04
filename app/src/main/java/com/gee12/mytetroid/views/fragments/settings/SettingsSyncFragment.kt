package com.gee12.mytetroid.views.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings

class SettingsSyncFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_sync, rootKey)
        requireActivity().setTitle(R.string.pref_category_sync)

        updateSummary(R.string.pref_key_app_for_sync, CommonSettings.getSyncAppNameDef(context))
        updateSummary(
            R.string.pref_key_sync_command, CommonSettings.getSyncCommandDef(context),
            getString(R.string.pref_sync_command_summ)
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_key_app_for_sync)) {
            updateSummary(R.string.pref_key_app_for_sync, CommonSettings.getSyncAppNameDef(context))
        } else if (key == getString(R.string.pref_key_sync_command)) {
            updateSummary(
                R.string.pref_key_sync_command, CommonSettings.getSyncCommandDef(context),
                getString(R.string.pref_sync_command_summ)
            )
        }
    }
}
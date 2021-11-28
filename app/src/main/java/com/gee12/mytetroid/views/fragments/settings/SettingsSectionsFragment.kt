package com.gee12.mytetroid.views.fragments.settings

import android.os.Bundle
import com.gee12.mytetroid.R

/**
 * Фрагмент с разделами общих настроек приложения.
 */
class SettingsSectionsFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs, rootKey)
        requireActivity().setTitle(R.string.title_settings)
    }
}
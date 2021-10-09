package com.gee12.mytetroid.views.fragments.settings

import android.os.Bundle
import com.gee12.mytetroid.R

class SettingsFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs, rootKey)
        requireActivity().setTitle(R.string.title_settings)
    }
}
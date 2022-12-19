package com.gee12.mytetroid.ui.settings

import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment

class SettingsHandlingFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_handling, rootKey)
        requireActivity().setTitle(R.string.pref_category_handling)
    }
}
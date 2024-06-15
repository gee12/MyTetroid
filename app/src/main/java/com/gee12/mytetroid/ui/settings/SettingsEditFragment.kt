package com.gee12.mytetroid.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.model.enums.ImagesSaveMode
import com.gee12.mytetroid.ui.base.TetroidSettingsFragment
import com.gee12.mytetroid.ui.dialogs.image.ImagesSaveModeDialog

class SettingsEditFragment : TetroidSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.prefs_edit, rootKey)
        requireActivity().setTitle(R.string.pref_category_edit)

        findPreference<Preference>(CommonSettingsManager.Key.IMAGES_SAVE_MODE)?.also {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                ImagesSaveModeDialog(
                    settingsManager = settingsManager,
                    resourcesProvider = resourcesProvider,
                ).showIfPossible(parentFragmentManager)
                true
            }
        }

        updateImagesSaveModeSummary()
        updateImagesSaveModeQuality()
        updateImagesSaveModeIsEnabled()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            CommonSettingsManager.Key.IMAGES_SAVE_MODE -> {
                updateImagesSaveModeSummary()
                updateImagesSaveModeIsEnabled()
            }
            CommonSettingsManager.Key.IMAGES_SAVE_QUALITY -> {
                updateImagesSaveModeQuality()
            }
        }
    }

    private fun updateImagesSaveModeSummary() {
        settingsManager.getImagesSaveMode().also { mode ->
            updateSummary(CommonSettingsManager.Key.IMAGES_SAVE_MODE, mode.getTitle(resourcesProvider))
        }
    }

    private fun updateImagesSaveModeQuality() {
        settingsManager.getImagesSaveQuality().also { quality ->
            updateSummary(CommonSettingsManager.Key.IMAGES_SAVE_QUALITY, quality.toString())
        }
    }

    private fun updateImagesSaveModeIsEnabled() {
        findPreference<Preference>(CommonSettingsManager.Key.IMAGES_SAVE_QUALITY)?.also {
            it.isEnabled = settingsManager.getImagesSaveMode() == ImagesSaveMode.CONVERT_TO_JPG
        }
    }

}
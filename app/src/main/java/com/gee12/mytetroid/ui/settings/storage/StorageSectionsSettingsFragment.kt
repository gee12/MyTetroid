package com.gee12.mytetroid.ui.settings.storage

import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.base.TetroidStorageSettingsFragment

/**
 * Фрагмент с разделами настроек хранилища.
 */
class StorageSectionsSettingsFragment : TetroidStorageSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.storage_prefs, rootKey)
    }

    override fun onStorageFoundInBase(storage: TetroidStorage) {
        setTitle(R.string.title_storage_settings, storage.name)
    }

    override fun onResume() {
        // меняем заголовок обратно при выходе из дочернего фрагмента
        setTitle(R.string.title_storage_settings, viewModel.storage?.name)
        super.onResume()
    }

    override fun onUpdateStorageFieldEvent(key: String, value: String) {
        when (key) {
            // основное
            getString(R.string.pref_key_storage_path) -> {
                updateSummary(key, value)
            }
        }
    }

}
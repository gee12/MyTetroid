package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidStorage

class StorageSyncSettingsFragment : TetroidStorageSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onStorageInited(storage: TetroidStorage) {
        setTitle(R.string.title_storage_sync, storage.name)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_sync, null)

        // добавляем подписи, если значения установлены
        updateSummary(R.string.pref_key_app_for_sync, viewModel.getSyncAppName())
        updateSummary(
            R.string.pref_key_sync_command, viewModel.getSyncCommand(),
            getString(R.string.pref_sync_command_summ)
        )
    }

    override fun onUpdateStorageFieldEvent(key: String, value: String) {
        when (key) {
            getString(R.string.pref_key_app_for_sync) -> updateSummary(key, value)
            getString(R.string.pref_key_sync_command) -> updateSummary(key, value,
                getString(R.string.pref_sync_command_summ))
        }
    }

}
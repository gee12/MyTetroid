package com.gee12.mytetroid.ui.settings.storage

import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.base.TetroidStorageSettingsFragment

class StorageSyncSettingsFragment : TetroidStorageSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
    }

    override fun onStorageFoundInBase(storage: TetroidStorage) {
        setTitle(R.string.title_storage_sync, storage.name)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_sync, null)

        // добавляем подписи, если значения установлены
        updateSyncAppSummary()
        updateCommandSummary()
    }

    override fun onUpdateStorageFieldEvent(key: String, value: String) {
        when (key) {
            getString(R.string.pref_key_app_for_sync) -> {
                updateSummary(key, value, getString(R.string.pref_app_for_sync_summ_storage))
                updateCommandSummary()
            }
            getString(R.string.pref_key_sync_command) -> updateSummary(key, value, getCommandSummBySyncApp())
        }
    }

    private fun updateSyncAppSummary() {
        updateSummary(
            R.string.pref_key_app_for_sync, viewModel.getStorageSyncAppName(),
            getString(R.string.pref_app_for_sync_summ_storage)
        )
    }

    private fun updateCommandSummary() {
        updateSummary(
            R.string.pref_key_sync_command, viewModel.getStorageSyncCommand(),
            getCommandSummBySyncApp()
        )
    }

    private fun getCommandSummBySyncApp(): String {
        val app = viewModel.getStorageSyncAppName()
        val index = resources.getStringArray(R.array.sync_apps).indexOf(app)
        return resources.getStringArray(R.array.sync_apps_commands_for_storage).getOrElse(index) {
            getString(R.string.pref_sync_command_summ_storage)
        }
    }
}
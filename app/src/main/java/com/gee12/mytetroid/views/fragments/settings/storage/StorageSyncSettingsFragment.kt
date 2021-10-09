package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.R
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment

class StorageSyncSettingsFragment : TetroidSettingsFragment() {

    private lateinit var viewModel: StorageSettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        viewModel = ViewModelProvider(requireActivity(), TetroidViewModelFactory(application))
            .get(StorageSettingsViewModel::class.java)

        // устанавливаем preferenceDataStore после onCreate(), но перед setPreferencesFromResource()
        preferenceManager?.preferenceDataStore = viewModel.prefsDataStore

        setPreferencesFromResource(R.xml.storage_prefs_sync, rootKey)
        setTitle(R.string.pref_category_sync, viewModel.getStorageName())

        // добавляем подписи, если значения установлены
        updateSummary(R.string.pref_key_app_for_sync, viewModel.getSyncAppName())
        updateSummary(
            R.string.pref_key_sync_command, viewModel.getSyncCommand(),
            getString(R.string.pref_sync_command_summ)
        )

        // can't access viewLifecycleOwner when getView() is null yet
        viewModel.updateStorageField.observe(this, { pair ->
            val key = pair.first
            val value = pair.second.toString()
            when (key) {
                getString(R.string.pref_key_app_for_sync) -> updateSummary(key, value)
                getString(R.string.pref_key_sync_command) -> updateSummary(key, value,
                    getString(R.string.pref_sync_command_summ))
            }
        })
    }
}
package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import android.view.View
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants.StorageEvents
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel
import com.gee12.mytetroid.views.activities.StorageSettingsActivity
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment
import java.lang.Exception

open class TetroidStorageSettingsFragment : TetroidSettingsFragment() {

    private val settingsActivity: StorageSettingsActivity?
        get() = activity as StorageSettingsActivity?

    protected val viewModel: StorageSettingsViewModel
        get() = settingsActivity?.viewModel ?: throw Exception("Not inited StorageSettingsViewModel")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsActivity?.getStorageId()?.let { storageId ->
            viewModel.startInitStorageFromBase(storageId)
        } ?: run {
            viewModel.logError(R.string.log_not_transferred_storage_id)
        }
    }

    open fun onStorageEvent(event: StorageEvents, data: Any?) {
    }

    open fun onStorageFoundInBase(storage: TetroidStorage) {
    }

    open fun onStorageInited(storage: TetroidStorage) {
    }

    open fun onStorageInitFailed() {
    }

    open fun onUpdateStorageFieldEvent(key: String, value: String) {
    }

    /**
     * Чтобы заблокировать выход, нужно вернуть true.
     */
    open fun onBackPressed(): Boolean {
        return false
    }

}
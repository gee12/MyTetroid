package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.views.TetroidMessage
import com.gee12.mytetroid.views.activities.StorageSettingsActivity
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment

open class TetroidStorageSettingsFragment : TetroidSettingsFragment() {

    protected open lateinit var viewModel: StorageViewModel

    private val settingsActivity: StorageSettingsActivity?
        get() = activity as StorageSettingsActivity?

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        settingsActivity?.storageId?.let { storageId ->
            viewModel.setStorageFromBase(storageId)
        } ?: run {
            viewModel.logError(R.string.log_not_transferred_storage_id)
        }
    }

    override fun initViewModel() {
        super.initViewModel()
        viewModel = ViewModelProvider(this, TetroidViewModelFactory(requireActivity().application))
            .get(StorageViewModel::class.java)

        viewModel.messageObservable.observe(requireActivity(), { TetroidMessage.show(requireActivity(), it) })
        viewModel.viewEvent.observe(requireActivity(), { (event, data) -> onViewEvent(event, data) })
        viewModel.storageEvent.observe(requireActivity(), { (event, data) -> onStorageEvent(event, data) })
        viewModel.updateStorageField.observe(requireActivity(), { pair -> onUpdateStorageFieldEvent(pair.first, pair.second.toString()) })
    }

    protected open fun onStorageEvent(event: Constants.StorageEvents, data: Any?) {
        when (event) {
            Constants.StorageEvents.Inited -> onStorageInited(data as TetroidStorage)
            else -> {}
        }
    }

    protected open fun onStorageInited(storage: TetroidStorage) {}

    protected open fun onUpdateStorageFieldEvent(key: String, value: String) {}

}
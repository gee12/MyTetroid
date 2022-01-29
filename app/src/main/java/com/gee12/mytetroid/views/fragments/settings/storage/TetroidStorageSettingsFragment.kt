package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants.StorageEvents
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsActivity?.storageId?.let { storageId ->
            viewModel.initStorageFromBase(storageId)
        } ?: run {
            viewModel.logError(R.string.log_not_transferred_storage_id)
        }
    }

    override fun initViewModel() {
        super.initViewModel()
        viewModel = ViewModelProvider(
            this, TetroidViewModelFactory(
                app = requireActivity().application,
                storageId = settingsActivity?.storageId
            )
        )
            .get(StorageViewModel::class.java)

        viewModel.messageObservable.observe(viewLifecycleOwner, {
            TetroidMessage.show(requireContext(), it)
        })
        viewModel.viewEvent.observe(viewLifecycleOwner, { (event, data) -> onViewEvent(event, data) })
        viewModel.storageEvent.observe(viewLifecycleOwner, { (event, data) -> onStorageEvent(event, data) })
        viewModel.updateStorageField.observe(viewLifecycleOwner, { pair -> onUpdateStorageFieldEvent(pair.first, pair.second.toString()) })
    }

    protected open fun onStorageEvent(event: StorageEvents, data: Any?) {
        when (event) {
            StorageEvents.FoundInBase -> onStorageFoundInBase(data as TetroidStorage)
            StorageEvents.PermissionCheck -> viewModel.checkWriteExtStoragePermission(requireActivity())
            StorageEvents.PermissionGranted -> viewModel.initStorage()
            StorageEvents.Inited -> onStorageInited(data as TetroidStorage)
            StorageEvents.InitFailed -> onStorageInitFailed()
            else -> {}
        }
    }

    protected open fun onStorageFoundInBase(storage: TetroidStorage) {
    }

    protected open fun onStorageInited(storage: TetroidStorage) {
        val storageFilesError = viewModel.checkStorageFilesExistingError()
        setWarningMenuItem(
            isVisible = (storageFilesError != null)
        ) {
            viewModel.showMessage(
                storageFilesError ?: getString(R.string.mes_storage_init_error)
            )
        }
    }

    protected open fun onStorageInitFailed() {
        setWarningMenuItem(
            isVisible = true
        ) {
            viewModel.showMessage(
                viewModel.checkStorageFilesExistingError() ?: getString(R.string.mes_storage_init_error)
            )
        }
    }

    protected open fun onUpdateStorageFieldEvent(key: String, value: String) {
    }

    protected fun setWarningMenuItem(isVisible: Boolean, onClick: (() -> Unit)? = null) {
        optionsMenu?.findItem(R.id.action_error)?.let {
            it.isVisible = isVisible
            it.setOnMenuItemClickListener {
                onClick?.invoke()
                true
            }
        }
        updateOptionsMenu()
    }

    /**
     * Чтобы заблокировать выход, нужно вернуть true.
     */
    open fun onBackPressed(): Boolean {
        return false
    }

}
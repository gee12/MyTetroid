package com.gee12.mytetroid.ui.settings.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidSettingsActivity
import com.gee12.mytetroid.ui.base.TetroidStorageSettingsFragment
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.FullFileStoragePermissionDialog
import com.gee12.mytetroid.ui.storage.StorageEvent

/**
 * Активность для управления настройками хранилища.
 * (замена SettingsManager для параметров хранилища)
 */
class StorageSettingsActivity : TetroidSettingsActivity<StorageSettingsViewModel>() {

    // region Create

    override fun getLayoutResourceId() = R.layout.activity_settings

    override fun getViewModelClazz() = StorageSettingsViewModel::class.java

    override fun isSingleTitle() = false

    fun getStorageId(): Int {
        return intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun createDependencyScope() {
        val currentStorageProvider = ScopeSource.current.scope.get<IStorageProvider>()
        val currentStorageId = currentStorageProvider.storage?.id
        // создавать новый koin scope или использовать существующий current.scope
        scopeSource = if (currentStorageId?.let { it == getStorageId() } == true) {
            ScopeSource.current
        } else {
            ScopeSource.createNew()
        }
    }

    override fun initViewModel() {
        super.initViewModel()

        viewModel.updateStorageField.observe(this) { pair ->
            onUpdateStorageFieldEvent(pair.first, pair.second.toString())
        }

    }

    override fun startDefaultFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, StorageSectionsSettingsFragment())
            .commit()
    }

    // endregion Create

    // region Events

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StorageEvent -> {
                onStorageEvent(event)
            }
            is BaseEvent.ShowProgress -> setProgressVisibility(true)
            is BaseEvent.HideProgress -> setProgressVisibility(false)
            is BaseEvent.ShowProgressWithText -> showProgress(event.message)
            is BaseEvent.TaskStarted -> {
                setProgressVisibility(true, event.titleResId?.let { getString(it) })
            }
            BaseEvent.TaskFinished -> setProgressVisibility(false)
            BaseEvent.ShowMoreInLogs -> showSnackMoreInLogs()
            is BaseEvent.Permission.Check -> {
                if (event.permission is TetroidPermission.FileStorage.Write) {
                    viewModel.checkAndRequestWriteFileStoragePermission(
                        uri = event.permission.uri,
                        requestCode = PermissionRequestCode.OPEN_STORAGE_FOLDER,
                    )
                }
            }
            is BaseEvent.Permission.Granted -> {
                if (event.permission is TetroidPermission.FileStorage.Write) {
                    viewModel.initStorage()
                }
            }
            is BaseEvent.Permission.ShowRequest -> {
                if (event.permission is TetroidPermission.FileStorage
                    && event.requestCode == PermissionRequestCode.OPEN_STORAGE_FOLDER
                ) {
                    showFileStoragePermissionRequest(event.permission, event.requestCode)
                } else {
                    showPermissionRequest(
                        permission = event.permission,
                        requestCallback = event.requestCallback,
                    )
                }
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun onStorageEvent(event: StorageEvent) {
        (currentFragment as? TetroidStorageSettingsFragment)?.onStorageEvent(event)

        when (event) {
            is StorageEvent.FoundInBase -> onStorageFoundInBase(event.storage)
            is StorageEvent.Inited -> onStorageInited(event.storage)
            is StorageEvent.InitFailed -> onStorageInitFailed()
            else -> Unit
        }
    }

    // endregion Events

    // region Storage

    private fun onStorageFoundInBase(storage: TetroidStorage) {
        (currentFragment as? TetroidStorageSettingsFragment)?.onStorageFoundInBase(storage)
    }

    private fun onStorageInited(storage: TetroidStorage) {
        (currentFragment as? TetroidStorageSettingsFragment)?.onStorageInited(storage)

        val storageFilesError = viewModel.checkStorageFilesExistingError()
        setWarningMenuItem(
            isVisible = (storageFilesError != null),
            onClick = {
                viewModel.showMessage(
                    storageFilesError ?: getString(R.string.mes_storage_init_error)
                )
            }
        )
    }

    private fun onStorageInitFailed() {
        (currentFragment as? TetroidStorageSettingsFragment)?.onStorageInitFailed()

        setWarningMenuItem(
            isVisible = true,
            onClick = {
                viewModel.showMessage(
                    viewModel.checkStorageFilesExistingError() ?: getString(R.string.mes_storage_init_error)
                )
            }
        )
    }

    private fun onUpdateStorageFieldEvent(key: String, value: String) {
        (currentFragment as? TetroidStorageSettingsFragment)?.onUpdateStorageFieldEvent(key, value)
    }

    // endregion Storage

    protected fun showPermissionRequest(
        permission: TetroidPermission,
        requestCallback: (() -> Unit)?
    ) {
        // диалог с объяснием зачем нужно разрешение
        AskDialogs.showYesDialog(
            context = this,
            message = permission.getPermissionRequestMessage(resourcesProvider),
            onApply = {
                requestCallback?.invoke()
            },
        )
    }

    protected fun showFileStoragePermissionRequest(
        permission: TetroidPermission.FileStorage,
        requestCode: PermissionRequestCode,
    ) {
        if (viewModel.buildInfoProvider.hasAllFilesAccessVersion()) {
            viewModel.permissionManager.requestWriteExtStoragePermissions(
                activity = this,
                requestCode = requestCode,
                onManualPermissionRequest = { callback ->
                    FullFileStoragePermissionDialog(
                        onSuccess = {
                            callback()
                        },
                        onCancel = {}
                    ).showIfPossibleAndNeeded(supportFragmentManager)
                },
            )
        } else {
            AskDialogs.showOkCancelDialog(
                context = this,
                title = getString(R.string.ask_permission_on_storage_folder_title),
                message = getString(R.string.ask_permission_on_storage_folder_mask, viewModel.storage?.name.orEmpty()),
                isCancelable = false,
                onYes = {
                    requestFileStorageAccess(
                        uri = permission.uri,
                        requestCode = requestCode,
                    )
                },
                onCancel = {}
            )
        }
    }

    private fun setWarningMenuItem(isVisible: Boolean, onClick: (() -> Unit)? = null) {
        optionsMenu?.findItem(R.id.action_error)?.let {
            it.isVisible = isVisible
            it.setOnMenuItemClickListener {
                onClick?.invoke()
                true
            }
        }
        updateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.storage_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onBackPressed() {
        val fragment = currentFragment
        if (fragment is TetroidStorageSettingsFragment) {
            if (!fragment.onBackPressed()) {
                if (fragment is StorageSectionsSettingsFragment) {
                    checkSettingsChanges()
                }
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun checkSettingsChanges() {
        // если настройки хранилища были изменены, добавляем пометку в результат активити
        if (viewModel.isFieldsChanged) {
            val intent = buildIntent {
                if (viewModel.isStoragePathChanged) {
                    putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
                    putExtra(Constants.EXTRA_STORAGE_ID, getStorageId())
                } else {
                    putExtra(Constants.EXTRA_IS_RELOAD_STORAGE_ENTITY, true)
                }
            }
            setResult(Activity.RESULT_OK, intent)
        }
    }

    companion object {

        @JvmStatic
        fun newIntent(context: Context, storage: TetroidStorage): Intent {
            return Intent(context, StorageSettingsActivity::class.java).apply {
                putExtra(Constants.EXTRA_STORAGE_ID, storage.id)
            }
        }
    }

}
package com.gee12.mytetroid.ui.activities

import android.content.Intent
import android.view.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.providers.IStorageProvider
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.activities.StorageSettingsActivity.Companion.newIntent
import com.gee12.mytetroid.ui.activities.StoragesActivity.Companion.start
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.viewmodels.BaseEvent
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent
import lib.folderpicker.FolderPicker

abstract class TetroidStorageActivity<VM : BaseStorageViewModel> : TetroidActivity<VM>() {

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

    fun getStorageId(): Int {
        return intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (scopeSource != ScopeSource.current) {
            scopeSource.scope.close()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StorageEvent -> onStorageEvent(event)
            else -> super.onBaseEvent(event)
        }
    }

    /**
     * Обработчик изменения состояния хранилища.
     * @param event
     * @param data
     */
    protected open fun onStorageEvent(event: StorageEvent) {
        when (event) {
            StorageEvent.NoDefaultStorage -> {
                AskDialogs.showYesNoDialog(
                    context = this,
                    messageResId = R.string.ask_no_set_default_storage,
                    onApply = {
                        showStoragesActivity()
                    },
                    onCancel = {},
                )
            }
            is StorageEvent.Inited -> afterStorageInited()
            is StorageEvent.Loaded -> afterStorageLoaded(event.result)
            StorageEvent.Decrypted -> afterStorageDecrypted(/*data as TetroidNode*/)
            else -> {}
        }
    }

    open fun afterStorageInited() {
    }

    open fun afterStorageLoaded(res: Boolean) {
    }

    open fun afterStorageDecrypted() {
    }

    // region FileFolderPicker

    fun openFilePicker() {
        openFileFolderPicker(true)
    }

    fun openFolderPicker() {
        openFileFolderPicker(false)
    }

    /**
     * Открытие активности для выбора файла или каталога в файловой системе.
     */
    fun openFileFolderPicker(isPickFile: Boolean) {
        val intent = Intent(this, FolderPicker::class.java)
        intent.putExtra(
            FolderPicker.EXTRA_TITLE,
            if (isPickFile) getString(R.string.title_select_file_to_upload) else getString(R.string.title_save_file_to)
        )
        intent.putExtra(FolderPicker.EXTRA_LOCATION, viewModel.getLastFolderPathOrDefault(false))
        intent.putExtra(FolderPicker.EXTRA_PICK_FILES, isPickFile)
        startActivityForResult(intent, if (isPickFile) Constants.REQUEST_CODE_FILE_PICKER else Constants.REQUEST_CODE_FOLDER_PICKER)
    }

    // endregion FileFolderPicker

    /**
     * Обработчик выбора пунктов системного меню.
     * @param item
     * @return
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    /**
     * Обработчик результата активити.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Обработчик запроса разрешения.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    protected fun showStoragesActivity() {
        start(this, Constants.REQUEST_CODE_STORAGES_ACTIVITY)
    }

    protected fun showStorageSettingsActivity(storage: TetroidStorage?) {
        if (storage == null) return
        startActivityForResult(newIntent(this, storage), Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY)
    }

}
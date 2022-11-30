package com.gee12.mytetroid.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.activities.StorageSettingsActivity.Companion.newIntent
import com.gee12.mytetroid.ui.activities.StoragesActivity.Companion.start
import com.gee12.mytetroid.ui.dialogs.storage.StorageDialogs.askForDefaultStorageNotSpecified
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent
import com.gee12.mytetroid.viewmodels.VMEvent
import kotlinx.coroutines.launch
import lib.folderpicker.FolderPicker

abstract class TetroidStorageActivity<VM : BaseStorageViewModel> : TetroidActivity<VM>() {


    protected open fun getStorageId(): Int? = null

    override fun initViewModel() {
        super.initViewModel()

        lifecycleScope.launch {
            viewModel.storageEventFlow.collect { event -> onStorageEvent(event) }
        }
        lifecycleScope.launch {
            viewModel.objectEventFlow.collect { event -> onObjectEvent(event) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
    }

    /**
     * Обработчик изменения состояния хранилища.
     * @param event
     * @param data
     */
    protected open fun onStorageEvent(event: StorageEvent) {
        when (event) {
            StorageEvent.NoDefaultStorage -> askForDefaultStorageNotSpecified(this) { showStoragesActivity() }
            is StorageEvent.Inited -> afterStorageInited()
            is StorageEvent.Loaded -> afterStorageLoaded(event.result)
            StorageEvent.Decrypted -> afterStorageDecrypted(/*data as TetroidNode*/)
            else -> {}
        }
    }

    /**
     * Обработчик изменения состояния объекта.
     * @param event
     * @param data
     */
    protected open fun onObjectEvent(event: VMEvent) {
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
package com.gee12.mytetroid.ui.storages

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.common.extensions.showForcedWithIcons
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.FullFileStoragePermissionDialog
import com.gee12.mytetroid.ui.dialogs.storage.DeleteStorageDialog
import com.gee12.mytetroid.ui.dialogs.storage.StorageFieldsDialog
import com.gee12.mytetroid.ui.settings.storage.StorageSettingsActivity
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.ui.storage.info.StorageInfoActivity
import com.github.clans.fab.FloatingActionMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.scope.get


class StoragesActivity : TetroidActivity<StoragesViewModel>() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StoragesAdapter
    private var storageDialog: StorageFieldsDialog? = null

    override fun getLayoutResourceId() = R.layout.activity_storages

    override fun getViewModelClazz() = StoragesViewModel::class.java


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recyclerView = findViewById(R.id.recycle_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(
            context = recyclerView.context,
            orientation = DividerItemDecoration.VERTICAL,
            spaceBetweenRes = R.dimen.recycler_view_item_top_spacing
        ))
        adapter = StoragesAdapter(
            context = this,
            currentStorageId = viewModel.getCurrentStorageId()
        )
        adapter.onItemClickListener = { item, _ ->
            selectStorage(item)
        }
        adapter.onItemLongClickListener = { item, view ->
            showStoragePopupMenu(view, item)
            true
        }
        adapter.onItemMenuClickListener = { item, view ->
            showStoragePopupMenu(view, item)
        }
        recyclerView.adapter = adapter

        val fabAddStorage = findViewById<FloatingActionMenu>(R.id.fab_add_storage)
        fabAddStorage.setClosedOnTouchOutside(true)
        findViewById<com.github.clans.fab.FloatingActionButton>(R.id.fab_add_existing_storage).also {
            it.setOnClickListener {
                fabAddStorage.close(true)
                viewModel.addNewStorage(isNew = false)
            }
        }
        findViewById<com.github.clans.fab.FloatingActionButton>(R.id.fab_create_new_storage).also {
            it.setOnClickListener {
                fabAddStorage.close(true)
                viewModel.addNewStorage(isNew = true)

            }
        }

        loadStorages()
    }

    override fun initViewModel() {
        super.initViewModel()

        viewModel.storages.observe(this) { list -> showStoragesList(list) }
        viewModel.checkStoragesFilesExisting = true
    }

    override fun onUiCreated() {}

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StoragesEvent -> {
                onStoragesEvent(event)
            }
            is BaseEvent.Permission.Granted -> {
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun onStoragesEvent(event: StoragesEvent) {
        when (event) {
            is StoragesEvent.ShowAddStorageDialog -> {
                showStorageDialog(storageId = null, isNew = event.isNew)
            }
            is StoragesEvent.SetStorageFolder -> {
                storageDialog?.setStorageFolder(
                    folder = event.folder,
                )
            }
            is StoragesEvent.AddedNewStorage -> {
                checkPermissionsAndInitAddedStorage(event.storage)
            }
        }
    }

    private fun loadStorages() {
        viewModel.loadStorages()
    }

    private fun showStoragesList(list: List<TetroidStorage>) {
        adapter.submitList(list)
        findViewById<TextView>(R.id.text_view_empty_storages)?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun selectStorage(storage: TetroidStorage) {
        AskDialogs.showYesDialog(
            context = this,
            message = getString(R.string.ask_load_storage_mask, storage.name),
            onApply = {
                finishWithLoadStorageResult(storage)
            },
        )
    }

    private fun finishWithLoadStorageResult(storage: TetroidStorage) {
        val intent = buildIntent {
            putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
            putExtra(Constants.EXTRA_STORAGE_ID, storage.id)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setCloseStorageResult(storage: TetroidStorage) {
        val intent = buildIntent {
            putExtra(Constants.EXTRA_IS_CLOSE_STORAGE, true)
            putExtra(Constants.EXTRA_STORAGE_ID, storage.id)
        }
        setResult(RESULT_OK, intent)
    }

    private fun checkPermissionsAndInitAddedStorage(storage: TetroidStorage) {
        val tempKoinScope = ScopeSource.createNew().scope
        val storageViewModel: StorageViewModel = tempKoinScope.get(StorageViewModel::class.java)

        lifecycleScope.launch {
            storageViewModel.eventFlow.collect { event ->
                when (event) {
                    is StorageEvent.InitFailed -> {
                        hideProgress()
                    }
                    is StorageEvent.FilesCreated -> {
                        hideProgress()
                        onStorageFilesCreated(event.storage)
                    }
                    // проверка разрешения перед созданием файлов хранилища
                    is BaseEvent.Permission.Check -> {
                        if (event.permission is TetroidPermission.FileStorage.Write) {
                            storageViewModel.checkAndRequestWriteFileStoragePermission(
                                uri = event.permission.uri,
                                requestCode = PermissionRequestCode.CREATE_STORAGE_FILES,
                            )
                        }
                    }
                    is BaseEvent.Permission.Granted -> {
                        if (event.permission is TetroidPermission.FileStorage.Write) {
                            storageViewModel.initStorage()
                        }
                    }
                    is BaseEvent.Permission.ShowRequest -> {
                        if (event.permission is TetroidPermission.FileStorage
                            && event.requestCode == PermissionRequestCode.CREATE_STORAGE_FILES
                        ) {
                            showFileStoragePermissionRequest(event.permission, event.requestCode, storage)
                        } else {
                            showPermissionRequest(
                                permission = event.permission,
                                requestCallback = event.requestCallback,
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }

        if (storage.isNew) {
            showProgress(R.string.state_storage_files_creating)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            storageViewModel.checkPermissionsAndInitStorage(storage)
        }
    }

    private fun showPermissionRequest(
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

    private fun showFileStoragePermissionRequest(
        permission: TetroidPermission.FileStorage,
        requestCode: PermissionRequestCode,
        storage: TetroidStorage,
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
                message = getString(R.string.ask_permission_on_storage_folder_mask, storage.name),
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

    private fun onStorageFilesCreated(storage: TetroidStorage) {
        loadStorages()

        AskDialogs.showYesNoCancelDialog(
            context = this,
            message = getString(R.string.log_storage_created_mask, storage.name),
            yesResId = R.string.action_load,
            noResId = R.string.action_cancel,
            cancelResId = R.string.action_settings,
            onYes = {
                finishWithLoadStorageResult(storage)
            },
            onNo = {},
            onCancel = {
                showStorageSettings(storage)
            },
        )
    }

    private fun showStorageSettings(storage: TetroidStorage) {
        startActivityForResult(
            StorageSettingsActivity.newIntent(this, storage),
            Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY
        )
    }

    private fun showStorageInfo(storage: TetroidStorage) {
        StorageInfoActivity.start(this, storage.id)
    }

    private fun setDefault(storage: TetroidStorage) {
        viewModel.setDefault(storage)
    }

    private fun deleteStorage(storage: TetroidStorage) {
        val isCurrentStorage = storage.id == viewModel.getCurrentStorageId()
        DeleteStorageDialog(
            storage = storage,
            isCurrentStorage = isCurrentStorage,
            onApply = { withFiles ->
                viewModel.deleteStorage(storage, withFiles, deleteIfAlreadyLoaded = true)
                if (isCurrentStorage) {
                    setCloseStorageResult(storage)
                }
            }
        ).showIfPossibleAndNeeded(supportFragmentManager)
    }

    private fun showStorageDialog(storageId: Int?, isNew: Boolean) {
        if (storageDialog?.hasAlreadyShown(supportFragmentManager) == true) {
            return
        }
        storageDialog = StorageFieldsDialog(
            storageId = storageId,
            isNew = isNew,
            isDefault = (viewModel.storages.value?.isEmpty() == true),
            onApply = { storage ->
                viewModel.addStorage(storage)
            },
            onPickStorageFolder = { currentFolderUri ->
                openFolderPicker(
                    requestCode = if (isNew) {
                        PermissionRequestCode.SELECT_FOLDER_FOR_NEW_STORAGE
                    } else {
                        PermissionRequestCode.CHANGE_STORAGE_FOLDER
                    },
                    initialPath = currentFolderUri?.toString(),
                    forStorageFolder = true,
                    isNeedEmptyFolder = isNew,
                    isNeedCheckFolderWritePermission = true,
                )
            }
        ).apply {
            this.showIfPossibleAndNeeded(supportFragmentManager)
        }
    }

    // region File

    override fun isUseFileStorage() = true

    override fun onFolderSelected(requestCode: Int, folder: DocumentFile) {
        when (PermissionRequestCode.fromCode(requestCode)) {
            PermissionRequestCode.CHANGE_STORAGE_FOLDER,
            PermissionRequestCode.SELECT_FOLDER_FOR_NEW_STORAGE -> {
                viewModel.checkFolderForNewStorage(
                    folder = folder,
                    isNew = (requestCode == PermissionRequestCode.SELECT_FOLDER_FOR_NEW_STORAGE.code),
                )
            }
            else -> Unit
        }
    }

    // endregion File

    /**
     * Обработчик создания системного меню
     * @param menu
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.storages, menu)
        return true
    }

    /**
     * Обработчик выбора пунктов системного меню
     * @param item
     * @return
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {

                return true
            }
            R.id.action_choice_mode -> {
                // TODO: включаем режим множественного выбора (ActionMode) для RecyclerView
//                ActionModeController(R.menu.storage_actions, ActionMode.TYPE_PRIMARY, a).startActionMode(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Отображение контексного меню хранилища.
     * FIXME: Заменить на использование AlertDialog ? (чтобы посередине экрана)
     *
     * @param anchorView
     * @param tag
     */
    @SuppressLint("RestrictedApi")
    private fun showStoragePopupMenu(anchorView: View, storage: TetroidStorage) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.inflate(R.menu.storage_context)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_open -> {
                    selectStorage(storage)
                    true
                }
                R.id.action_storage_settings -> {
                    showStorageSettings(storage)
                    true
                }
                R.id.action_info -> {
                    showStorageInfo(storage)
                    true
                }
                R.id.action_set_default -> {
                    setDefault(storage)
                    true
                }
                R.id.action_delete -> {
                    deleteStorage(storage)
                    true
                }
                else -> false
            }
        }
        (popupMenu.menu as MenuBuilder).showForcedWithIcons(anchorView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY -> {
                loadStorages()
            }
        }
    }

    companion object {

        fun start(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, StoragesActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
package com.gee12.mytetroid.ui.activities

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.viewmodels.BaseEvent
import com.gee12.mytetroid.common.extensions.toApplyCancelResult
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent
import com.gee12.mytetroid.viewmodels.StoragesViewModel
import com.gee12.mytetroid.viewmodels.StoragesViewModel.StoragesEvent
import com.gee12.mytetroid.ui.adapters.StoragesAdapter
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.storage.StorageFieldsDialog
import com.gee12.mytetroid.ui.dialogs.storage.StorageDialogs
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import lib.folderpicker.FolderPicker
import org.koin.java.KoinJavaComponent.get


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
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL))
        adapter = StoragesAdapter(
            context = this,
            currentStorageId = viewModel.getCurrentStorageId()
        )
        adapter.onItemClickListener = View.OnClickListener { v ->
            val itemPosition = recyclerView.getChildLayoutPosition(v!!)
            val item = adapter.getItem(itemPosition)
            selectStorage(item)
        }
        adapter.onItemLongClickListener = View.OnLongClickListener { v ->
            val itemPosition = recyclerView.getChildLayoutPosition(v)
            val item = adapter.getItem(itemPosition)
            showStoragePopupMenu(v, item)
            true
        }
        adapter.onItemMenuClickListener = object : StoragesAdapter.IItemMenuClickListener {
            override fun onClick(itemView: View, anchorView: View) {
                val itemPosition = recyclerView.getChildLayoutPosition(itemView)
                val item = adapter.getItem(itemPosition)
                showStoragePopupMenu(anchorView, item)
            }
        }
        recyclerView.adapter = adapter

        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_storage)
        fabAdd.setOnClickListener { viewModel.addNewStorage(this) }

        loadStorages()
    }

    override fun initViewModel() {
        super.initViewModel()

        viewModel.storages.observe(this) { list -> showStoragesList(list) }
        viewModel.checkStoragesFilesExisting = true
    }

    override fun onUICreated(uiCreated: Boolean) {}

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StoragesEvent -> {
                onStoragesEvent(event)
            }
            is BaseEvent.PermissionGranted -> {
                when (event.requestCode) {
                    Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE -> viewModel.addNewStorage(this)
                    Constants.REQUEST_CODE_PERMISSION_READ_STORAGE -> Unit // ничего не делаем
                }
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun onStoragesEvent(event: StoragesEvent) {
        when (event) {
            StoragesEvent.ShowAddNewStorageDialog -> showStorageDialog(storageId = null)
            is StoragesEvent.AddedNewStorage -> onStorageAdded(event.storage)
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
        AskDialogs.showLoadStorageDialog(this, storage.name) {
            finishWithResult(storage)
        }
    }

    private fun finishWithResult(storage: TetroidStorage) {
        val intent = Intent().apply {
            putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
            putExtra(Constants.EXTRA_STORAGE_ID, storage.id)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun onStorageAdded(storage: TetroidStorage) {
        if (storage.isNew) {
            AskDialogs.showCreateStorageFilesDialog(this, storage.path) {
                createStorageFiles(storage)
            }
        }
    }

    private fun createStorageFiles(storage: TetroidStorage) {
        val storageViewModel: StorageViewModel = get(StorageViewModel::class.java)
        lifecycleScope.launch {
            storageViewModel.eventFlow.collect { event ->
                when (event) {
                    is StorageEvent.FilesCreated -> onStorageFilesCreated(event.storage)
                    // проверка разрешения перед созданием файлов хранилища
                    BaseEvent.PermissionCheck -> storageViewModel.checkWriteExtStoragePermission(this@StoragesActivity)
                    is BaseEvent.PermissionGranted -> storageViewModel.initStorage()
                    else -> Unit
                }
            }
        }

        storageViewModel.startInitStorage(storage)
    }

    private fun onStorageFilesCreated(storage: TetroidStorage) {
        loadStorages()
        AskDialogs.showOpenStorageSettingsDialog(this) {
            showStorageSettings(storage)
        }
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
        AskDialogs.showYesNoDialog(
            this, Dialogs.IApplyResult {
                viewModel.deleteStorage(storage)
            }.toApplyCancelResult(),
            getString(R.string.ask_delete_storage_mask, storage.name)
        )
    }

    private fun showStorageDialog(storageId: Int?) {
        storageDialog = StorageFieldsDialog(
            storageId = storageId,
            isDefault = (viewModel.storages.value?.isEmpty() == true),
            onApply = { storage ->
                viewModel.addStorage(storage)
            },
            onSelectPath = { path ->
                selectStorageFolder(path)
            }
        ).apply {
            showIfPossible(supportFragmentManager)
        }
    }

    private fun selectStorageFolder(path: String) {
        // спрашиваем: создать или выбрать хранилище ?
        StorageDialogs.createStorageSelectionDialog(
            context = this,
            onItemClick = { isNew ->
                openFolderPicker(getString(R.string.title_storage_folder), path, isNew)
            }
        )
    }

    private fun openFolderPicker(title: String?, location: String, isNew: Boolean) {
        val path = location.ifBlank { viewModel.getLastFolderPathOrDefault(forWrite = true) }
        val intent = Intent(this, FolderPicker::class.java)
        intent.putExtra(FolderPicker.EXTRA_TITLE, title)
        intent.putExtra(FolderPicker.EXTRA_LOCATION, path)
        if (isNew) {
            intent.putExtra(FolderPicker.EXTRA_EMPTY_FOLDER, true)
        } else {
            intent.putExtra(FolderPicker.EXTRA_DESCRIPTION, getString(R.string.title_storage_path_desc))
        }
        startActivityForResult(
            intent,
            if (isNew) Constants.REQUEST_CODE_CREATE_STORAGE_PATH else Constants.REQUEST_CODE_OPEN_STORAGE_PATH
        )
    }

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
        setForceShowMenuIcons(anchorView, popupMenu.menu as MenuBuilder)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // активити выбора каталога
        when (requestCode) {
            Constants.REQUEST_CODE_CREATE_STORAGE_PATH,
            Constants.REQUEST_CODE_OPEN_STORAGE_PATH -> {
                val isNew = (requestCode == Constants.REQUEST_CODE_CREATE_STORAGE_PATH)
                val path = data?.getStringExtra(FolderPicker.EXTRA_DATA).orEmpty()
                storageDialog?.setPath(path, isNew)
            }
            Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY -> {
                loadStorages()
            }
        }
    }

    companion object {

        @JvmStatic
        fun start(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, StoragesActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
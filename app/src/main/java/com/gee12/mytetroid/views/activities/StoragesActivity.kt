package com.gee12.mytetroid.views.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.mytetroid.App
import com.gee12.mytetroid.PermissionManager
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.StoragesViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.views.adapters.StoragesAdapter
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.mytetroid.views.dialogs.storage.StorageDialog
import com.gee12.mytetroid.views.dialogs.storage.StorageDialogs
import com.google.android.material.floatingactionbutton.FloatingActionButton
import lib.folderpicker.FolderPicker
import org.jsoup.internal.StringUtil


class StoragesActivity : TetroidActivity<StoragesViewModel>() {

    private var storageViewModel: StorageViewModel? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StoragesAdapter
    private var storageDialog: StorageDialog? = null

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_storages
    }

    override fun getViewModelClazz() = StoragesViewModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recyclerView = findViewById(R.id.recycle_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StoragesAdapter(this, App.CurrentStorageId)
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
        fabAdd.setOnClickListener { addStorage() }

        loadList()
    }

    override fun initViewModel() {
        viewModel = ViewModelProvider(this, TetroidViewModelFactory(application))
            .get(StoragesViewModel::class.java)

        viewModel.storages.observe(this, { list: List<TetroidStorage?> -> adapter.submitList(list) })
        viewModel.storageEvent.observe(this, {
            when (it.state) {
                Constants.StorageEvents.Added -> (it.data as? TetroidStorage)?.let { storage -> onStorageAdded(storage) }
                else -> {}
            }
        })
    }

    override fun onUICreated(uiCreated: Boolean) {}

    fun checkPermission() {
        if (PermissionManager.checkWriteExtStoragePermission(this, Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE)) {
            storageViewModel?.onPermissionChecked()
        }
    }

    fun loadList() {
        viewModel.loadStorages()
    }

    fun selectStorage(storage: TetroidStorage) {
        AskDialogs.showLoadStorageDialog(this, storage.name) { finishWithResult(storage) }
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
            AskDialogs.showCreateNewStorageDialog(this, storage.path) {
                storageViewModel = StorageViewModel(
                    app = application,
//                    logger = viewModel.logger,
                    storagesRepo = null,
                    xmlLoader = null,
                    crypter = null
                )
                storageViewModel!!.storageEvent.observe(this, {
                    when (it.state) {
                        Constants.StorageEvents.PermissionCheck -> checkPermission()
                        Constants.StorageEvents.PermissionChecked -> storageViewModel!!.initStorage()
                        Constants.StorageEvents.FilesCreated -> onStorageFilesCreated(storage)
                        else -> {}
                    }
                })
                storageViewModel!!.startInitStorage(storage)
//                storageViewModel!!.createStorage(storage)
            }
        }
    }

    private fun onStorageFilesCreated(storage: TetroidStorage) {
        AskDialogs.showOpenStorageSettingsDialog(this) { editStorage(storage) }
    }

    private fun addStorage() {
        showStorageDialog(null)
    }

    private fun editStorage(storage: TetroidStorage) {
        startActivityForResult(StorageSettingsActivity.newIntent(this, storage), Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY)
    }

    private fun deleteStorage(storage: TetroidStorage) {
        AskDialogs.showYesNoDialog(this, object : Dialogs.IApplyCancelResult {
            override fun onApply() {
                viewModel.deleteStorage(storage)
            }
            override fun onCancel() {}
        }, R.string.ask_delete_storage_from_list)
    }

    private fun showStorageDialog(storage: TetroidStorage?) {
        storageDialog = StorageDialog(storage, object : StorageDialog.IStorageResult {
            override fun onApply(storage: TetroidStorage) {
                viewModel.addStorage(storage)
            }
            override fun onSelectPath(path: String) {
                selectStorageFolder(path)
            }
        }).apply {
            showIfPossible(supportFragmentManager)
        }
    }

    private fun selectStorageFolder(path: String) {
        // спрашиваем: создать или выбрать хранилище ?
        StorageDialogs.createStorageSelectionDialog(this, object : StorageDialogs.IItemClickListener {
            override fun onItemClick(isNew: Boolean) {
                openFolderPicker(getString(R.string.title_storage_folder), path, isNew)
            }
        })
    }

    private fun openFolderPicker(title: String?, location: String, isNew: Boolean) {
        val path = if (!StringUtil.isBlank(location)) location else viewModel.getLastFolderPathOrDefault(true)
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
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
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

//    fun a(menuItem: MenuItem) {
//        when (menuItem.itemId) {
//            R.id.action_select -> selectStorage(TetroidStorage("test\\test"))
//            R.id.action_edit -> selectStorage(TetroidStorage("test\\test"))
//        }
//    }

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
                R.id.action_edit -> {
                    editStorage(storage)
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
        if (requestCode == Constants.REQUEST_CODE_CREATE_STORAGE_PATH
            || requestCode == Constants.REQUEST_CODE_OPEN_STORAGE_PATH) {
            val isNew = (requestCode == Constants.REQUEST_CODE_CREATE_STORAGE_PATH)
            val path = data?.getStringExtra(FolderPicker.EXTRA_DATA) ?: ""
            storageDialog?.setPath(path, isNew)
        } else if (requestCode == Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY) {
            loadList()
        }
    }

}
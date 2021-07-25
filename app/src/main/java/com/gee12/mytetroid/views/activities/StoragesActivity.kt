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
import com.gee12.mytetroid.PermissionManager
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StoragesViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModelFactory
import com.gee12.mytetroid.views.adapters.StoragesAdapter
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.mytetroid.views.dialogs.StorageDialogs
import com.gee12.mytetroid.views.dialogs.StorageDialogs.StorageDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import lib.folderpicker.FolderPicker
import org.jsoup.internal.StringUtil


class StoragesActivity : TetroidActivity() {

    private lateinit var viewModel: StoragesViewModel
    private lateinit var recyclerView: RecyclerView
    private var storageDialog: StorageDialog? = null

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_storages
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, StorageViewModelFactory(application))
            .get(StoragesViewModel::class.java)

        recyclerView = findViewById(R.id.recycle_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = StoragesAdapter(this)
        adapter.setOnItemClickListener { v: View ->
            val itemPosition = recyclerView.getChildLayoutPosition(v)
            val item = adapter.getItem(itemPosition)
            selectStorage(item)
        }
        adapter.setOnItemLongClickListener { v: View ->
            val itemPosition = recyclerView.getChildLayoutPosition(v)
            val item = adapter.getItem(itemPosition)
            showStoragePopupMenu(v, item)
            true
        }
        adapter.setOnItemMenuClickListener { itemView: View, anchorView: View ->
            val itemPosition = recyclerView.getChildLayoutPosition(itemView)
            val item = adapter.getItem(itemPosition)
            showStoragePopupMenu(anchorView, item)
        }
        recyclerView.adapter = adapter

        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_storage)
        fabAdd.setOnClickListener { addStorage() }

        viewModel.storages.observe(this, { list: List<TetroidStorage?> -> adapter.submitList(list) })
        viewModel.storageEvent.observe(this, {
            when (it.state) {
                Constants.StorageEvents.PermissionCheck -> checkPermission()
                Constants.StorageEvents.PermissionChecked -> {}
                Constants.StorageEvents.Added -> (it.data as TetroidStorage?)?.let { storage -> onStorageAdded(storage) }
                Constants.StorageEvents.FilesCreated -> (it.data as TetroidStorage?)?.let { storage -> onStorageFilesCreated(storage) }
                else -> {}
            }
        })
        loadList()
    }

    override fun onGUICreated() {}

//    override fun loadStorage(folderPath: String) {}

//    override fun createStorage(storagePath: String) {}

    fun checkPermission() {
        if (PermissionManager.checkWriteExtStoragePermission(this, Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE)) {
            viewModel.onPermissionChecked()
        }
    }

    fun loadList() {
        viewModel.loadStorages()
    }

    fun selectStorage(storage: TetroidStorage) {
        AskDialogs.showLoadStorageDialog(this, storage.name) { finishWithResult(storage) }
    }

    private fun finishWithResult(storage: TetroidStorage) {
//        val bundle = Bundle()
//        if (callingActivity != null) {
//            if (bundle != null) {
//                bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode)
//                val intent = Intent(Constants.ACTION_RECORD)
//                intent.putExtras(bundle)
//                setResult(resCode, intent)
//            } else {
//                setResult(resCode)
//            }
//        } else {
//            bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode)
//            ViewUtils.startActivity(this, MainActivity::class.java, bundle)
//        }

        val intent = Intent().apply {
            putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
            putExtra(Constants.EXTRA_STORAGE_ID, storage.id)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun onStorageAdded(storage: TetroidStorage) {
        if (storage.isNew) {
            AskDialogs.showCreateNewStorageDialog(this, storage.path) { viewModel.createStorage(storage) }
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
        storageDialog = StorageDialog(this, storage, object : StorageDialogs.IStorageResult {
            override fun onApply(storage: TetroidStorage) {
                viewModel.addStorage(storage)
            }
            override fun onSelectPath(path: String?) {
                selectStorageFolder(storage?.path ?: "")
            }
        })
    }

    private fun selectStorageFolder(path: String) {
        // спрашиваем: создать или выбрать хранилище ?
        StorageDialogs.createStorageSelectionDialog(this) { isNew: Boolean ->
            openFolderPicker(getString(R.string.title_storage_folder), path, isNew)
        }
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
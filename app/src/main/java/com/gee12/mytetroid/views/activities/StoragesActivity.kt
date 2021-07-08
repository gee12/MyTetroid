package com.gee12.mytetroid.views.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StoragesViewModel
import com.gee12.mytetroid.viewmodels.StoragesViewModelFactory
import com.gee12.mytetroid.views.adapters.StoragesAdapter
import com.gee12.mytetroid.views.dialogs.AskDialogs
import com.gee12.mytetroid.views.dialogs.StorageDialogs
import com.gee12.mytetroid.views.dialogs.StorageDialogs.StorageDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import lib.folderpicker.FolderPicker
import org.jsoup.internal.StringUtil


class StoragesActivity : TetroidActivity() {

    companion object {
        private const val REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY = 1
        private const val REQUEST_CODE_OPEN_STORAGE_PATH = 2
        private const val REQUEST_CODE_CREATE_STORAGE_PATH = 3
    }

    private lateinit var mViewModel: StoragesViewModel
    private lateinit var mRecyclerView: RecyclerView
    private var mStorageDialog: StorageDialog? = null

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_storages
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewModel = ViewModelProvider(this, StoragesViewModelFactory(application))
            .get(StoragesViewModel::class.java)

        mRecyclerView = findViewById(R.id.recycle_view)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = StoragesAdapter(this)
        adapter.setOnItemClickListener { v: View ->
            val itemPosition = mRecyclerView.getChildLayoutPosition(v)
            val item = adapter.getItem(itemPosition)
            selectStorage(item)
        }
        adapter.setOnItemLongClickListener { v: View ->
            val itemPosition = mRecyclerView.getChildLayoutPosition(v)
            val item = adapter.getItem(itemPosition)
            showStoragePopupMenu(v, item)
            true
        }
        adapter.setOnItemMenuClickListener { itemView: View, anchorView: View ->
            val itemPosition = mRecyclerView.getChildLayoutPosition(itemView)
            val item = adapter.getItem(itemPosition)
            showStoragePopupMenu(anchorView, item)
        }
        mRecyclerView.adapter = adapter

        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_storage)
        fabAdd.setOnClickListener { addStorage() }

        mViewModel.storages.observe(this, { list: List<TetroidStorage?> -> adapter.submitList(list) })
        loadList()
    }

    override fun onGUICreated() {}

    override fun loadStorage(folderPath: String) {}

    override fun createStorage(storagePath: String) {}

    fun loadList() {
        mViewModel.loadStorages()
    }

    fun selectStorage(storage: TetroidStorage) {
        // FIXME:
        Toast.makeText(this, storage.name, Toast.LENGTH_LONG).show()
    }

    private fun addStorage() {
        showStorageDialog(null)
    }

    private fun editStorage(storage: TetroidStorage) {
        startActivityForResult(StorageSettingsActivity.newIntent(this, storage), REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY)
    }

    private fun deleteStorage(storage: TetroidStorage) {
        AskDialogs.showYesNoDialog(this, object : Dialogs.IApplyCancelResult {
            override fun onApply() {
                mViewModel.deleteStorage(storage)
            }
            override fun onCancel() {}
        }, R.string.ask_delete_storage_from_list)
    }

    private fun showStorageDialog(storage: TetroidStorage?) {
        mStorageDialog = StorageDialog(this, storage, object : StorageDialogs.IStorageResult {
            override fun onApply(storage: TetroidStorage) {
                mViewModel.addStorage(storage)
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
        val path = if (!StringUtil.isBlank(location)) location else DataManager.getLastFolderPathOrDefault(this, true)
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
            if (isNew) REQUEST_CODE_CREATE_STORAGE_PATH else REQUEST_CODE_OPEN_STORAGE_PATH
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

        if (data != null && requestCode == REQUEST_CODE_CREATE_STORAGE_PATH
            || requestCode == REQUEST_CODE_OPEN_STORAGE_PATH) {
            val path = data!!.getStringExtra(FolderPicker.EXTRA_DATA)
            mStorageDialog?.setPath(path)
        } else if (requestCode == REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY) {
            loadList()
        }
    }
}
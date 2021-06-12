package com.gee12.mytetroid.views.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StoragesViewModel
import com.gee12.mytetroid.viewmodels.StoragesViewModelFactory
import com.gee12.mytetroid.views.adapters.StoragesAdapter
import com.gee12.mytetroid.views.dialogs.StorageDialogs
import com.gee12.mytetroid.views.dialogs.StorageDialogs.StorageDialog
import com.gee12.mytetroid.views.fragments.settings.SettingsFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import lib.folderpicker.FolderPicker
import org.jsoup.internal.StringUtil


class StoragesActivity : TetroidActivity() {

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

            // FIXME:
            Toast.makeText(this, item.name, Toast.LENGTH_LONG).show()
        }
        adapter.setOnItemLongClickListener { v: View ->
            val itemPosition = mRecyclerView.getChildLayoutPosition(v)
            val item = adapter.getItem(itemPosition)
            editStorage(item)
            true
        }
        mRecyclerView.adapter = adapter

        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_storage)
        fabAdd.setOnClickListener { addStorage() }

        mViewModel.storages.observe(this, { list: List<TetroidStorage?> -> adapter.submitList(list) })
        mViewModel.loadStorages()
    }

    override fun onGUICreated() {}

    override fun loadStorage(folderPath: String) {}

    override fun createStorage(storagePath: String) {}

    private fun addStorage() {
        showStorageDialog(null)
    }

    private fun editStorage(storage: TetroidStorage) {
        showStorageDialog(storage)
    }

    private fun showStorageDialog(storage: TetroidStorage?) {
        mStorageDialog = StorageDialog(this, storage, object : StorageDialogs.IStorageResult {
            override fun onApply(path: String?, name: String?, isReadOnly: Boolean) {
                mViewModel.saveStorage(storage, name!!, path!!, isReadOnly)
            }
            override fun onSelectPath(path: String?) {
                selectStorageFolder()
            }
        })
    }

    private fun selectStorageFolder() {
        // спрашиваем: создать или выбрать хранилище ?
        StorageDialogs.createStorageSelectionDialog(this) { isNew: Boolean ->
            openFolderPicker(
                getString(R.string.title_storage_folder),
                SettingsManager.getStoragePath(this), isNew
            )
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
            if (isNew) SettingsFragment.REQUEST_CODE_CREATE_STORAGE_PATH else SettingsFragment.REQUEST_CODE_OPEN_STORAGE_PATH
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SettingsFragment.REQUEST_CODE_CREATE_STORAGE_PATH
            || requestCode == SettingsFragment.REQUEST_CODE_OPEN_STORAGE_PATH) {
            val path = data!!.getStringExtra(FolderPicker.EXTRA_DATA)
            mStorageDialog?.setPath(path)
        }
    }
}
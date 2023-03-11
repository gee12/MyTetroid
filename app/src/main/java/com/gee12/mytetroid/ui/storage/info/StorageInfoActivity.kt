package com.gee12.mytetroid.ui.storage.info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.isVisible
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
import com.gee12.mytetroid.ui.base.TetroidStorageActivity
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.storage.info.StorageInfoViewModel.StorageInfoEvent
import com.gee12.mytetroid.ui.storage.StorageEvent

/**
 * Активность для просмотра информации о хранилище.
 */
class StorageInfoActivity : TetroidStorageActivity<StorageInfoViewModel>() {

    private lateinit var layoutError: View
    private lateinit var textViewError: TextView
    private lateinit var progressSize: View
    private lateinit var progressLastEdit: View
    private lateinit var layoutLoadStorage: View
    private lateinit var buttonLoadStorage: Button

    override fun getLayoutResourceId() = R.layout.activity_storage_info

    override fun getViewModelClazz() = StorageInfoViewModel::class.java


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layoutError = findViewById(R.id.layout_error)
        textViewError = findViewById(R.id.text_view_error)
        progressSize = findViewById(R.id.progress_size)
        progressLastEdit = findViewById(R.id.progress_last_edit)
        layoutLoadStorage = findViewById(R.id.layout_load_storage)
        buttonLoadStorage = findViewById(R.id.button_load_storage)

        // загружаем параметры хранилища из бд
        viewModel.checkPermissionsAndInitStorageById(storageId = getStorageId())
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StorageInfoEvent -> {
                onStorageInfoEvent(event)
            }
            is BaseEvent.Permission.Check -> {
                if (event.permission is TetroidPermission.FileStorage.Write) {
                    viewModel.checkAndRequestWriteFileStoragePermission(
                        file = event.permission.root,
                        requestCode = PermissionRequestCode.OPEN_STORAGE_FOLDER,
                    )
                }
            }
            is BaseEvent.Permission.Granted -> {
                if (event.permission is TetroidPermission.FileStorage.Write) {
                    viewModel.initStorage()
                }
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun onStorageInfoEvent(event: StorageInfoEvent) {
        when (event) {
            StorageInfoEvent.GetStorageFolderSize.InProgress -> {
                progressSize.visibility = View.VISIBLE
            }
            is StorageInfoEvent.GetStorageFolderSize.Failed -> {
                findViewById<TextView>(R.id.text_view_size).text = getString(R.string.title_error)
                progressSize.visibility = View.GONE
            }
            is StorageInfoEvent.GetStorageFolderSize.Success -> {
                findViewById<TextView>(R.id.text_view_size).text = event.size
                progressSize.visibility = View.GONE
            }

            StorageInfoEvent.GetMyTetraXmlLastModifiedDate.InProgress -> {
                progressLastEdit.visibility = View.VISIBLE
            }
            is StorageInfoEvent.GetMyTetraXmlLastModifiedDate.Failed -> {
                findViewById<TextView>(R.id.text_view_last_edit).text = getString(R.string.title_error)
                progressLastEdit.visibility = View.GONE
            }
            is StorageInfoEvent.GetMyTetraXmlLastModifiedDate.Success -> {
                findViewById<TextView>(R.id.text_view_last_edit).text = event.date
                progressLastEdit.visibility = View.GONE
            }
        }
    }

    override fun onStorageEvent(event: StorageEvent) {
        when (event) {
            is StorageEvent.FoundInBase -> onStorageFoundInBase()
            is StorageEvent.Inited -> onStorageInitedOrLoaded(true)
            is StorageEvent.InitFailed -> onStorageInitFailed()
            is StorageEvent.Loaded -> onStorageInitedOrLoaded(event.result)
            else -> super.onStorageEvent(event)
        }
    }

    private fun onStorageFoundInBase() {
        (findViewById<View>(R.id.text_view_name) as TextView).text = viewModel.getStorageName()
        (findViewById<View>(R.id.text_view_path) as TextView).text = viewModel.getStorageFolderPath().fullPath
        viewModel.computeStorageFolderSize()
        viewModel.computeMyTetraXmlLastModifiedDate()
    }

    private fun onStorageInitedOrLoaded(result: Boolean) {
        val error = viewModel.checkStorageFilesExistingError()
        layoutError.isVisible = !error.isNullOrEmpty()
        textViewError.text = error

        when {
            !viewModel.isStorageLoaded() -> {
                layoutLoadStorage.visibility = View.VISIBLE
                buttonLoadStorage.apply {
                    text = getString(R.string.action_load_storage)
                    setOnClickListener {
                        viewModel.startLoadStorage(
                            isLoadFavoritesOnly = false,
                            isHandleReceivedIntent = false,
                            isNeedDecrypt = false,
                        )
                    }
                }
            }
            viewModel.isLoadedFavoritesOnly() -> {
                layoutLoadStorage.visibility = View.VISIBLE
                buttonLoadStorage.apply {
                    text = getString(R.string.action_load_all_nodes)
                    setOnClickListener {
                        viewModel.loadAllNodes(
                            isHandleReceivedIntent = false
                        )
                    }
                }
            }
            else -> {
                fillData()
            }
        }
    }

    private fun fillData() {
        layoutLoadStorage.visibility = View.GONE
        findViewById<RelativeLayout>(R.id.layout_data).visibility = View.VISIBLE
        val info = viewModel.getStorageInfo()
        val isDecrypted = viewModel.isStorageNonEncryptedOrDecrypted()
        info.calcCounters()
        findViewById<TextView>(R.id.text_view_stats_nodes_count).text = info.nodesCount.toString()
        findViewById<TextView>(R.id.text_view_stats_crypt_nodes_count).text = info.cryptedNodesCount.toString()
//        ((TextView)findViewById(R.id.text_view_stats_icons_count)).setText(String.valueOf(info.getIconsCount()));
        findViewById<TextView>(R.id.text_view_stats_max_subnodes).text = info.maxSubnodesCount.toString()
        findViewById<TextView>(R.id.text_view_stats_max_depth).text = info.maxDepthLevel.toString()
        findViewById<TextView>(R.id.text_view_stats_records_count).text = info.recordsCount.toString()
        findViewById<TextView>(R.id.text_view_stats_crypt_records_count).text = info.cryptedRecordsCount.toString()
        findViewById<TextView>(R.id.text_view_stats_files_count).text = info.filesCount.toString()
        findViewById<TextView>(R.id.text_view_stats_tags_count).text = if (isDecrypted) {
            info.uniqueTagsCount.toString()
        } else {
            getString(R.string.title_need_decrypt_storage)
        }
        if (isDecrypted) {
            findViewById<TextView>(R.id.text_view_stats_unique_tags_count).text = info.uniqueTagsCount.toString()
        } else {
            findViewById<TableRow>(R.id.tags_table_row).isVisible = false
        }
        findViewById<TextView>(R.id.text_view_stats_authors_count).text = info.authorsCount.toString()
    }

    private fun onStorageInitFailed() {
        layoutError.visibility = View.VISIBLE
        textViewError.text = viewModel.checkStorageFilesExistingError() ?: getString(R.string.mes_storage_init_error)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        fun start(context: Context, storageId: Int) {
            val intent = Intent(context, StorageInfoActivity::class.java).apply {
                putExtra(Constants.EXTRA_STORAGE_ID, storageId)
            }
            context.startActivity(intent)
        }
    }

}
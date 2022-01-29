package com.gee12.mytetroid.views.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.Constants.ViewEvents
import com.gee12.mytetroid.viewmodels.StorageInfoViewModel

/**
 * Активность для просмотра информации о хранилище.
 */
class StorageInfoActivity : TetroidActivity<StorageInfoViewModel>() {

    override fun getViewModelClazz() = StorageInfoViewModel::class.java

    override fun getLayoutResourceId() = R.layout.activity_storage_info

    override fun getStorageId(): Int {
        return intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // загружаем параметры хранилища из бд
        viewModel.startInitStorage(intent)
    }

    override fun onStorageEvent(event: Constants.StorageEvents?, data: Any?) {
        when (event) {
            Constants.StorageEvents.FoundInBase -> onStorageFoundInBase()
            Constants.StorageEvents.Inited -> onStorageInitedOrLoaded(true)
            Constants.StorageEvents.InitFailed -> onStorageInitFailed()
            Constants.StorageEvents.Loaded -> onStorageInitedOrLoaded(data as Boolean)
            else -> super.onStorageEvent(event, data)
        }
    }

    private fun onStorageFoundInBase() {
        (findViewById<View>(R.id.text_view_name) as TextView).text = viewModel.getStorageName()
        (findViewById<View>(R.id.text_view_path) as TextView).text = viewModel.getStoragePath()
        viewModel.computeStorageFolderSize()
        viewModel.computeMyTetraXmlLastModifiedDate()
    }

    private fun onStorageInitedOrLoaded(result: Boolean) {
        viewModel.checkStorageFilesExistingError()?.let { error ->
            findViewById<RelativeLayout>(R.id.layout_error).visibility = View.VISIBLE
            findViewById<TextView>(R.id.text_view_error).text = error
        } ?: run {
            findViewById<RelativeLayout>(R.id.layout_error).visibility = View.GONE
        }

        when {
            !viewModel.isStorageLoaded() -> {
                findViewById<LinearLayout>(R.id.layout_load_storage).visibility = View.VISIBLE
                findViewById<Button>(R.id.button_load_storage).setOnClickListener {
                    viewModel.startLoadStorage(
                        isLoadFavoritesOnly = false,
                        isHandleReceivedIntent = false
                    )
                }
            }
            viewModel.isLoadedFavoritesOnly() -> {
                findViewById<LinearLayout>(R.id.layout_load_storage).visibility = View.VISIBLE
                findViewById<Button>(R.id.button_load_storage).apply {
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
        findViewById<LinearLayout>(R.id.layout_load_storage).visibility = View.GONE
        findViewById<RelativeLayout>(R.id.layout_data).visibility = View.VISIBLE
        val info = viewModel.getStorageInfo()
        info.calcCounters()
        findViewById<TextView>(R.id.text_view_stats_nodes_count).text = info.nodesCount.toString()
        findViewById<TextView>(R.id.text_view_stats_crypt_nodes_count).text = info.cryptedNodesCount.toString()
//        ((TextView)findViewById(R.id.text_view_stats_icons_count)).setText(String.valueOf(info.getIconsCount()));
        findViewById<TextView>(R.id.text_view_stats_max_subnodes).text = info.maxSubnodesCount.toString()
        findViewById<TextView>(R.id.text_view_stats_max_depth).text = info.maxDepthLevel.toString()
        findViewById<TextView>(R.id.text_view_stats_records_count).text = info.recordsCount.toString()
        findViewById<TextView>(R.id.text_view_stats_crypt_records_count).text = info.cryptedRecordsCount.toString()
        findViewById<TextView>(R.id.text_view_stats_files_count).text = info.filesCount.toString()
        findViewById<TextView>(R.id.text_view_stats_tags_count).text = info.tagsCount.toString()
        findViewById<TextView>(R.id.text_view_stats_unique_tags_count).text = info.uniqueTagsCount.toString()
        findViewById<TextView>(R.id.text_view_stats_authors_count).text = info.authorsCount.toString()
    }

    private fun onStorageInitFailed() {
        findViewById<RelativeLayout>(R.id.layout_error).visibility = View.VISIBLE
        findViewById<TextView>(R.id.text_view_error).text = viewModel.checkStorageFilesExistingError()
            ?: getString(R.string.mes_storage_init_error)
    }

    override fun onViewEvent(event: ViewEvents, data: Any?) {
        when (event) {
            ViewEvents.TaskStarted -> taskPreExecute(R.string.task_storage_initializing)
            ViewEvents.TaskFinished -> taskPostExecute()
            else -> {}
        }
    }

    override fun onObjectEvent(event: Any?, data: Any?) {
        when (event) {
            StorageInfoViewModel.Event.StorageFolderSize -> {
                findViewById<TextView>(R.id.text_view_size).text = data as String
                findViewById<View>(R.id.progress_size).visibility = View.GONE
            }
            StorageInfoViewModel.Event.MyTetraXmlLastModifiedDate -> {
                findViewById<TextView>(R.id.text_view_last_edit).text = data as String
                findViewById<View>(R.id.progress_last_edit).visibility = View.GONE
            }
        }
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
        @JvmStatic
        fun start(context: Context, storageId: Int) {
            val intent = Intent(context, StorageInfoActivity::class.java).apply {
                putExtra(Constants.EXTRA_STORAGE_ID, storageId)
            }
            context.startActivity(intent)
        }
    }

}
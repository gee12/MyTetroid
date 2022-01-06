package com.gee12.mytetroid.views.activities

import android.os.Bundle
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // загружаем параметры хранилища из бд
        viewModel.startInitStorage(intent)
    }

    override fun onStorageEvent(event: Constants.StorageEvents?, data: Any?) {
        when (event) {
            Constants.StorageEvents.Inited -> onStorageInited()
            Constants.StorageEvents.InitFailed -> finish()
            else -> super.onStorageEvent(event, data)
        }
    }

    private fun onStorageInited() {
        viewModel.computeStorageFolderSize()
        viewModel.computeMyTetraXmlLastModifiedDate()

        (findViewById<View>(R.id.text_view_path) as TextView).text = viewModel.getStoragePath()

        // статистика
        val info = viewModel.getStorageInfo()
        info.calcCounters()
        (findViewById<View>(R.id.text_view_stats_nodes_count) as TextView).text = info.nodesCount.toString()
        (findViewById<View>(R.id.text_view_stats_crypt_nodes_count) as TextView).text =
            info.cryptedNodesCount.toString()
//        ((TextView)findViewById(R.id.text_view_stats_icons_count)).setText(String.valueOf(info.getIconsCount()));
//        String maxSubnodes = (info.getMaxSubnodesCount() >= 0) ? String.valueOf(info.getMaxSubnodesCount()) : "?";
//        ((TextView)findViewById(R.id.text_view_stats_max_subnodes)).setText(maxSubnodes);
        (findViewById<View>(R.id.text_view_stats_max_subnodes) as TextView).text = info.maxSubnodesCount.toString()
//        String maxDepthLevel = (info.getMaxDepthLevel() >= 0) ? String.valueOf(info.getMaxDepthLevel()) : "?";
//        ((TextView)findViewById(R.id.text_view_stats_max_depth)).setText(maxDepthLevel);
        (findViewById<View>(R.id.text_view_stats_max_depth) as TextView).text = info.maxDepthLevel.toString()
        (findViewById<View>(R.id.text_view_stats_records_count) as TextView).text = info.recordsCount.toString()
        (findViewById<View>(R.id.text_view_stats_crypt_records_count) as TextView).text =
            info.cryptedRecordsCount.toString()
        (findViewById<View>(R.id.text_view_stats_files_count) as TextView).text = info.filesCount.toString()
        (findViewById<View>(R.id.text_view_stats_tags_count) as TextView).text = info.tagsCount.toString()
//        String uniqueTags = (info.getUniqueTagsCount() >= 0) ? String.valueOf(info.getUniqueTagsCount()) : "?";
//        ((TextView)findViewById(R.id.text_view_stats_unique_tags_count)).setText(uniqueTags);
        (findViewById<View>(R.id.text_view_stats_unique_tags_count) as TextView).text = info.uniqueTagsCount.toString()
        (findViewById<View>(R.id.text_view_stats_authors_count) as TextView).text = info.authorsCount.toString()
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

}
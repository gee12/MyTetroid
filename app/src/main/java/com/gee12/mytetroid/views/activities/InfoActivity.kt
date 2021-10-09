package com.gee12.mytetroid.views.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.Constants.MainEvents
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.viewmodels.ViewModelEvent
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory

/**
 * Активность для просмотра информации о хранилище.
 */
class InfoActivity : AppCompatActivity() {
    
    private lateinit var viewModel: StorageViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        
        initViewModel()
        
        // загружаем параметры хранилища из бд
        if (!viewModel.initStorage(intent)) {
            finish()
            return
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, TetroidViewModelFactory(application))
            .get(StorageViewModel::class.java)

        viewModel.storageEvent.observe(this, Observer { it ->
            when (it.state) {
                Constants.StorageEvents.Inited -> onStorageInited()
                else -> {}
            }
        })
    }

    private fun onStorageInited() {
        (findViewById<View>(R.id.text_view_path) as TextView).text = viewModel.getStoragePath()
        (findViewById<View>(R.id.text_view_last_edit) as TextView).text =
            viewModel.getMyTetraXmlLastModifiedDate()
        (findViewById<View>(R.id.text_view_size) as TextView).text = viewModel.getStorageFolderSize()

        // статистика
        val info = viewModel.xmlLoader
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
}
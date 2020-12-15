package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidXml;
import com.gee12.mytetroid.utils.Utils;

import java.util.Date;

/**
 * Активность для просмотра информации о хранилище.
 */
public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String path = DataManager.getStoragePath();
        ((TextView)findViewById(R.id.text_view_path)).setText(path);
        String tetraXml = DataManager.getPathToMyTetraXml();
        Date edited = DataManager.getFileModifiedDate(this, tetraXml);
        ((TextView)findViewById(R.id.text_view_last_edit)).setText(
                Utils.dateToString(edited, getString(R.string.full_date_format_string)));
        ((TextView)findViewById(R.id.text_view_size)).setText(
                DataManager.getFileSize(this, path));
        // статистика
        TetroidXml storage = DataManager.getInstance().getXmlManager();
        storage.calcCounters();
        ((TextView)findViewById(R.id.text_view_stats_nodes_count)).setText(String.valueOf(storage.getNodesCount()));
        ((TextView)findViewById(R.id.text_view_stats_crypt_nodes_count)).setText(String.valueOf(storage.getCryptedNodesCount()));
        ((TextView)findViewById(R.id.text_view_stats_icons_count)).setText(String.valueOf(storage.getIconsCount()));
//        String maxSubnodes = (storage.getMaxSubnodesCount() >= 0) ? String.valueOf(storage.getMaxSubnodesCount()) : "?";
//        ((TextView)findViewById(R.id.text_view_stats_max_subnodes)).setText(maxSubnodes);
        ((TextView)findViewById(R.id.text_view_stats_max_subnodes)).setText(String.valueOf(storage.getMaxSubnodesCount()));
//        String maxDepthLevel = (storage.getMaxDepthLevel() >= 0) ? String.valueOf(storage.getMaxDepthLevel()) : "?";
//        ((TextView)findViewById(R.id.text_view_stats_max_depth)).setText(maxDepthLevel);
        ((TextView)findViewById(R.id.text_view_stats_max_depth)).setText(String.valueOf(storage.getMaxDepthLevel()));
        ((TextView)findViewById(R.id.text_view_stats_records_count)).setText(String.valueOf(storage.getRecordsCount()));
        ((TextView)findViewById(R.id.text_view_stats_crypt_records_count)).setText(String.valueOf(storage.getCryptedRecordsCount()));
        ((TextView)findViewById(R.id.text_view_stats_files_count)).setText(String.valueOf(storage.getFilesCount()));
        ((TextView)findViewById(R.id.text_view_stats_tags_count)).setText(String.valueOf(storage.getTagsCount()));
//        String uniqueTags = (storage.getUniqueTagsCount() >= 0) ? String.valueOf(storage.getUniqueTagsCount()) : "?";
//        ((TextView)findViewById(R.id.text_view_stats_unique_tags_count)).setText(uniqueTags);
        ((TextView)findViewById(R.id.text_view_stats_unique_tags_count)).setText(String.valueOf(storage.getUniqueTagsCount()));
        ((TextView)findViewById(R.id.text_view_stats_authors_count)).setText(String.valueOf(storage.getAuthorsCount()));
    }
}

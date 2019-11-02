package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.XMLManager;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        XMLManager storage = DataManager.getInstance();
        ((TextView)findViewById(R.id.text_view_stats_nodes_count)).setText(storage.getNodesCount());
        ((TextView)findViewById(R.id.text_view_stats_crypt_nodes_count)).setText(storage.getCryptedNodesCount());
        ((TextView)findViewById(R.id.text_view_stats_icons_count)).setText(storage.getIconsCount());
        ((TextView)findViewById(R.id.text_view_stats_max_subnodes)).setText(storage.getMaxSubnodesCount());
        ((TextView)findViewById(R.id.text_view_stats_max_depth)).setText(storage.getMaxDepthLevel());
        ((TextView)findViewById(R.id.text_view_stats_records_count)).setText(storage.getRecordsCount());
        ((TextView)findViewById(R.id.text_view_stats_tags_count)).setText(storage.getTagsCount());
        ((TextView)findViewById(R.id.text_view_stats_unique_tags_count)).setText(storage.getUniqueTagsCount());
        ((TextView)findViewById(R.id.text_view_stats_authors_count)).setText(storage.getAuthorsCount());
    }
}

package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.adapters.StoragesAdapter;
import com.gee12.mytetroid.model.TetroidStorage;
import com.gee12.mytetroid.viewmodels.StoragesViewModel;

public class StoragesActivity extends TetroidActivity {

    private StoragesViewModel mViewModel;
    private RecyclerView mRecyclerView;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_logs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mViewModel = new ViewModelProvider(this).get(StoragesViewModel.class);

        this.mRecyclerView = findViewById(R.id.recycle_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        StoragesAdapter adapter = new StoragesAdapter(this);
        adapter.setOnItemClickListener(v -> {
            int itemPosition = mRecyclerView.getChildLayoutPosition(v);
            TetroidStorage item = adapter.getItem(itemPosition);

            // FIXME:
            Toast.makeText(this, item.getName(), Toast.LENGTH_LONG).show();
        });

        mViewModel.getStorages().observe(this, adapter::submitList);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onGUICreated() {
    }

    @Override
    protected void loadStorage(String folderPath) {
    }

    @Override
    protected void createStorage(String storagePath) {
    }
}

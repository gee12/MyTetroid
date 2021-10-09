package com.gee12.mytetroid.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.interactors.StorageInteractor;
import com.gee12.mytetroid.viewmodels.IconsViewModel;
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory;
import com.gee12.mytetroid.views.adapters.IconsListAdapter;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.TetroidIcon;
import com.gee12.mytetroid.model.TetroidNode;

import java.io.File;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * Активность для выбора иконки ветки.
 */
public class IconsActivity extends AppCompatActivity {

    private Spinner spFolders;
    private ArrayAdapter<String> mFoldersAdapter;
    private IconsListAdapter mIconsAdapter;
    private View btnSubmit;
    private String mNodeId;
    private TetroidIcon mSelectedIcon;

    private IconsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icons);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.viewModel = new ViewModelProvider(this, new TetroidViewModelFactory(getApplication()))
                .get(IconsViewModel.class);

        // список иконок
        ListView mListView = findViewById(R.id.list_view_icons);
        this.mIconsAdapter = new IconsListAdapter(this, viewModel.getPathToIcons());
        mListView.setAdapter(mIconsAdapter);
        mListView.setOnItemClickListener((adapterView, view, i, l) -> {
            TetroidIcon icon = (TetroidIcon) mIconsAdapter.getItem(i);
            if (icon != null) {
                this.mSelectedIcon = icon;
                mIconsAdapter.setSelectedIcon(icon);
                mIconsAdapter.setSelectedView(view);
                btnSubmit.setEnabled(true);
            }
        });

        // список каталогов
        this.spFolders = findViewById(R.id.spinner_folders);
        List<String> folders = viewModel.getIconsFolders();
        if (folders != null) {
            if (folders.size() == 0) {
                TetroidLog.log(this, R.string.log_icons_dirs_absent, Toast.LENGTH_LONG);
                return;
            }
            this.mFoldersAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, folders);
            mFoldersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFolders.setAdapter(mFoldersAdapter);

            spFolders.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String folder = mFoldersAdapter.getItem(position);
                    if (folder != null) {
                        List<TetroidIcon> icons = viewModel.getIconsFromFolder(folder);
                        if (icons != null) {
                            // пришлось заново пересоздавать адаптер, т.к. при простой установке dataSet
                            // с помощью reset(icons)
                            // не сбрасывалось выделение от предыдущего списка icons (из другого раздела)
                            mIconsAdapter = new IconsListAdapter(IconsActivity.this, viewModel.getPathToIcons());
                            mListView.setAdapter(mIconsAdapter);
                            if (mSelectedIcon != null && folder.equals(mSelectedIcon.getFolder())) {
                                String selectedIconName = mSelectedIcon.getName();
                                OptionalInt iconPos = IntStream.range(0, icons.size())
                                        .filter(i -> selectedIconName.equals(icons.get(i).getName()))
                                        .findFirst();
                                if (iconPos.isPresent()) {
                                    // выделяем существующую иконку
                                    int pos = iconPos.getAsInt();
                                    TetroidIcon icon = icons.get(pos);
                                    mSelectedIcon = icon;
                                    mIconsAdapter.setSelectedIcon(icon);
                                    btnSubmit.setEnabled(true);
                                }
                                mIconsAdapter.setData(icons);
                                if (iconPos.isPresent()) {
                                    int pos = iconPos.getAsInt();
                                    mListView.setSelection(pos);
                                }
                            } else {
//                                mIconsAdapter.setSelectedIcon(null);
//                                mIconsAdapter.setSelectedView(null);
                                mIconsAdapter.setData(icons);
//                                mListView.setItemChecked(-1, true);
//                                mListView.setSelection(-1);
//                                mListView.setSelectionAfterHeaderView();
                            }
                        }
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            TetroidLog.log(this,
                    getString(R.string.log_icons_dir_absent_mask, StorageInteractor.ICONS_FOLDER_NAME), Toast.LENGTH_LONG);
        }

        findViewById(R.id.button_clear_icon).setOnClickListener(view -> setResult(null));
        this.btnSubmit = findViewById(R.id.button_submit);
        btnSubmit.setOnClickListener(view -> setResult(mSelectedIcon));

        // получаем данные
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mNodeId = extras.getString(Constants.EXTRA_NODE_ID);
            String iconPath = extras.getString(Constants.EXTRA_NODE_ICON_PATH);
            selectCurrentIcon(iconPath);
        }
    }

    private void selectCurrentIcon(String iconPath) {
        if (!TextUtils.isEmpty(iconPath)) {
            String[] pathParts = iconPath.split(File.separator);
            if (pathParts.length >= 2) {
                String name = pathParts[pathParts.length - 1];
                String folder = pathParts[pathParts.length - 2];
                mSelectedIcon = new TetroidIcon(folder, name);

                if (mFoldersAdapter != null) {
                    int pos = mFoldersAdapter.getPosition(folder);
                    if (pos >= 0) {
                        spFolders.setSelection(pos);
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setResult(TetroidIcon icon) {
        Intent intent = new Intent();
        intent.putExtra(Constants.EXTRA_NODE_ID, mNodeId);
        intent.putExtra(Constants.EXTRA_NODE_ICON_PATH, (icon != null) ? icon.getPath() : null);
        intent.putExtra(Constants.EXTRA_IS_DROP, icon == null);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public static void startIconsActivity(Activity activity, TetroidNode node, int requestCode) {
        if (node == null) {
            return;
        }
        Intent intent = new Intent(activity, IconsActivity.class);
        intent.putExtra(Constants.EXTRA_NODE_ID, node.getId());
        intent.putExtra(Constants.EXTRA_NODE_ICON_PATH, node.getIconName());
        activity.startActivityForResult(intent, requestCode);
    }
}

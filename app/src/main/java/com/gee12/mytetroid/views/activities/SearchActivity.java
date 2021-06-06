package com.gee12.mytetroid.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.data.StorageManager;
import com.gee12.mytetroid.views.dialogs.NodeDialogs;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.views.Message;

/**
 * Аквтивность для настройки параметров глобального поиска.
 */
public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_KEY_SCAN_MANAGER = "ScanManager";

    private EditText etQuery;
    private CheckBox cbText;
    private CheckBox cbRecordsNames;
    private CheckBox cbAuthor;
    private CheckBox cbUrl;
    private CheckBox cbTags;
    private CheckBox cbNodes;
    private CheckBox cbFiles;
    private CheckBox cbIds;
    private Spinner spSplitToWords;
    private Spinner spInWholeWords;
    private Spinner spInNodeMode;
    private EditText etNodeName;
    private ImageButton bNodeChooser;
//    private boolean isCurNodeNotNull;
    private String nodeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.etQuery = findViewById(R.id.edit_text_query);
        this.cbText = findViewById(R.id.check_box_records_text);
        this.cbRecordsNames = findViewById(R.id.check_box_records_names);
        this.cbAuthor = findViewById(R.id.check_box_author);
        this.cbUrl = findViewById(R.id.check_box_url);
        this.cbTags = findViewById(R.id.check_box_tags);
        this.cbNodes = findViewById(R.id.check_box_nodes);
        this.cbFiles = findViewById(R.id.check_box_files);
        this.cbIds = findViewById(R.id.check_box_ids);
        this.spSplitToWords = findViewById(R.id.spinner_split_to_words);
        this.spInWholeWords = findViewById(R.id.spinner_in_whole_words);
        this.spInNodeMode = findViewById(R.id.spinner_in_cur_node);

        initSpinner(spSplitToWords, R.array.search_split_to_words);
        initSpinner(spInWholeWords, R.array.search_in_whole_words);
        initSpinner(spInNodeMode, R.array.search_in_cur_node);
        spInNodeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                   updateNodeChooser();
               }
               @Override
               public void onNothingSelected(AdapterView<?> parent) {

               }
           });

        findViewById(R.id.button_clear).setOnClickListener(v -> etQuery.setText(""));

        readSearchPrefs();

        etQuery.setSelection(etQuery.getText().length());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
//            this.isCurNodeNotNull = extras.getBoolean(MainActivity.EXTRA_CUR_NODE_IS_NOT_NULL);
            if (SettingsManager.getSearchInNodeMode(this) == 1) {
                this.nodeId = extras.getString(MainActivity.EXTRA_CUR_NODE_ID);
            }
            String query = extras.getString(MainActivity.EXTRA_QUERY);
            if (query != null) {
                etQuery.setText(query);
            }
        }

        initNodeChooser();
        updateNodeChooser();
    }

    /**
     *
     */
    private void readSearchPrefs() {
        etQuery.setText(SettingsManager.getSearchQuery(this));
        cbText.setChecked(SettingsManager.isSearchInText(this));
        cbRecordsNames.setChecked(SettingsManager.isSearchInRecordsNames(this));
        cbAuthor.setChecked(SettingsManager.isSearchInAuthor(this));
        cbUrl.setChecked(SettingsManager.isSearchInUrl(this));
        cbTags.setChecked(SettingsManager.isSearchInTags(this));
        cbNodes.setChecked(SettingsManager.isSearchInNodes(this));
        cbFiles.setChecked(SettingsManager.isSearchInFiles(this));
        cbIds.setChecked(SettingsManager.isSearchInIds(this));
        spSplitToWords.setSelection(SettingsManager.isSearchSplitToWords(this) ? 0 : 1);
        spInWholeWords.setSelection(SettingsManager.isSearchInWholeWords(this) ? 0 : 1);
//        spInNodeMode.setSelection(SettingsManager.isSearchInCurNode(this) ? 1 : 0);
        spInNodeMode.setSelection(SettingsManager.getSearchInNodeMode(this));
        nodeId = SettingsManager.getSearchNodeId(this);
    }

    /**
     *
     * @param spinner
     * @param arrayId
     */
    private void initSpinner(Spinner spinner, int arrayId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     *
     */
    private void initNodeChooser() {
        this.etNodeName = findViewById(R.id.edit_text_node);
        this.bNodeChooser = findViewById(R.id.button_node);

        TetroidNode node;
        if (StorageManager.isLoaded()) {
            node = NodesManager.getNode(nodeId);
            if (node == null) {
                // очищаем, если такой ветки нет
                nodeId = null;
            }
        } else {
            LogManager.log(this, R.string.title_storage_not_loaded, ILogger.Types.ERROR, Toast.LENGTH_LONG);
            finish();
            return;
        }

        etNodeName.setText((node != null) ? node.getName() : getString(R.string.title_select_node));
        etNodeName.setInputType(InputType.TYPE_NULL);

        // диалог выбора ветки
        NodeDialogs.NodeChooserResult nodeCallback = new NodeDialogs.NodeChooserResult() {
            @Override
            public void onApply(TetroidNode node) {
                this.mSelectedNode = node;
                if (node != null) {
                    etNodeName.setText(node.getName());
                    nodeId = node.getId();
                }
            }
            @Override
            public void onProblem(int code) {
                switch (code) {
                    case NodeDialogs.INodeChooserResult.LOAD_STORAGE:
                        Message.show(SearchActivity.this, getString(R.string.log_storage_need_load), Toast.LENGTH_LONG);
                        break;
                    case NodeDialogs.INodeChooserResult.LOAD_ALL_NODES:
                        Message.show(SearchActivity.this, getString(R.string.log_all_nodes_need_load), Toast.LENGTH_LONG);
                        break;
                }
            }
        };
        View.OnClickListener clickListener = v -> {
            NodeDialogs.createNodeChooserDialog(this,
                    (nodeCallback.getSelectedNode() != null) ? nodeCallback.getSelectedNode() : node,
                    false, true, false, nodeCallback);
        };
        etNodeName.setOnClickListener(clickListener);
        bNodeChooser.setOnClickListener(clickListener);
    }

    /**
     *
     */
    private void updateNodeChooser() {
        if (spInNodeMode.getSelectedItemPosition() == 1) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                nodeId = extras.getString(MainActivity.EXTRA_CUR_NODE_ID);
            }
        }
        boolean isNodeSelectionMode = (spInNodeMode.getSelectedItemPosition() == 2);
        etNodeName.setEnabled(isNodeSelectionMode);
        bNodeChooser.setEnabled(isNodeSelectionMode);
    }

    /**
     *
     * @return
     */
    public ScanManager buildScanManager() {
        String query = etQuery.getText().toString();
        ScanManager scan = new ScanManager(query);
        scan.setInText(cbText.isChecked());
        scan.setInRecordsNames(cbRecordsNames.isChecked());
        scan.setInAuthor(cbAuthor.isChecked());
        scan.setInUrl(cbUrl.isChecked());
        scan.setInTags(cbTags.isChecked());
        scan.setInNodes(cbNodes.isChecked());
        scan.setInFiles(cbFiles.isChecked());
        scan.setInIds(cbIds.isChecked());

        scan.setSplitToWords(spSplitToWords.getSelectedItemPosition() == 0);
        scan.setOnlyWholeWords(spInWholeWords.getSelectedItemPosition() == 0);
        scan.setSearchInNode(spInNodeMode.getSelectedItemPosition() != 0);
        scan.setNodeId(nodeId);
        return scan;
    }

    /**
     *
     */
    private void startSearch() {
        // сохраняем параметры поиск
        saveSearchPrefs();
        // запускаем поиск и выводим результат
        Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_SCAN_MANAGER, buildScanManager());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     *
     */
    private void saveSearchPrefs() {
        SettingsManager.setSearchQuery(this, etQuery.getText().toString());
        SettingsManager.setSearchInText(this, cbText.isChecked());
        SettingsManager.setSearchInRecordsNames(this, cbRecordsNames.isChecked());
        SettingsManager.setSearchInAuthor(this, cbAuthor.isChecked());
        SettingsManager.setSearchInUrl(this, cbUrl.isChecked());
        SettingsManager.setSearchInTags(this, cbTags.isChecked());
        SettingsManager.setSearchInNodes(this, cbNodes.isChecked());
        SettingsManager.setSearchInFiles(this, cbFiles.isChecked());
        SettingsManager.setSearchInIds(this, cbIds.isChecked());
        SettingsManager.setSearchSplitToWords(this, spSplitToWords.getSelectedItemPosition() == 0);
        SettingsManager.setSearchInWholeWords(this, spInWholeWords.getSelectedItemPosition() == 0);
//        SettingsManager.setSearchInCurNode(this, spInCurrentNode.getSelectedItemPosition() == 1);
        SettingsManager.setSearchInNodeMode(this, spInNodeMode.getSelectedItemPosition());
        SettingsManager.setSearchNodeId(this, nodeId);
    }

    /**
     * Обработчик создания системного меню
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.global_search, menu);
        return true;
    }

    /**
     * Обработчик выбора пунктов системного меню
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_query_submit) {
            if (TextUtils.isEmpty(etQuery.getText().toString())) {
                Message.show(this, getString(R.string.title_enter_query));
//            } else if (spInCurrentNode.getSelectedItem().equals(getString(R.string.global_search_in_cur_node))
            } else if (spInNodeMode.getSelectedItemPosition() == 1 && TextUtils.isEmpty(nodeId)) {
                Message.show(this, getString(R.string.log_cur_node_is_not_selected));
            } else if (spInNodeMode.getSelectedItemPosition() == 2 && TextUtils.isEmpty(nodeId)) {
                Message.show(this, getString(R.string.log_select_node_to_search));
            } else {
                startSearch();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

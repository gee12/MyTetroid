package com.gee12.mytetroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.gee12.mytetroid.Message;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.data.ScanManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.gee12.mytetroid.R;

public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_KEY_SCAN_MANAGER = "ScanManager";

    EditText etQuery;
    CheckBox cbText;
    CheckBox cbRecordsNames;
    CheckBox cbAuthor;
    CheckBox cbUrl;
    CheckBox cbTags;
    CheckBox cbNodes;
    CheckBox cbFiles;
    Spinner spSplitToWords;
    Spinner spInWholeWords;
    Spinner spInCurrentNode;

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
        this.spSplitToWords = findViewById(R.id.spinner_split_to_words);
        this.spInWholeWords = findViewById(R.id.spinner_in_whole_words);
        this.spInCurrentNode = findViewById(R.id.spinner_in_cur_node);

        initSpinner(spSplitToWords, R.array.search_split_to_words);
        initSpinner(spInWholeWords, R.array.search_in_whole_words);
        spInWholeWords.setEnabled(false);
        initSpinner(spInCurrentNode, R.array.search_in_cur_node);

        readSearchPrefs();

        etQuery.setSelection(etQuery.getText().length());
    }

    private void readSearchPrefs() {
        etQuery.setText(SettingsManager.getSearchQuery());
        cbText.setChecked(SettingsManager.getSearchInText());
        cbRecordsNames.setChecked(SettingsManager.getSearchInRecordsNames());
        cbAuthor.setChecked(SettingsManager.getSearchInAuthor());
        cbUrl.setChecked(SettingsManager.getSearchInUrl());
        cbTags.setChecked(SettingsManager.getSearchInTags());
        cbNodes.setChecked(SettingsManager.getSearchInNodes());
        cbFiles.setChecked(SettingsManager.getSearchInFiles());
        spSplitToWords.setSelection(SettingsManager.getSearchSplitToWords() ? 0 : 1);
        spInWholeWords.setSelection(SettingsManager.getSearchInWholeWords() ? 0 : 1);
        spInCurrentNode.setSelection(SettingsManager.getSearchInCurNode() ? 1 : 0);
    }

    private void initSpinner(Spinner spinner, int arrayId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

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

        scan.setSplitToWords(spSplitToWords.getSelectedItemPosition() == 0);
        scan.setOnlyWholeWords(spInWholeWords.getSelectedItemPosition() == 0);
        scan.setSearchInNode(spInCurrentNode.getSelectedItemPosition() == 1);
        return scan;
    }

    private void startSearch() {
        // сохраняем параметры поиск
        saveSearchPrefs();
        // запускаем поиск и выводим результат
        Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_SCAN_MANAGER, buildScanManager());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void saveSearchPrefs() {
        SettingsManager.setSearchQuery(etQuery.getText().toString());
        SettingsManager.setSearchInText(cbText.isChecked());
        SettingsManager.setSearchInRecordsNames(cbRecordsNames.isChecked());
        SettingsManager.setSearchInAuthor(cbAuthor.isChecked());
        SettingsManager.setSearchInUrl(cbUrl.isChecked());
        SettingsManager.setSearchInTags(cbTags.isChecked());
        SettingsManager.setSearchInNodes(cbNodes.isChecked());
        SettingsManager.setSearchInFiles(cbFiles.isChecked());
        SettingsManager.setSearchSplitToWords(spSplitToWords.getSelectedItemPosition() == 0);
        SettingsManager.setSearchInWholeWords(spInWholeWords.getSelectedItemPosition() == 0);
        SettingsManager.setSearchInCurNode(spInCurrentNode.getSelectedItemPosition() == 1);
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
            if (Utils.isNullOrEmpty(etQuery.getText().toString())) {
                Message.create(this, getString(R.string.enter_query));
            } else {
                startSearch();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

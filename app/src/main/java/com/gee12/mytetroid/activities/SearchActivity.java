package com.gee12.mytetroid.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.gee12.mytetroid.data.ScanManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    Spinner spInWHoleWords;
    Spinner spInCurrentNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        this.etQuery = findViewById(R.id.edit_text_query);
        this.cbText = findViewById(R.id.check_box_records_text);
        this.cbRecordsNames = findViewById(R.id.check_box_records_names);
        this.cbAuthor = findViewById(R.id.check_box_author);
        this.cbUrl = findViewById(R.id.check_box_url);
        this.cbTags = findViewById(R.id.check_box_tags);
        this.cbNodes = findViewById(R.id.check_box_nodes);
        this.cbFiles = findViewById(R.id.check_box_files);
        this.spSplitToWords = findViewById(R.id.spinner_split_to_words);
        this.spInWHoleWords = findViewById(R.id.spinner_in_whole_words);
        this.spInCurrentNode = findViewById(R.id.spinner_in_cur_node);

        initSpinner(R.id.spinner_split_to_words, R.array.search_split_to_words);
        initSpinner(R.id.spinner_in_whole_words, R.array.search_in_whole_words);
        initSpinner(R.id.spinner_in_cur_node, R.array.search_in_cur_node);
    }

    private void initSpinner(int spinnerId, int arrayId) {
        Spinner spinner = findViewById(spinnerId);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public ScanManager buildScanManager() {
        String query = etQuery.getText().toString();
//        boolean inText = cbText.isChecked();
//        boolean inRecorNames = cbRecordsNames.isChecked();
//        boolean inAuthor = cbAuthor.isChecked();
//        boolean inUrl = cbUrl.isChecked();
//        boolean inTags = cbTags.isChecked();
//        boolean inNodes = cbNodes.isChecked();
//        boolean inFiles = cbFiles.isChecked();

        ScanManager scan = new ScanManager(query);
        scan.setInText(cbText.isChecked());
        scan.setInRecordsNames(cbRecordsNames.isChecked());
        scan.setInAuthor(cbAuthor.isChecked());
        scan.setInUrl(cbUrl.isChecked());
        scan.setInTags(cbTags.isChecked());
        scan.setInNodes(cbNodes.isChecked());
        scan.setInFiles(cbFiles.isChecked());

        scan.setSplitToWords(spSplitToWords.getSelectedItemPosition() == 0);
        scan.setOnlyWholeWords(spInWHoleWords.getSelectedItemPosition() == 0);
        scan.setSearchInNode(spInCurrentNode.getSelectedItemPosition() == 1);
        return scan;
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
            Intent intent = new Intent();
            intent.putExtra(EXTRA_KEY_SCAN_MANAGER, buildScanManager());
            setResult(Activity.RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

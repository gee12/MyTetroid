package com.gee12.mytetroid.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.Spanned;
import android.view.ContextMenu;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.UriUtil;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidNode;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.views.NodesListAdapter;
import com.gee12.mytetroid.views.RecordsListAdapter;

//import net.rdrei.android.dirchooser.DirectoryChooserActivity;
//import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.net.URLDecoder;

import lib.folderpicker.FolderPicker;
import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListView;
import pl.openrnd.multilevellistview.OnItemClickListener;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_OPEN_DIRECTORY = 1;

    public static final int FILE_BROWSE = 1;
    public static final int GET_CONTENT = 2;
    public static final int OPEN_DOC = 3;

    public static final int OPEN_RECORD_MENU_ITEM_ID = 1;
    public static final int RECORDS_LIST_VIEW_NUM = 0;
    public static final int RECORD_DETAILS_VIEW_NUM = 1;

    private DrawerLayout drawerLayout;
    private RecordsListAdapter listAdapter;
    private ListView recordsListView;
    private MultiLevelListView nodesListView;
    private TetroidNode currentNode;
    private ViewSwitcher viewSwitcher;
//    private TextView recordContentTextView;
    private WebView recordContentWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        // панель
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        drawerLayout.openDrawer(GravityCompat.START);
        toggle.syncState();

        // список веток
        nodesListView = (MultiLevelListView) findViewById(R.id.nodes_list_view);
        nodesListView.setOnItemClickListener(onNodeClickListener);
        // список записей
        recordsListView = (ListView)findViewById(R.id.records_list_view);
        recordsListView.setOnItemClickListener(onRecordClicklistener);
        TextView emptyTextView = (TextView)findViewById(R.id.text_view_empty);
        recordsListView.setEmptyView(emptyTextView);
        registerForContextMenu(recordsListView);

        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
//        recordContentTextView = (TextView) findViewById(R.id.text_view_record_content);
        recordContentWebView = (WebView)findViewById(R.id.web_view_record_content);

        // загружаем данные
        SettingsManager.init(this);
        initStorageFromSettings();
    }

    private void initStorageFromSettings() {
        String storagePath = SettingsManager.getStoragePath();
//        String storagePath = "net://Иван Бондарь-687:@gdrive/MyTetraData";
        if (storagePath != null) {
            // показываем диалог подтверждения
            // ...

            initStorage(storagePath);
        } else {
            showChooser3();
        }
    }

    private void initStorage(String storagePath) {
        if (DataManager.init(storagePath)) {
            SettingsManager.setStoragePath(storagePath);
            initListViews();
        } else {

        }
    }

    private void initListViews() {
        // список веток
        NodesListAdapter listAdapter = new NodesListAdapter(this, onNodeHeaderClickListener);
        nodesListView.setAdapter(listAdapter);
        listAdapter.setDataItems(DataManager.getRootNodes());

        // список записей
        this.listAdapter = new RecordsListAdapter(this);
    }

    void showChooser1() {
        //            if (StorageAF.useStorageFramework(FileSelectActivity.this)) {
        if (false) {
            Intent intent = new Intent(StorageChooserActivity.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
//                intent.setType("text/*");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, OPEN_DOC);
        }
        else {
            Intent intent;
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
//                intent.setType("text/*");

            try {
                startActivityForResult(intent, GET_CONTENT);
            } catch (ActivityNotFoundException e) {
//                    lookForOpenIntentsFilePicker();
            } catch (SecurityException e) {
//                    lookForOpenIntentsFilePicker();
            }
        }
    }

//    static final int REQUEST_DIRECTORY = 222;
//    void showChooser2() {
//        final Intent chooserIntent = new Intent(this, DirectoryChooserActivity.class);
//
//        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
//                .newDirectoryName("DirChooserSample")
//                .allowReadOnlyDirectory(true)
//                .allowNewDirectoryNameModification(true)
//                .build();
//
//        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
////        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_NEW_DIR_NAME, "Snapprefs");
//
//        // REQUEST_DIRECTORY is a constant integer to identify the request, e.g. 0
//        startActivityForResult(chooserIntent, REQUEST_DIRECTORY);
//    }

    static final int FOLDERPICKER_CODE = 333;
    void showChooser3() {
        Intent intent = new Intent(this, FolderPicker.class);
        startActivityForResult(intent, FOLDERPICKER_CODE);
    }

//    private void lookForOpenIntentsFilePicker() {
//
////        if (Interaction.isIntentAvailable(FileSelectActivity.this, Intents.OPEN_INTENTS_FILE_BROWSE)) {
//        if (true) {
//            Intent i = new Intent(Intents.OPEN_INTENTS_FILE_BROWSE);
//            i.setData(Uri.parse("file://" + Util.getEditText(FileSelectActivity.this, R.id.file_filename)));
//            try {
//                startActivityForResult(i, FILE_BROWSE);
//            } catch (ActivityNotFoundException e) {
////                showBrowserDialog();
//            }
//
//        } else {
////            showBrowserDialog();
//        }
//    }

//    private void showBrowserDialog() {
//        BrowserDialog diag = new BrowserDialog(FileSelectActivity.this);
//        diag.show();
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        fillData();

        String fileName = null;
        if (requestCode == FILE_BROWSE && resultCode == RESULT_OK) {
            fileName = data.getDataString();
            if (fileName != null) {
                if (fileName.startsWith("file://")) {
                    fileName = fileName.substring(7);
                }

                fileName = URLDecoder.decode(fileName);
            }

        }
        else if ((requestCode == GET_CONTENT || requestCode == OPEN_DOC) && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
//                    if (StorageAF.useStorageFramework(this)) {
                    if (false) {
                        try {
                            // try to persist read and write permissions
                            ContentResolver resolver = getContentResolver();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            }
                        } catch (Exception e) {
                            // nop
                        }
                    }
                    if (requestCode == GET_CONTENT) {
                        uri = UriUtil.translate(this, uri);
                    }
                    fileName = uri.toString();
                }
            }
//        } else if (requestCode == REQUEST_DIRECTORY) {
//            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
//                fileName = (data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
//            } else {
//                // Nothing selected
//            }
        } else if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {

            fileName = data.getExtras().getString("data");
        }

        if (fileName != null) {
            // выбор файла mytetra.xml
//            File file = new File(fileName);
//            String path = file.getParent();

            // 1
//            Uri uri = UriUtil.parseDefaultFile(fileName);
//            String scheme = uri.getScheme();
//
//            if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
//                File dbFile = new File(uri.getPath());
//                if (!dbFile.exists()) {
////                    throw new FileNotFoundException();
//                    return;
//                }
//                String path = dbFile.getParent();
//                initListViews(path);
//            }
            // 2
            initStorage(fileName);
        }
    }



    /**
     * Отображение ветки => список записей
     * @param node
     */
    private void showNode(TetroidNode node)
    {
        if (!node.isDecrypted()) {
            Toast.makeText(this, "Нужно ввести пароль", Toast.LENGTH_SHORT).show();
            return;
        }
        this.currentNode = node;
        Toast.makeText(getApplicationContext(), node.getName(), Toast.LENGTH_SHORT).show();
        if (viewSwitcher.getDisplayedChild() == RECORD_DETAILS_VIEW_NUM)
            viewSwitcher.showPrevious();
        drawerLayout.closeDrawers();

        this.listAdapter.reset(node.getRecords());
        recordsListView.setAdapter(listAdapter);
        setTitle(node.getName());
//        Toast.makeText(this, "Открытие " + node.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей ветки
     */
    private void showRecord(int position) {
        TetroidRecord record = currentNode.getRecords().get(position);
        showRecord(record);
    }

    /**
     * Отображение записи
     * @param record Запись
     */
    private void showRecord(TetroidRecord record) {
        String recordContentUrl = record.getRecordTextUrl(DataManager.getStoragePath());
//        Spanned recordContent = record.getContent();
//        recordContentTextView.setText(recordContent);
        recordContentWebView.loadUrl(recordContentUrl);
        viewSwitcher.showNext();
        setTitle(record.getName());
//        Toast.makeText(this, "Открытие " + record.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Обработчик клика на заголовке ветки с подветками
     */
    NodesListAdapter.OnNodeHeaderClickListener onNodeHeaderClickListener = new NodesListAdapter.OnNodeHeaderClickListener() {
        @Override
        public void onClick(TetroidNode node) {
            showNode(node);
        }
    };

    /**
     * Обработчик клика на "конечной" ветке (без подветок)
     */
    private OnItemClickListener onNodeClickListener = new OnItemClickListener() {

        @Override
        public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            showNode((TetroidNode)item);
        }

        @Override
        public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            // это событие обрабатывается с помощью OnNodeHeaderClickListener, чтобы разделить клик
            // на заголовке и на стрелке раскрытия/закрытия ветки
            if (!((TetroidNode)item).isDecrypted()) {
                Toast.makeText(MainActivity.this, "Нужно ввести пароль", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    /**
     * Обработчик клика на записи
     */
    private AdapterView.OnItemClickListener onRecordClicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showRecord(position);
        }
    };

    /**
     * Обработчик нажатия кнопки Назад
     */
    @Override
    public void onBackPressed() {
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (viewSwitcher.getDisplayedChild() == RECORD_DETAILS_VIEW_NUM) {
            viewSwitcher.showPrevious();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Обработчик создания системного меню
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Обработчик выбора пунктов системного меню
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Обработчик создания контекстного меню при долгом тапе на записи
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, OPEN_RECORD_MENU_ITEM_ID, Menu.NONE, "Открыть");
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPEN_RECORD_MENU_ITEM_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                showRecord(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

}

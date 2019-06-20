package com.gee12.mytetroid.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.ContextMenu;
import android.view.Gravity;
import android.view.View;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.FoundObject;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidNode;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.data.TetroidTag;
import com.gee12.mytetroid.views.FilesListAdapter;
import com.gee12.mytetroid.views.NodesListAdapter;
import com.gee12.mytetroid.views.ActivityDialogs;
import com.gee12.mytetroid.views.RecordsListAdapter;
import com.gee12.mytetroid.views.SearchViewListener;
import com.gee12.mytetroid.views.TagsListAdapter;
import com.google.android.material.navigation.NavigationView;

//import net.rdrei.android.dirchooser.DirectoryChooserActivity;
//import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import lib.folderpicker.FolderPicker;
import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListView;
import pl.openrnd.multilevellistview.OnItemClickListener;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    public static final int REQUEST_CODE_OPEN_DIRECTORY = 1;
    public static final int REQUEST_CODE_PERMISSION_REQUEST = 2;
    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 3;
    public static final int REQUEST_CODE_SEARCH_ACTIVITY = 4;

    public static final int OPEN_RECORD_MENU_ITEM_ID = 1;
    public static final int SHOW_FILES_MENU_ITEM_ID = 2;
    public static final int OPEN_RECORD_FOLDER_MENU_ITEM_ID = 3;
    public static final int VIEW_RECORDS_LIST = 0;
    public static final int VIEW_RECORD_TEXT = 1;
    public static final int VIEW_RECORD_FILES = 2;
    public static final int VIEW_TAG_RECORDS = 3;
    public static final int VIEW_found_RECORDS = 4;
//    public static final String[] VIEW_TYPE_TITLES = { "", ""};

    private DrawerLayout drawerLayout;
    private MultiLevelListView nodesListView;
    private NodesListAdapter nodesListAdapter;
    private RecordsListAdapter recordsListAdapter;
    private ListView recordsListView;
    private FilesListAdapter filesListAdapter;
    private TagsListAdapter tagsListAdapter;
    private ListView filesListView;
    private ListView tagsListView;
    private TetroidNode currentNode;
    private TetroidRecord currentRecord;
    private String currentTag;
    private ViewFlipper viewFlipper;
    private WebView recordContentWebView;
    private LinearLayout layoutProgress;
    ToggleButton tbRecordFieldsExpander;
    ExpandableLayout expRecordFieldsLayout;
    TextView tvRecordTags;
    TextView tvRecordAuthor;
    TextView tvRecordUrl;
    TextView tvRecordDate;
    TextView tvProgress;
    TextView tvAppTitle;
    TextView tvViewType;
    TextView tvNodesEmpty;
    TextView tvRecordsEmpty;
    TextView tvTagsEmpty;
    android.widget.SearchView nodesSearchView;
    android.widget.SearchView tagsSearchView;
    private int curViewId;
    private int lastViewId;
    private boolean isAlreadyTryDecrypt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.tvAppTitle = toolbar.findViewById(R.id.text_view_app_title);
        this.tvViewType = toolbar.findViewById(R.id.text_view_view_type);

        // панель
        this.drawerLayout = findViewById(R.id.drawer_layout);
        // задаем кнопку (стрелку) управления шторкой
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
//        drawerLayout.openDrawer(GravityCompat.START);
        toggle.syncState();

        // список веток
        nodesListView = findViewById(R.id.nodes_list_view);
        nodesListView.setOnItemClickListener(onNodeClickListener);
        this.tvNodesEmpty = findViewById(R.id.nodes_text_view_empty);
//        nodesListView.setEmptyView(tvNodesEmpty);
        // список записей
        this.recordsListView = findViewById(R.id.list_view_records);
        recordsListView.setOnItemClickListener(onRecordClicklistener);
        this.tvRecordsEmpty = findViewById(R.id.text_view_empty_records);
        recordsListView.setEmptyView(tvRecordsEmpty);
        registerForContextMenu(recordsListView);
        // список файлов
        this.filesListView = findViewById(R.id.list_view_files);
        filesListView.setOnItemClickListener(onFileClicklistener);
        TextView emptyTextView = findViewById(R.id.text_view_empty_files);
        filesListView.setEmptyView(emptyTextView);
        // список меток
        this.tagsListView = findViewById(R.id.tags_list_view);
        tagsListView.setOnItemClickListener(onTagClicklistener);
        this.tvTagsEmpty = findViewById(R.id.tags_text_view_empty);
        tagsListView.setEmptyView(tvTagsEmpty);

        this.viewFlipper = findViewById(R.id.view_flipper);
        this.layoutProgress = findViewById(R.id.layout_progress);
        this.tvProgress = findViewById(R.id.progress_text);
        this.recordContentWebView = findViewById(R.id.web_view_record_content);

        NavigationView nodesNavView = drawerLayout.findViewById(R.id.nav_view_left);
        View nodesHeader = nodesNavView.getHeaderView(0);
        this.nodesSearchView = nodesHeader.findViewById(R.id.search_view_nodes);
        nodesViewInit(nodesHeader);

        NavigationView tagsNavView = drawerLayout.findViewById(R.id.nav_view_right);
        View tagsHeader = tagsNavView.getHeaderView(0);
        this.tagsSearchView = tagsHeader.findViewById(R.id.search_view_tags);
        tagsViewInit(tagsHeader);

        this.tvRecordTags = findViewById(R.id.text_view_record_tags);
        this.tvRecordAuthor = findViewById(R.id.text_view_record_author);
        this.tvRecordUrl = findViewById(R.id.text_view_record_url);
        this.tvRecordDate = findViewById(R.id.text_view_record_date);
        this.expRecordFieldsLayout = findViewById(R.id.layout_expander);
        this.tbRecordFieldsExpander = findViewById(R.id.toggle_button_expander);
        tbRecordFieldsExpander.setOnCheckedChangeListener(this);

        // инициализация
        SettingsManager.init(this);
        LogManager.init(this, SettingsManager.getLogPath(), SettingsManager.isWriteLog());
        LogManager.addLog(String.format(getString(R.string.app_start), Utils.getVersionName(this)));
        startInitStorage();
    }

    private void nodesViewInit(View nodesHeader) {
        final TextView tvHeader = nodesHeader.findViewById(R.id.text_view_nodes_header);
        final ImageView ivIcon = nodesHeader.findViewById(R.id.image_view_app_icon);
        new SearchViewListener(nodesSearchView) {
            @Override
            public void OnClose() {
                nodesListAdapter.setDataItems(DataManager.getRootNodes());
                setListEmptyViewState(tvNodesEmpty, DataManager.getRootNodes().isEmpty(), R.string.nodes_is_missing);
                tvHeader.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.VISIBLE);
            }

            @Override
            public void OnSearch() {
                tvHeader.setVisibility(View.GONE);
                ivIcon.setVisibility(View.GONE);
            }

            @Override
            public void onQuerySubmit(String query) {
                LogManager.addLog(String.format(getString(R.string.search_nodes_by_query), query));
                List<TetroidNode> found = ScanManager.searchInNodesNames(
                        DataManager.getRootNodes(), query, false);
                nodesListAdapter.setDataItems(found);
                setListEmptyViewState(tvNodesEmpty, found.isEmpty(),
                        String.format(getString(R.string.nodes_not_found), query));
            }
        };
    }

    private void setListEmptyViewState(TextView tvEmpty, boolean isVisible, @StringRes int stringId) {
        tvEmpty.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        tvEmpty.setText(stringId);
    }

    private void setListEmptyViewState(TextView tvEmpty, boolean isVisible, String string) {
        tvEmpty.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        tvEmpty.setText(string);
    }

    private void tagsViewInit(View tagsHeader) {
        final TextView tvHeader = tagsHeader.findViewById(R.id.text_view_tags_header);
        new SearchViewListener(tagsSearchView) {
            @Override
            public void OnClose() {
//                TreeMap<String, List<TetroidRecord>> tags = DataManager.getTagsHashMap();
                TreeMap<String, TetroidTag> tags = DataManager.getTagsHashMap();
                tagsListAdapter.setDataItems(tags);
                if (tags.isEmpty())
                    tvTagsEmpty.setText(R.string.tags_is_missing);
                tvHeader.setVisibility(View.VISIBLE);
            }

            @Override
            public void OnSearch() {
                tvHeader.setVisibility(View.GONE);
            }

            @Override
            public void onQuerySubmit(String query) {
                LogManager.addLog(String.format(getString(R.string.search_tags_by_query), query));
//                TreeMap<String, List<TetroidRecord>> found = ScanManager.searchInTags(
                TreeMap<String, TetroidTag> found = ScanManager.searchInTags(
                        DataManager.getTagsHashMap(), query);
                tagsListAdapter.setDataItems(found);
                if (found.isEmpty())
                    tvTagsEmpty.setText(String.format(getString(R.string.tags_not_found), query));
            }
        };
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        expRecordFieldsLayout.toggle();
    }

    private void startInitStorage() {
        isAlreadyTryDecrypt = false;
        String storagePath = SettingsManager.getStoragePath();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (!checkReadExtStoragePermission()) {
                return;
            }
        }

        if (SettingsManager.isLoadLastStoragePath() && storagePath != null) {
            initStorage(storagePath);
        } else {
            showFolderChooser();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkReadExtStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else*/ {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                        REQUEST_CODE_PERMISSION_REQUEST);
            }
            return false;
        }
        return true;
    }

    private void initStorage(String storagePath) {
        if (DataManager.init(storagePath)) {
            LogManager.addLog(getString(R.string.storage_settings_inited) + storagePath);
            drawerLayout.openDrawer(Gravity.LEFT);
            // сохраняем путь к хранилищу, если загрузили его в первый раз
            if (SettingsManager.isLoadLastStoragePath()) {
                SettingsManager.setStoragePath(storagePath);
            }

            // нужно ли выделять ветку, выбранную в прошлый раз
            // (обязательно после initListViews)
            TetroidNode nodeToSelect = null;
            String nodeId = SettingsManager.getSelectedNodeId();
            if (nodeId != null) {
                nodeToSelect = DataManager.getNode(nodeId);
                // если нашли, отображаем
//                if (nodeToSelect != null) {
//                    showNode(nodeToSelect);
//                }
            }

            // проверка зашифрованы ли данные
//            if (DataManager.isExistsCryptedNodes()) {
            if (DataManager.isCrypted()) {

                if (SettingsManager.getWhenAskPass().equals(getString(R.string.pref_when_ask_password_on_start))) {
                    // спрашивать пароль при старте
                    decryptStorage(nodeToSelect);
                    return;
                }
            }

            initStorage(nodeToSelect, false);

        } else {
            LogManager.addLog(R.string.storage_init_error, Toast.LENGTH_SHORT);
        }
    }

    /**
     * Вызывается при:
     * 1) запуске приложения, если есть зашифрованные ветки и установлен isAskPasswordOnStart
     * 2) запуске приложения, если выделение было сохранено на зашифрованной ветке
     * 3) при выделении зашифрованной ветки
     * @param node Ветка для выбора при удачной расшифровке
     */
    private void decryptStorage(TetroidNode node) {
        String middlePassHash;
        // пароль сохранен локально?
        if (SettingsManager.isSaveMiddlePassHashLocal()
                && (middlePassHash = SettingsManager.getMiddlePassHash()) != null) {
            // проверяем
            try {
                if (DataManager.checkMiddlePassHash(middlePassHash)) {
                    decryptStorage(middlePassHash, true, node);
                } else {
                    LogManager.addLog(R.string.wrong_saved_pass, Toast.LENGTH_LONG);
                    if (!isAlreadyTryDecrypt) {
                        isAlreadyTryDecrypt = true;
                        initStorage(node, false);
                    }
                }
            } catch (DataManager.EmptyFieldException e) {
                // if middle_hash_check_data field is empty, so asking "decrypt anyway?"
               ActivityDialogs.showEmptyPassCheckingFieldDialog(this, e.getFieldName(), node, new ActivityDialogs.IPassCheckResult() {
                   @Override
                   public void onApply(TetroidNode node) {
                       decryptStorage(SettingsManager.getMiddlePassHash(), true, node);
                   }
               });
            }
        } else {
            showPassDialog(node);
        }
    }

    void showPassDialog(final TetroidNode node) {
        LogManager.addLog(R.string.show_pass_dialog);
        // выводим окно с запросом пароля в асинхронном режиме
        ActivityDialogs.showPassDialog(this, node, new ActivityDialogs.IPassInputResult() {
            @Override
            public void applyPass(final String pass, TetroidNode node) {
                // подтверждение введенного пароля
                try {
                    if (DataManager.checkPass(pass)) {
                        String passHash = CryptManager.passToHash(pass);
                        // сохраняем хэш пароля
                        if (SettingsManager.isSaveMiddlePassHashLocal())
                            SettingsManager.setMiddlePassHash(passHash);
                        else
                            CryptManager.setMiddlePassHash(passHash);

                        decryptStorage(pass, false, node);
                    } else {
                        LogManager.addLog(R.string.password_is_incorrect, Toast.LENGTH_LONG);
                        showPassDialog(node);
                    }
                } catch (DataManager.EmptyFieldException e) {
                    // если поля в INI-файле для проверки пустые
                    LogManager.addLog(e);
                    ActivityDialogs.showEmptyPassCheckingFieldDialog(MainActivity.this, e.getFieldName(), node, new ActivityDialogs.IPassCheckResult() {
                        @Override
                        public void onApply(TetroidNode node) {
                            decryptStorage(pass, false, node);
                            // пароль не сохраняем
                            // а спрашиваем нормально ли расшифровались данные, и потом сохраняем
                            // ...
                        }
                    });
                }
            }

            @Override
            public void cancelPass() {
                // загружаем хранилище как есть (только в первый раз, затем перезагружать не нужно)
                if (!isAlreadyTryDecrypt) {
                    isAlreadyTryDecrypt = true;
                    initStorage(node, false);
                }
            }
        });
    }

    private void decryptStorage(String pass, boolean isMiddleHash, TetroidNode nodeToSelect) {
        if (isMiddleHash)
            CryptManager.initFromMiddleHash(pass, DataManager.getInstance());
        else
            CryptManager.initFromPass(pass, DataManager.getInstance());
        initStorage(nodeToSelect, true);
    }

    private void initStorage(TetroidNode node, boolean isDecrypt) {
        if (isDecrypt && DataManager.isNodesExist()) {
            // расшифровываем зашифрованные ветки уже загруженного дерева
            if (DataManager.decryptAll()) {
                LogManager.addLog(R.string.storage_decrypted);
            } else {
                LogManager.addLog(getString(R.string.errors_during_decryption)
                    + (SettingsManager.isWriteLog()
                                ? getString(R.string.details_in_logs)
                                : getString(R.string.for_more_info_enable_log)),
                        Toast.LENGTH_LONG);
            }
            nodesListAdapter.notifyDataSetChanged();
            tagsListAdapter.onDataSetChanged();

            if (node != null)
                showNode(node);
        } else {
            LogManager.addLog(getString(R.string.start_storage_loading) +  DataManager.getStoragePath());
            new ReadStorageTask().execute(isDecrypt);
            // парсим дерево веток и расшифровываем зашифрованные
//            if (DataManager.readStorage(isDecrypt)) {
//                LogManager.addLog(R.string.storage_loaded);
//            }
            // инициализация контролов
//            initListViews();
        }
        // выбираем ветку в новом списке расшифрованных веток
//        if (node != null)
//            showNode(node);
    }


    private void initListViews() {
        // список веток
        this.nodesListAdapter = new NodesListAdapter(this, onNodeHeaderClickListener);
        nodesListView.setAdapter(nodesListAdapter);
        nodesListAdapter.setDataItems(DataManager.getRootNodes());
        setListEmptyViewState(tvNodesEmpty, DataManager.getRootNodes().isEmpty(), R.string.nodes_is_missing);
        // список записей
        this.recordsListAdapter = new RecordsListAdapter(this, onRecordAttachmentClickListener);
        recordsListView.setAdapter(recordsListAdapter);
        // список файлов
        this.filesListAdapter = new FilesListAdapter(this);
        filesListView.setAdapter(filesListAdapter);
        // список меток
        this.tagsListAdapter = new TagsListAdapter(this, DataManager.getTagsHashMap());
        tagsListView.setAdapter(tagsListAdapter);
        tvTagsEmpty.setText(R.string.tags_is_missing);
    }

    void showFolderChooser() {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra("title", getString(R.string.folder_chooser_title));
        intent.putExtra("location", SettingsManager.getStoragePath());
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    /**
     * Отображение ветки => список записей
     * @param node
     */
    private void showNode(TetroidNode node)
    {
        // проверка нужно ли расшифровать ветку перед отображением
        if (!node.isNonCryptedOrDecrypted()) {
            decryptStorage(node);
            // выходим, т.к. возможен запрос пароля в асинхронном режиме
            return;
        }
        this.currentNode = node;
        LogManager.addLog("Открытие записей ветки: id=" + node.getId());
//        tvRecordsEmpty.setText(R.string.records_is_missing);
        showRecords(node.getRecords(), VIEW_RECORDS_LIST);
    }

    private void showRecords(List<TetroidRecord> records, int viewId) {
        showView(viewId);
        if (viewId == VIEW_RECORDS_LIST)
            tvRecordsEmpty.setText(R.string.records_is_missing);
//        else
//            tvRecordsEmpty.setText(R.string.);
        drawerLayout.closeDrawers();

        this.recordsListAdapter.setDataItems(records);
        recordsListView.setAdapter(recordsListAdapter);
    }

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей ветки
     */
    private void showRecord(int position) {
        TetroidRecord record = (curViewId == VIEW_RECORDS_LIST)
                ? currentNode.getRecords().get(position)
                : DataManager.getTagsHashMap().get(currentTag).getRecord(position);
        showRecord(record);
    }

    /**
     * Отображение записи
     * @param record Запись
     */
    private void showRecord(final TetroidRecord record) {
        this.currentRecord = record;
        LogManager.addLog("Чтение записи: id=" + record.getId());
        String text = DataManager.getRecordHtmlTextDecrypted(record);
        if (text == null) {
            LogManager.addLog("Ошибка чтения записи", Toast.LENGTH_LONG);
            return;
        }
        recordContentWebView.loadDataWithBaseURL(DataManager.getRecordDirUri(record),
                text, "text/html", "UTF-8", null);
//            recordContentWebView.loadUrl(recordContentUrl);
        recordContentWebView.setWebViewClient(new WebViewClient() {
            /*@Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }*/
            @Override
            public void onPageFinished(WebView view, String url) {
                tvRecordTags.setText(record.getTagsString());
                tvRecordAuthor.setText(record.getAuthor());
                tvRecordUrl.setText(record.getUrl());
                if (record.getCreated() != null)
                    tvRecordDate.setText(record.getCreatedString(SettingsManager.getDateFormatString()));
                showView(VIEW_RECORD_TEXT);
            }
        });
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param position Индекс записи в списке записей ветки
     */
    private void showFilesList(int position) {
        TetroidRecord record = currentNode.getRecords().get(position);
        showFilesList(record);
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param record Запись
     */
    private void showFilesList(TetroidRecord record) {
        this.currentRecord = record;
        showView(VIEW_RECORD_FILES);
        this.filesListAdapter.reset(record);
        filesListView.setAdapter(filesListAdapter);
//        setTitle(record.getName());
    }

    /**
     * Открытие прикрепленного файла
     * @param position Индекс файла в списке прикрепленных файлов записи
     */
    private void openFile(int position) {
        if (currentRecord.isCrypted() && !SettingsManager.isDecryptFilesInTemp()) {
            LogManager.addLog(R.string.viewing_decrypted_not_possible, Toast.LENGTH_LONG);
            return;
        }
        TetroidFile file = currentRecord.getAttachedFiles().get(position);
        DataManager.openFile(this, currentRecord, file);
    }

    private void openRecordFolder(int position) {
        TetroidRecord record = currentNode.getRecords().get(position);
        openFolder(DataManager.getRecordDirUri(record));
    }

    public void openFolder(String pathUri){
        Uri uri = Uri.parse(pathUri);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
            startActivity(intent);
        } else {
            LogManager.addLog("Отсутствует файл менеджер", Toast.LENGTH_LONG);
        }
    }

    /**
     * Отображение записей по метке
     * @param position Индекс метки в списке
     */
    private void showTagRecords(int position) {
        this.currentNode = null;
        String tag = (String)tagsListAdapter.getItem(position);
        this.currentTag = tag;
        LogManager.addLog("Открытие записей метки: " + tag);
        showRecords(DataManager.getTagsHashMap().get(tag).getRecords(), VIEW_TAG_RECORDS);
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
     * Обработчик клика на иконке прикрепленных файлов записи
     */
    RecordsListAdapter.OnRecordAttachmentClickListener onRecordAttachmentClickListener = new RecordsListAdapter.OnRecordAttachmentClickListener() {
        @Override
        public void onClick(TetroidRecord record) {
            showFilesList(record);
        }
    };

    /**
     * Обработчик клика на "конечной" ветке (без подветок)
     */
    private OnItemClickListener onNodeClickListener = new OnItemClickListener() {

        /**
         * Клик на конечной ветке
         */
        @Override
        public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            showNode((TetroidNode)item);
        }

        /**
         * Клик на родительской ветке
         */
        @Override
        public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            // это событие обрабатывается с помощью OnNodeHeaderClickListener, чтобы разделить клик
            // на заголовке и на стрелке раскрытия/закрытия ветки
            TetroidNode node = (TetroidNode) item;
            if (!node.isNonCryptedOrDecrypted()) {
                decryptStorage(node);
                // как остановить дальнейшее выполнение, чтобы не стабатывал Expander?
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
     * Обработчик клика на прикрепленном файле
     */
    private AdapterView.OnItemClickListener onFileClicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            openFile(position);
        }
    };

    /**
     * Обработчик клика на метке
     */
    private AdapterView.OnItemClickListener onTagClicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showTagRecords(position);
        }
    };

    /**
     *
     * @param viewId
     */
    void showView(int viewId) {
        this.lastViewId = this.curViewId;
        int whichChild = viewId;
        if (viewId == VIEW_RECORDS_LIST) {
            setTitle((currentNode != null) ? currentNode.getName() : "");
        } else if (viewId == VIEW_RECORD_TEXT || viewId == VIEW_RECORD_FILES) {
            setTitle((currentRecord != null) ? currentRecord.getName() : "");
        } else if (viewId == VIEW_TAG_RECORDS) {
            setTitle((currentTag != null) ? currentTag : "");
            // один контрол на записи ветки и метки
            whichChild = VIEW_RECORDS_LIST;
        }
        setViewTypeTitle(viewId);
        this.curViewId = viewId;
        viewFlipper.setDisplayedChild(whichChild);
    }

    /**
     * Переопределяем заголовок активности
     * @param title
     */
    @Override
    public void setTitle(CharSequence title) {
        tvAppTitle.setText(title);
    }

    /**
     * Заголовок типа активности
     * @param viewId
     */
    private void setViewTypeTitle(int viewId) {
        String[] titles = getResources().getStringArray(R.array.view_type_titles);
        if (viewId >= 0 && viewId < titles.length)
            tvViewType.setText(titles[viewId]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            if (SettingsManager.isAskReloadStorage) {
                SettingsManager.isAskReloadStorage = false;
                ActivityDialogs.showReloadStorageDialog(this, new ActivityDialogs.IReloadStorageResult() {
                    @Override
                    public void onApply() {
                        startInitStorage();
                    }
                    @Override
                    public void onCancel() {
                    }
                });
            }
        } else if (requestCode == REQUEST_CODE_SEARCH_ACTIVITY && resultCode == RESULT_OK) {
            ScanManager scan = data.getParcelableExtra(SearchActivity.EXTRA_KEY_SCAN_MANAGER);
            startGlobalSearch(scan);

        } else if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == RESULT_OK) {
            String folderFullName = data.getStringExtra("data");
            initStorage(folderFullName);
        }
    }

    private void startGlobalSearch(ScanManager scan) {
        List<FoundObject> found = scan.globalSearch(/*DataManager.getInstance(), */currentNode);

        showActivity(this, NewMainActivity.class);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startInitStorage();
                } else {
                    LogManager.addLog(R.string.missing_read_ext_storage_permissions, Toast.LENGTH_SHORT);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            if (currentNode != null) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                LogManager.addLog(String.format(getString(R.string.search_records_by_query), currentNode.getName(), query));
                TetroidSuggestionProvider.SaveRecentQuery(this, query);
                List<TetroidRecord> found = ScanManager.searchInRecordsNames(currentNode.getRecords(), query, false);
                if (found.isEmpty())
//                    tvRecordsEmpty.setText(String.format(getString(R.string.records_not_found), query, currentNode.getName()));
                    tvRecordsEmpty.setText(String.format(getString(R.string.records_not_found), query, currentNode.getName()));
                showRecords(found, VIEW_found_RECORDS);
            } else {
                LogManager.addLog(R.string.records_search_select_node, Toast.LENGTH_LONG);
            }
        }
    }

    /**
     * Обработчик нажатия кнопки Назад
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else if (viewFlipper.getDisplayedChild() == VIEW_RECORD_TEXT) {
            if (lastViewId == VIEW_RECORDS_LIST)
                showView(VIEW_RECORDS_LIST);
            else
                showView(VIEW_TAG_RECORDS);
        } else if (viewFlipper.getDisplayedChild() == VIEW_RECORD_FILES) {
            // смотрим какая страница была перед этим
            if (lastViewId == VIEW_RECORD_TEXT)
                showView(VIEW_RECORD_TEXT);
            else if (lastViewId == VIEW_RECORDS_LIST)
                showView(VIEW_RECORDS_LIST);
            else
                showView(VIEW_TAG_RECORDS);
        } else if (SettingsManager.isConfirmAppExit()) {
            onExit();
        } else {
            super.onBackPressed();
        }
    }

    private void onExit() {
        ActivityDialogs.showExitDialog(this, new ActivityDialogs.IExitResult() {
            @Override
            public void onApply() {
                finish();
            }
        });
    }

    /**
     * Обработчик создания системного меню
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // меню
        getMenuInflater().inflate(R.menu.main, menu);
        // виджет поиска
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search_records).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                List<TetroidRecord> records = (currentNode != null) ? currentNode.getRecords() : new ArrayList<TetroidRecord>();
                showRecords(records, VIEW_RECORDS_LIST);
                return false;
            }
        });

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
        if (id == R.id.action_settings) {
            showActivityForResult(SettingsActivity.class, REQUEST_CODE_SETTINGS_ACTIVITY);
            return true;
        } else if (id == R.id.action_global_search) {
            showActivityForResult(SearchActivity.class, REQUEST_CODE_SEARCH_ACTIVITY);
            return true;
        } else if (id == R.id.action_about_app) {
            showActivity(this, AboutActivity.class);
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

        menu.add(Menu.NONE, OPEN_RECORD_MENU_ITEM_ID, Menu.NONE, R.string.show_record_content);
        menu.add(Menu.NONE, SHOW_FILES_MENU_ITEM_ID, Menu.NONE, R.string.show_attached_files);
        menu.add(Menu.NONE, OPEN_RECORD_FOLDER_MENU_ITEM_ID, Menu.NONE, R.string.open_record_folder);
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case OPEN_RECORD_MENU_ITEM_ID:
                showRecord(info.position);
                return true;
            case SHOW_FILES_MENU_ITEM_ID:
                showFilesList(info.position);
                return true;
            case OPEN_RECORD_FOLDER_MENU_ITEM_ID:
                openRecordFolder(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public static void showActivity(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        context.startActivity(intent);
    }

    public void showActivityForResult(Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent, requestCode);
    }

    /**
     *
     */
    private class ReadStorageTask extends AsyncTask<Boolean,Void,Boolean> {
        @Override
        protected void onPreExecute() {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            tvProgress.setText(R.string.storage_loading);
            layoutProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {
            boolean isDecrypt = booleans[0];
            return DataManager.readStorage(isDecrypt);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            drawerLayout.openDrawer(Gravity.LEFT);
            layoutProgress.setVisibility(View.INVISIBLE);
            if (res) {
                LogManager.addLog(getString(R.string.storage_loaded) + DataManager.getStoragePath());
            }
            // инициализация контролов
            initListViews();
        }
    }
}

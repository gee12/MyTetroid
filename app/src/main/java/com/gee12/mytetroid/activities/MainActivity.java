package com.gee12.mytetroid.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.ViewUtils;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.FoundType;
import com.gee12.mytetroid.data.ITetroidObject;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.data.SyncManager;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidNode;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.data.TetroidTag;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.views.AskDialogs;
import com.gee12.mytetroid.views.MainPagerAdapter;
import com.gee12.mytetroid.views.MainViewPager;
import com.gee12.mytetroid.views.NodesListAdapter;
import com.gee12.mytetroid.views.SearchViewListener;
import com.gee12.mytetroid.views.TagsListAdapter;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

import lib.folderpicker.FolderPicker;
import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListView;
import pl.openrnd.multilevellistview.OnItemClickListener;

//import android.widget.SearchView;

public class MainActivity extends AppCompatActivity implements IMainView, View.OnTouchListener {

    public static final int REQUEST_CODE_OPEN_STORAGE = 1;
    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 2;
    public static final int REQUEST_CODE_SEARCH_ACTIVITY = 3;
    public static final int REQUEST_CODE_SYNC_STORAGE = 4;
    public static final int REQUEST_CODE_PERMISSION_READ = 1;
    public static final int REQUEST_CODE_PERMISSION_WRITE = 2;
    public static final String EXTRA_CUR_NODE_IS_NOT_NULL = "EXTRA_CUR_NODE_IS_NOT_NULL";

    private DrawerLayout drawerLayout;
    private MultiLevelListView nodesListView;
    private NodesListAdapter nodesListAdapter;
    private TagsListAdapter tagsListAdapter;
    private ListView tagsListView;
    private TetroidNode curNode;
    private TetroidTag curTag;
    private LinearLayout layoutProgress;
    private TextView tvProgress;
    private TextView tvAppTitle;
    private TextView tvViewType;
    private TextView tvNodesEmpty;
    private TextView tvTagsEmpty;
    private View vNodesHeader;
    private View vTagsHeader;
    private android.widget.SearchView nodesSearchView;
    private android.widget.SearchView tagsSearchView;
    private boolean isRecordsFiltered;
    private SearchView recordsSearchView;
    private MenuItem miRecordsSearchView;
    private MenuItem miGlobalSearch;
    private MenuItem miStorageSync;
    private MenuItem miStorageInfo;
    private GestureDetectorCompat gestureDetector;
    private boolean isAlreadyTryDecrypt;
    private boolean isStorageLoaded;

    private MainPagerAdapter viewPagerAdapter;
    private MainViewPager viewPager;
    private PagerTabStrip titleStrip;
    private boolean isFullscreen;
    private boolean isStarted;
    private boolean isLoadStorageAfterSync;
    private TetroidFile tempOpenableFile;

    public MainActivity() {
    }

    public MainActivity(Parcel in) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // выдвигающиеся панели
        this.drawerLayout = findViewById(R.id.drawer_layout1);
        // задаем кнопку (стрелку) управления шторкой
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                // ?
//                if (drawerView == )
//                closeNodesSearchView();
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // обработчик нажатия на экране
        this.gestureDetector = new GestureDetectorCompat(this, new MyGestureListener());
        // обработчик нажатия на экране, когда ветка не выбрана
        drawerLayout.setOnTouchListener(this);

        // страницы (главная и найдено)
        this.viewPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), this, gestureDetector);
        this.viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(viewPagerAdapter);
//        viewPager.setGestureDetector(gestureDetector);

        this.titleStrip = viewPager.findViewById(R.id.pager_title_strip);
        setFoundPageVisibility(false);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                if (isStarted)
                    changeToolBarByPage(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

//        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
//        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
//        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        this.tvAppTitle = toolbar.findViewById(R.id.text_view_app_title);
        this.tvViewType = toolbar.findViewById(R.id.text_view_view_type);

        // список веток
        nodesListView = findViewById(R.id.nodes_list_view);
        nodesListView.setOnItemClickListener(onNodeClickListener);
        this.tvNodesEmpty = findViewById(R.id.nodes_text_view_empty);
//        nodesListView.setEmptyView(tvNodesEmpty);
        // список меток
        this.tagsListView = findViewById(R.id.tags_list_view);
        tagsListView.setOnItemClickListener(onTagClicklistener);
        this.tvTagsEmpty = findViewById(R.id.tags_text_view_empty);
        tagsListView.setEmptyView(tvTagsEmpty);

        this.layoutProgress = findViewById(R.id.layout_progress);
        this.tvProgress = findViewById(R.id.progress_text);

        NavigationView nodesNavView = drawerLayout.findViewById(R.id.nav_view_left);
        this.vNodesHeader = nodesNavView.getHeaderView(0);
        this.nodesSearchView = vNodesHeader.findViewById(R.id.search_view_nodes);
//        this.tvNodesHeader = nodesHeader.findViewById(R.id.text_view_nodes_header);
        initNodesView(nodesSearchView, vNodesHeader);

        NavigationView tagsNavView = drawerLayout.findViewById(R.id.nav_view_right);
        this.vTagsHeader = tagsNavView.getHeaderView(0);
        this.tagsSearchView = vTagsHeader.findViewById(R.id.search_view_tags);
//        this.tvTagsHeader = tagsHeader.findViewById(R.id.text_view_tags_header);
        initTagsView(vTagsHeader);

    }

    @Override
    public void onMainPageCreated() {
        // инициализация
        SettingsManager.init(this);
        setMenuItemsVisible();
        LogManager.init(this, SettingsManager.getLogPath(), SettingsManager.isWriteLog());
        LogManager.addLog(String.format(getString(R.string.app_start), Utils.getVersionName(this)));
        startInitStorage();
    }

    /**
     * Начало загрузки хранилища.
     */
    private void startInitStorage() {
        this.isStorageLoaded = false;
        this.isAlreadyTryDecrypt = false;
        String storagePath = SettingsManager.getStoragePath();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (!checkReadExtStoragePermission()) {
                return;
            }
        }

        if (SettingsManager.isLoadLastStoragePath() && storagePath != null) {
            initOrSyncStorage(storagePath);
        } else {
            showFolderChooser();
        }
    }

    /**
     * Перезагрузка хранилища (при изменении пути в настройках).
     */
    private void reinitStorage() {
        closeFoundFragment();
        viewPagerAdapter.getMainFragment().clearView();
        startInitStorage();
    }

    /**
     * Предоставление разрешения для чтения с внешней памяти.
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkReadExtStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION_READ);
            return false;
        }
        return true;
    }

    /**
     * Проверка нужно ли синхронизировать хранилище перед загрузкой.
     * @param storagePath
     */
    private void initOrSyncStorage(final String storagePath) {
        if (SettingsManager.isSyncStorage() && SettingsManager.isSyncBeforeInit()) {
            // спрашиваем о необходимости запуска синхронизации, если установлена опция
            if (SettingsManager.isAskBeforeSync()) {
                AskDialogs.showSyncRequestDialog(this, new AskDialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        MainActivity.this.isLoadStorageAfterSync = true;
                        startStorageSync(storagePath);
                    }

                    @Override
                    public void onCancel() {
                        initStorage(storagePath);
                    }
                });
            } else {
                this.isLoadStorageAfterSync = true;
                startStorageSync(storagePath);
            }
        } else {
            initStorage(storagePath);
        }
    }

    /**
     * Отправление запроса на синхронизацию стороннему приложению.
     * @param storagePath
     */
    private void startStorageSync(String storagePath) {
        String command = SettingsManager.getSyncCommand();
        Intent intent = SyncManager.createCommandSender(this, storagePath, command);

        LogManager.addLog(getString(R.string.start_storage_sync) + command);
        if (!SettingsManager.isNotRememberSyncApp()) {
            // использовать стандартный механизм запоминания используемого приложения
            startActivityForResult(intent, REQUEST_CODE_SYNC_STORAGE);
        } else { // или спрашивать постоянно
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.title_choose_sync_app)), REQUEST_CODE_SYNC_STORAGE);
        }
    }

    /**
     * Обработка результата синхронизации хранилища.
     * @param res
     */
    private void onSyncStorageFinish(boolean res) {
        final String storagePath = SettingsManager.getStoragePath();
        if (res) {
            LogManager.addLog(R.string.sync_successful, Toast.LENGTH_SHORT);
            if (isLoadStorageAfterSync)
                initStorage(storagePath);
            else {
                AskDialogs.showSyncDoneDialog(this, true, new AskDialogs.IApplyResult() {
                    @Override
                    public void onApply() {
                        initStorage(storagePath);
                    }
                });
            }
        } else {
            LogManager.addLog(getString(R.string.sync_failed), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            if (isLoadStorageAfterSync) {
                AskDialogs.showSyncDoneDialog(this, false, new AskDialogs.IApplyResult() {
                    @Override
                    public void onApply() {
                        initStorage(storagePath);
                    }
                });
            }
        }
        this.isLoadStorageAfterSync = false;
    }

    /**
     * Загрузка хранилища по указанному пути.
     * @param storagePath Путь хранилища
     */
    private void initStorage(String storagePath) {
        if (DataManager.init(this, storagePath)) {
            LogManager.addLog(getString(R.string.storage_settings_inited) + storagePath);
            drawerLayout.openDrawer(Gravity.LEFT);
            // сохраняем путь к хранилищу, если загрузили его в первый раз
            if (SettingsManager.isLoadLastStoragePath()) {
                SettingsManager.setStoragePath(storagePath);
            }
            // нужно ли выделять ветку, выбранную в прошлый раз
            // (обязательно после initGUI)
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
            LogManager.addLog(getString(R.string.failed_storage_init) + DataManager.getStoragePath(),
                    LogManager.Types.WARNING, Toast.LENGTH_LONG);
            drawerLayout.openDrawer(Gravity.LEFT);
            initGUI(false);
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
                AskDialogs.showEmptyPassCheckingFieldDialog(this, e.getFieldName(), node, new AskDialogs.IPassCheckResult() {
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

    /**
     * Отображения запроса пароля от хранилища.
     * @param node
     */
    void showPassDialog(final TetroidNode node) {
        LogManager.addLog(R.string.show_pass_dialog);
        // выводим окно с запросом пароля в асинхронном режиме
        AskDialogs.showPassDialog(this, node, new AskDialogs.IPassInputResult() {
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
                    AskDialogs.showEmptyPassCheckingFieldDialog(MainActivity.this, e.getFieldName(), node, new AskDialogs.IPassCheckResult() {
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
                // Если при первой загрузке хранилища
                // установлена текущей зашифрованная ветка (node),
                // и пароль не сохраняли, то нужно его спросить.
                // Но если пароль вводить отказались, то просто грузим хранилище как есть
                // (только в первый раз, затем перезагружать не нужно)
                if (!isAlreadyTryDecrypt && !isStorageLoaded) {
                    isAlreadyTryDecrypt = true;
                    initStorage(node, false);
                }
            }
        });
    }

    /**
     * Расшифровка хранилища.
     * @param pass
     * @param isMiddleHash
     * @param nodeToSelect
     */
    private void decryptStorage(String pass, boolean isMiddleHash, TetroidNode nodeToSelect) {
        if (isMiddleHash)
            CryptManager.initFromMiddleHash(pass, DataManager.getInstance());
        else
            CryptManager.initFromPass(pass, DataManager.getInstance());
        initStorage(nodeToSelect, true);
    }

    /**
     * Непосредственная расшифровка (если зашифровано) или чтение данных хранилища
     * @param node
     * @param isDecrypt
     */
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
            tagsListAdapter.setDataItems(DataManager.getTags());

            if (node != null)
                showNode(node);
        } else {
            LogManager.addLog(getString(R.string.start_storage_loading) + DataManager.getStoragePath());
            new MainActivity.ReadStorageTask().execute(isDecrypt);
            // парсим дерево веток и расшифровываем зашифрованные
//            if (DataManager.readStorage(isDecrypt)) {
//                LogManager.addLog(R.string.storage_loaded);
//            }
            // инициализация контролов
//            initGUI();
        }
        // выбираем ветку в новом списке расшифрованных веток
//        if (node != null)
//            showNode(node);
    }

    /**
     * Первоначальная инициализация списков веток, записей, файлов, меток
     */
    private void initGUI(boolean res) {
        List<TetroidNode> rootNodes = DataManager.getRootNodes();
        if (res && rootNodes != null) {
            // список веток
            this.nodesListAdapter = new NodesListAdapter(this, onNodeHeaderClickListener);
            nodesListView.setAdapter(nodesListAdapter);
            nodesListAdapter.setDataItems(rootNodes);
            boolean isEmpty = DataManager.getRootNodes().isEmpty();
            if (!isEmpty) {
                // списки записей, файлов
                viewPagerAdapter.getMainFragment().initListAdapters(this);

                // список меток
                this.tagsListAdapter = new TagsListAdapter(this, DataManager.getTags());
                tagsListView.setAdapter(tagsListAdapter);
                tvTagsEmpty.setText(R.string.tags_is_missing);
            }
            setListEmptyViewState(tvNodesEmpty, isEmpty, R.string.nodes_is_missing);
        } else {
            setListEmptyViewState(tvNodesEmpty, true, R.string.storage_load_error);
        }
        setMenuItemsAvailable(res);
    }

    private void setMenuItemsVisible() {
        ViewUtils.setVisibleIfNotNull(miStorageSync, SettingsManager.isSyncStorage());
    }

    private void setMenuItemsAvailable(boolean isAvailable) {
        int vis = (isAvailable) ? View.VISIBLE : View.INVISIBLE;
        nodesSearchView.setVisibility(vis);
        tagsSearchView.setVisibility(vis);
        ViewUtils.setEnabledIfNotNull(miGlobalSearch, isAvailable);
        ViewUtils.setEnabledIfNotNull(miStorageSync, isAvailable);
        ViewUtils.setEnabledIfNotNull(miStorageInfo, isAvailable);
        ViewUtils.setEnabledIfNotNull(miRecordsSearchView, isAvailable);
    }

    /**
     * Открытие активности для первоначального выбора пути хранилища в файловой системе.
     */
    void showFolderChooser() {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra("title", getString(R.string.folder_chooser_title));
        intent.putExtra("location", SettingsManager.getStoragePath());
        startActivityForResult(intent, REQUEST_CODE_OPEN_STORAGE);
    }


    boolean isNodeOpening = false;

    /**
     * Отображение записей ветки.
     * @param node
     */
    private void showNode(TetroidNode node) {
        if (node == null)
            return;
        // проверка нужно ли расшифровать ветку перед отображением
        if (!node.isNonCryptedOrDecrypted()) {
            decryptStorage(node);
            // выходим, т.к. возможен запрос пароля в асинхронном режиме
            return;
        }
        LogManager.addLog(getString(R.string.open_node_records) + node.getId());
        // сбрасываем фильтрацию при открытии ветки
        if (isRecordsFiltered && !recordsSearchView.isIconified()) {
            // сбрасываем SearchView;
            // но т.к. при этом срабатывает событие onClose, нужно избежать повторной загрузки
            // полного списка записей в его обработчике с помощью проверки isNodeOpening
            this.isNodeOpening = true;
//          recordsSearchView.onActionViewCollapsed();
            recordsSearchView.setQuery("", false);
            recordsSearchView.setIconified(true);
            this.isNodeOpening = false;
        }
        this.curNode = node;
        showRecords(node.getRecords(), MainPageFragment.VIEW_NODE_RECORDS, false);
    }

    /**
     * Отображение записей по метке.
     * @param position Индекс метки в списке
     */
    private void showTagRecords(int position) {
//        String tag = (String)tagsListAdapter.getItem(position);
        TetroidTag tag = (TetroidTag) tagsListAdapter.getItem(position);
        showTag(tag);
    }

    //    private void showTag(String tag) {
    private void showTag(TetroidTag tag) {
        this.curNode = null;
        this.curTag = tag;
        LogManager.addLog(getString(R.string.open_tag_records) + tag);
//        showRecords(DataManager.getTagsHashMap().get(tag).getRecords(), MainPageFragment.VIEW_TAG_RECORDS);
        showRecords(tag.getRecords(), MainPageFragment.VIEW_TAG_RECORDS);
    }

    @Override
    public void openTag(String tagName) {
        TetroidTag tag = DataManager.getTag(tagName);
        showTag(tag);
    }

    private void showRecords(List<TetroidRecord> records, int viewId) {
        showRecords(records, viewId, false);
    }

    private void showRecords(List<TetroidRecord> records, int viewId, boolean isFiltered) {
        this.isRecordsFiltered = isFiltered;
        drawerLayout.closeDrawers();
        viewPager.setCurrent(MainViewPager.PAGE_MAIN);
        viewPagerAdapter.getMainFragment().showRecords(records, viewId);
    }

    @Override
    public void openFolder(String pathUri) {
        DataManager.openFolder(this, pathUri);
    }

    @Override
    public void openRecord(TetroidRecord record) {
        viewPagerAdapter.getMainFragment().openRecord(record);
    }

    /**
     * Отрытие прикрепленного файла.
     * Если файл нужно расшифровать во временные каталог, спрашиваем разрешение
     * на запись во внешнее хранилище.
     * @param file
     */
    @Override
    public void openFile(TetroidFile file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // если файл нужно расшифровать во временный каталог, нужно разрешение на запись
            if (file.getRecord().isCrypted() && SettingsManager.isDecryptFilesInTemp()
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                this.tempOpenableFile = file;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION_WRITE);
                return;
            }
        }
        // расшифровываем без запроса разрешения во время выполнения, т.к. нужные разрешения
        // уже были выданы при установке приложения
        DataManager.openFile(this, file);
    }

    /**
     * Обработчик клика на заголовке ветки с подветками.
     */
    NodesListAdapter.OnNodeHeaderClickListener onNodeHeaderClickListener = new NodesListAdapter.OnNodeHeaderClickListener() {
        @Override
        public void onClick(TetroidNode node) {
            showNode(node);
        }
    };

    /**
     * Обработчик клика на "конечной" ветке (без подветок).
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
//                return;
            }
        }
    };

    /**
     * Обработчик клика на метке.
     */
    private AdapterView.OnItemClickListener onTagClicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showTagRecords(position);
        }
    };

    /**
     *
     * @param nodesHeader
     */
    private void initNodesView(final android.widget.SearchView searchView, View nodesHeader) {
        final TextView tvHeader = nodesHeader.findViewById(R.id.text_view_nodes_header);
        final ImageView ivIcon = nodesHeader.findViewById(R.id.image_view_app_icon);

//        int searchCloseButtonId = searchView.getContext().getResources()
//                .getIdentifier("android:id/search_close_btn", null, null);
//        ImageView clearButton = (ImageView) searchView.findViewById(searchCloseButtonId);
//        clearButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                EditText et = (EditText) findViewById(R.id.search_src_text);
//
//                //Clear the text from EditText view
//                et.setText("");
//
//                //Clear query
//                searchView.setQuery("", false);
//                //Collapse the action view
//                searchView.onActionViewCollapsed();
//                //Collapse the search widget
////                searchView.collapseActionView();
//            }
//        });

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
                searchInNodesNames(query);
            }
        };
    }

    private void searchInNodesNames(String query) {
        LogManager.addLog(String.format(getString(R.string.search_nodes_by_query), query));
        List<TetroidNode> found = ScanManager.searchInNodesNames(
                DataManager.getRootNodes(), query);
        nodesListAdapter.setDataItems(found);
        setListEmptyViewState(tvNodesEmpty, found.isEmpty(),
                String.format(getString(R.string.nodes_not_found), query));
    }

    private void closeSearchView(SearchView search) {
        search.setIconified(true);
        search.setQuery("", false);
    }

    private void setListEmptyViewState(TextView tvEmpty, boolean isVisible, @StringRes int stringId) {
        tvEmpty.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        tvEmpty.setText(stringId);
    }

    private void setListEmptyViewState(TextView tvEmpty, boolean isVisible, String string) {
        tvEmpty.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        tvEmpty.setText(string);
    }

    /**
     *
     * @param tagsHeader
     */
    private void initTagsView(View tagsHeader) {
        final TextView tvHeader = tagsHeader.findViewById(R.id.text_view_tags_header);
        new SearchViewListener(tagsSearchView) {
            @Override
            public void OnClose() {
                searchInTags(null, false);
                tvHeader.setVisibility(View.VISIBLE);
            }

            @Override
            public void OnSearch() {
                tvHeader.setVisibility(View.GONE);
            }

            @Override
            public void onQuerySubmit(String query) {
                searchInTags(query, true);
            }
        };
    }

    private void searchInTags(String query, boolean isSearch) {
        List<TetroidTag> tags;
        if (isSearch) {
            LogManager.addLog(String.format(getString(R.string.search_tags_by_query), query));
            tags = ScanManager.searchInTags(DataManager.getTags(), query);
        } else {
            tags = DataManager.getTags();
        }
        tagsListAdapter.setDataItems(tags);
        if (tags.isEmpty())
            tvTagsEmpty.setText((isSearch)
                    ? String.format(getString(R.string.tags_not_found), query)
                    : getString(R.string.tags_is_missing));
    }


    /**
     * Виджет поиска по записям/файлам/тексту.
     * @param menuItem
     */
    private void initRecordsSearchView(MenuItem menuItem) {
        this.recordsSearchView = (SearchView) menuItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        recordsSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        recordsSearchView.setIconifiedByDefault(true);

        recordsSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                // "сбрасываем" фильтрацию, но не для только что открытых веток
                // (т.к. при открытии ветки вызывается setIconified=false, при котором вызывается это событие,
                // что приводит к повторному открытию списка записей)
                if (!isNodeOpening) {
                    switch (viewPagerAdapter.getMainFragment().getCurViewId()) {
                        case MainPageFragment.VIEW_NODE_RECORDS:
                            if (curNode != null) {
                                showRecords(curNode.getRecords(), MainPageFragment.VIEW_NODE_RECORDS);
                            }
                            break;
                        case MainPageFragment.VIEW_TAG_RECORDS:
                            if (curTag != null) {
                                showRecords(curTag.getRecords(), MainPageFragment.VIEW_TAG_RECORDS);
                            }
                            break;
                        // пока по файлам не ищем
                   /* case MainPageFragment.VIEW_RECORD_FILES:
                        TetroidRecord curRecord = viewPagerAdapter.getMainFragment().getCurRecord();
                        if (curRecord != null) {
                            viewPagerAdapter.getMainFragment().showRecordFiles(curRecord);
                        }
                        break;*/
                        case MainPageFragment.VIEW_RECORD_TEXT:
                            // ?
                            break;
                    }
                }
                MainActivity.this.isRecordsFiltered = false;
                return false;
            }
        });
    }

    /**
     *
     * @param curPage
     */
    private void changeToolBarByPage(int curPage) {
        if (curPage == MainViewPager.PAGE_MAIN) {
            viewPagerAdapter.getMainFragment().restoreLastMainToolbarState();
            setRecordsSearchViewVisibility(true);
        } else {
            updateMainToolbar(MainPageFragment.VIEW_GLOBAL_FOUND, null);
        }
    }

//    String recordsSearchQuery;

    /**
     *
     * @param viewId
     */
    @Override
    public void updateMainToolbar(int viewId, String title) {
        boolean showRecordsSearch;

        switch (viewId) {
            case MainPageFragment.VIEW_GLOBAL_FOUND:
                title = getString(R.string.title_global_search);
                showRecordsSearch = false;
                break;
            case MainPageFragment.VIEW_NONE:
                title = null;
                showRecordsSearch = false;
                break;
            case MainPageFragment.VIEW_NODE_RECORDS:
                title = ((curNode != null) ? curNode.getName() : "");
                showRecordsSearch = true;
                break;
            case MainPageFragment.VIEW_TAG_RECORDS:
                title = ((curTag != null) ? curTag.getName() : "");
                showRecordsSearch = true;
                break;
//            case MainPageFragment.VIEW_FOUND_RECORDS:
//                showRecordsSearch = true;
//                break;
            case MainPageFragment.VIEW_RECORD_FILES:
            default:
                showRecordsSearch = false;
        }
        setTitle(title);
        setViewTypeTitle(viewId);
        setRecordsSearchViewVisibility(showRecordsSearch);
//        setRecordsSearchViewVisibility(showRecordsSearch, viewId);
    }

    /**
     * Установка заголовка активности.
     * @param title
     */
    @Override
    public void setTitle(CharSequence title) {
        tvAppTitle.setText(title);
    }

    /**
     * Установка заголовка типа активности.
     * @param viewId
     */
    private void setViewTypeTitle(int viewId) {
        String[] titles = getResources().getStringArray(R.array.view_type_titles);
        // преобразуем идентификатор view в индекс заголовка
        int titleId = viewId - 1;
        if (titleId >= 0 && titleId < titles.length) {
            tvViewType.setVisibility(View.VISIBLE);
            tvViewType.setText(titles[titleId]);
        }
        else if (titleId < 0) {
            tvViewType.setVisibility(View.GONE);
        }
    }

    public void setRecordsSearchViewVisibility(boolean isVisible) {
        miRecordsSearchView.setVisible(isVisible);
    }

    public void setFoundPageVisibility(boolean isVisible) {
        if (!isVisible)
            viewPager.setCurrent(MainViewPager.PAGE_MAIN);
        viewPager.setPagingEnabled(isVisible);
        titleStrip.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            // перезагружаем хранилище, если изменили путь
            if (SettingsManager.isAskReloadStorage) {
                SettingsManager.isAskReloadStorage = false;
                AskDialogs.showReloadStorageDialog(this, new AskDialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        reinitStorage();
                    }
                    @Override
                    public void onCancel() {
                    }
                });
            }
            // не гасим экран, если установили опцию
            checkKeepScreenOn(viewPagerAdapter.getMainFragment().getCurViewId());
            // скрываем пункт меню Синхронизация, если отключили
            ViewUtils.setVisibleIfNotNull(miStorageSync, SettingsManager.isSyncStorage());
        } else if (requestCode == REQUEST_CODE_SEARCH_ACTIVITY && resultCode == RESULT_OK) {
            ScanManager scan = data.getParcelableExtra(SearchActivity.EXTRA_KEY_SCAN_MANAGER);
            startGlobalSearch(scan);

        } else if (requestCode == REQUEST_CODE_OPEN_STORAGE && resultCode == RESULT_OK) {
            String folderFullName = data.getStringExtra("data");
            initOrSyncStorage(folderFullName);
        } else if (requestCode == REQUEST_CODE_SYNC_STORAGE) {
            onSyncStorageFinish(resultCode == RESULT_OK);
        }
    }

    /**
     * Запуск глобального поиска.
     * @param scan
     */
    private void startGlobalSearch(ScanManager scan) {
        new GlobalSearchTask(scan).execute();
    }

    /**
     * Открытие объекта из поисковой выдачи в зависимости от его типа.
     * @param found
     */
    @Override
    public void openFoundObject(ITetroidObject found) {
        switch (found.getType()) {
            case FoundType.TYPE_RECORD:
                openRecord((TetroidRecord)found);
                break;
            case FoundType.TYPE_FILE:
                viewPagerAdapter.getMainFragment().showRecordFiles(((TetroidFile)found).getRecord());
                break;
            case FoundType.TYPE_NODE:
                showNode((TetroidNode)found);
                break;
            case FoundType.TYPE_TAG:
                showTag((TetroidTag)found);
                break;
        }
        viewPager.setCurrent(MainViewPager.PAGE_MAIN);
    }

    @Override
    public void closeFoundFragment() {
        setFoundPageVisibility(false);
    }

    @Override
    public void openMainPage() {
        viewPager.setCurrent(MainViewPager.PAGE_MAIN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_READ: {
                if (permGranted) {
                    startInitStorage();
                } else {
                    LogManager.addLog(R.string.missing_read_ext_storage_permissions, Toast.LENGTH_SHORT);
                }
            } break;
            case REQUEST_CODE_PERMISSION_WRITE: {
                if (permGranted) {
                    openFile(tempOpenableFile);
                } else {
                    LogManager.addLog(R.string.missing_write_ext_storage_permissions, Toast.LENGTH_SHORT);
                }
            }
        }
    }

    /**
     * Вызывается при submit на RecordsSearchView (вместо пересоздания всей активности).
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        // search in main page
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            searchInMainPage(query);
        }
        super.onNewIntent(intent);
    }

    /**
     * Поиск по записям, меткам, файлам, тексту записи.
     * @param query
     */
    private void searchInMainPage(String query) {
        TetroidSuggestionProvider.SaveRecentQuery(this, query);
        searchInMainPage(query, viewPagerAdapter.getMainFragment().getCurViewId());
    }

    private void searchInMainPage(String query, int viewId) {
        switch (viewId) {
            case MainPageFragment.VIEW_NODE_RECORDS:
//                this.recordsSearchQuery = query;
                searchInNodeRecords(query);
                break;
            case MainPageFragment.VIEW_TAG_RECORDS:
//                this.recordsSearchQuery = query;
                searchInTagRecords(query);
                break;
            case MainPageFragment.VIEW_RECORD_FILES:
                searchInRecordFiles(query);
                break;
            case MainPageFragment.VIEW_RECORD_TEXT:
                searchInRecordText(query);
                break;
//            case MainPageFragment.VIEW_FOUND_RECORDS:
//                int lastVIewId = viewPagerAdapter.getMainFragment().getLastViewId();
//                if (viewId != lastVIewId)
//                    searchInMainPage(query, lastVIewId);
//                break;
        }
    }

    private void searchInNodeRecords(String query) {
        if (curNode != null) {
            searchInRecords(query, curNode.getRecords(), MainPageFragment.VIEW_NODE_RECORDS);
        } else {
            LogManager.addLog(R.string.records_search_select_node, Toast.LENGTH_LONG);
        }
    }

    private void searchInTagRecords(String query) {
        if (curTag != null) {
            searchInRecords(query, curTag.getRecords(), MainPageFragment.VIEW_TAG_RECORDS);
        } else {
            LogManager.addLog(R.string.records_search_select_tag, Toast.LENGTH_LONG);
        }
    }

    private void searchInRecords(String query, List<TetroidRecord> records, int viewId) {
        String log = (viewId == MainPageFragment.VIEW_NODE_RECORDS)
                ? String.format(getString(R.string.search_records_in_node_by_query), curNode.getName(), query)
                : String.format(getString(R.string.search_records_in_tag_by_query), curTag.getName(), query);
        LogManager.addLog(log);
        List<TetroidRecord> found = ScanManager.searchInRecordsNames(records, query);
//        showRecords(found, MainPageFragment.VIEW_FOUND_RECORDS);
        showRecords(found, viewId, true);
        if (found.isEmpty()) {
            String emptyText = (viewId == MainPageFragment.VIEW_NODE_RECORDS)
                    ? String.format(getString(R.string.records_in_node_not_found), query, curNode.getName())
                    : String.format(getString(R.string.records_in_tag_not_found), query, curTag.getName());
            viewPagerAdapter.getMainFragment().setRecordsEmptyViewText(emptyText);
        }
    }

    private void searchInRecordFiles(String query) {
        TetroidRecord curRecord = viewPagerAdapter.getMainFragment().getCurRecord();
        if (curRecord != null) {
            searchInFiles(query, curRecord);
        } else {
            LogManager.addLog(getString(R.string.cur_record_is_not_set), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    private void searchInFiles(String query, TetroidRecord record) {
        LogManager.addLog(String.format(getString(R.string.search_files_by_query), record.getName(), query));
        List<TetroidFile> found = ScanManager.searchInFiles(record.getAttachedFiles(), query);
        viewPagerAdapter.getMainFragment().showRecordFiles(found, record);
        if (found.isEmpty()) {
            viewPagerAdapter.getMainFragment().setFilesEmptyViewText(
                    String.format(getString(R.string.files_not_found), query));
        }
    }

    private void searchInRecordText(String query) {
        TetroidRecord curRecord = viewPagerAdapter.getMainFragment().getCurRecord();
        if (curRecord != null) {
            searchInText(query, curRecord);
        } else {
            LogManager.addLog(getString(R.string.cur_record_is_not_set), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Поиск по тексту записи.
     * @param query
     * @param record
     */
    private void searchInText(String query, TetroidRecord record) {
        //
        // TODO: реализовать поиск по тексту записи
        //
        LogManager.addLog(String.format(getString(R.string.search_text_by_query), record.getName(), query));
    }

    @Override
    public void checkKeepScreenOn(int curViewId) {
        setKeepScreenOn(curViewId == MainPageFragment.VIEW_RECORD_TEXT
            && SettingsManager.isKeepScreenOn());
    }

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else if (viewPager.getCurrentItem() == MainViewPager.PAGE_MAIN
                && !viewPagerAdapter.getMainFragment().onBackPressed()
            || viewPager.getCurrentItem() == MainViewPager.PAGE_FOUND
                && !viewPagerAdapter.getFoundFragment().onBackPressed()) {
            if (SettingsManager.isConfirmAppExit()) {
                onExit();
            } else {
                super.onBackPressed();
            }
        }
    }

    /**
     * Обработчик создания системного меню.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        viewPagerAdapter.getMainFragment().onCreateOptionsMenu(menu);
        this.miGlobalSearch = menu.findItem(R.id.action_global_search);
        this.miStorageSync = menu.findItem(R.id.action_storage_sync);
        ViewUtils.setVisibleIfNotNull(miStorageSync, SettingsManager.isSyncStorage());
        this.miStorageInfo = menu.findItem(R.id.action_storage_info);
        this.miRecordsSearchView = menu.findItem(R.id.action_search_records);
        initRecordsSearchView(miRecordsSearchView);
        //
        this.isStarted = true;
        return true;
    }

    /**
     * Обработчик выбора пунктов системного меню.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_cur_node:
                showNode(curNode);
                return true;
            case R.id.action_cur_record:
                viewPagerAdapter.getMainFragment().showCurRecord();
                return true;
            case R.id.action_attached_files:
                viewPagerAdapter.getMainFragment().showCurRecordFiles();
                return true;
            case R.id.action_cur_record_folder:
                viewPagerAdapter.getMainFragment().openRecordFolder();
                return true;
            case R.id.action_fullscreen:
                toggleFullscreen();
                return true;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, REQUEST_CODE_SETTINGS_ACTIVITY);
                return true;
            case R.id.action_global_search:
                showGlobalSearchActivity();
                return true;
            case R.id.action_storage_sync:
                startStorageSync(SettingsManager.getStoragePath());
                return true;
            case R.id.action_storage_info:
                showActivity(this, InfoActivity.class);
                return true;
            case R.id.action_about_app:
                showActivity(this, AboutActivity.class);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Переключатель полноэкранного режима.
     */
    @Override
    public void toggleFullscreen() {
        setFullscreen(!isFullscreen);
    }

    /**
     * Установка полноэкранного режима.
     * @param isFullscreen Если true, то toolbar исчезает и в опциях SystemUiVisibility устанавливаются нужные флаги
     *                     для полноэкранного режима, иначе все флаги сбрасываются.
     */
    private void setFullscreen(boolean isFullscreen) {
        this.isFullscreen = isFullscreen;

        // StatusBar
        View decorView = getWindow().getDecorView();
        int visibility = (isFullscreen)
                ? View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                : 0;
        decorView.setSystemUiVisibility(
                visibility);
        // ToolBar
        try {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                if (isFullscreen)
                    actionBar.hide();
                else
                    actionBar.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // панель с полями записи
        viewPagerAdapter.getMainFragment().setFullscreen(isFullscreen);
    }

    @Override
    public boolean isFullscreen() {
        return isFullscreen;
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэеранный режим.
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return false;
    }

    /**
     * Если потеряли фокус на активности, то выходим их полноэкранного режима
     * (например, при нажатии на "физическую" кнопку вызова меню).
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
//            setFullscreen(false);
        }
    }

    private void onExit() {
        AskDialogs.showExitDialog(this, new AskDialogs.IApplyResult() {
            @Override
            public void onApply() {
                finish();
            }
        });
    }

    public static void showActivity(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        context.startActivity(intent);
    }

    private void showGlobalSearchActivity() {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(EXTRA_CUR_NODE_IS_NOT_NULL, (curNode != null));
        startActivityForResult(intent, REQUEST_CODE_SEARCH_ACTIVITY);
    }

    public void showActivityForResult(Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    /**
     * Задание (параллельный поток), в котором выполняется загрузка хранилища.
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
                MainActivity.this.isStorageLoaded = true;
                LogManager.addLog(getString(R.string.storage_loaded) + DataManager.getStoragePath(), Toast.LENGTH_SHORT);
            } else {
                LogManager.addLog(getString(R.string.failed_storage_load) + DataManager.getStoragePath(),
                        LogManager.Types.WARNING, Toast.LENGTH_LONG);
            }
            // инициализация контролов
            initGUI(res);

//            if (BuildConfig.DEBUG) {
//                LogManager.addLog("is full version: " + App.isFullVersion(), Toast.LENGTH_LONG);
//            }
        }
    }

    /**
     * Задание, в котором выполняется глобальный поиск по объектам.
     */
    private class GlobalSearchTask extends AsyncTask<Void, Void,HashMap<ITetroidObject,FoundType>> {

        private ScanManager scan;

        GlobalSearchTask(ScanManager scan) {
            this.scan = scan;
        }

        @Override
        protected void onPreExecute() {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            tvProgress.setText(R.string.global_searching);
            layoutProgress.setVisibility(View.VISIBLE);

            LogManager.addLog(String.format(getString(R.string.global_search_start), scan.getQuery()));
        }

        @Override
        protected HashMap<ITetroidObject, FoundType> doInBackground(Void... voids /*ScanManager... scans*/) {
//            ScanManager scan = scans[0];
            return scan.globalSearch(curNode);
        }

        @Override
        protected void onPostExecute(HashMap<ITetroidObject,FoundType> found) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
//            drawerLayout.openDrawer(Gravity.LEFT);
            layoutProgress.setVisibility(View.INVISIBLE);
            if (found == null) {
                LogManager.addLog(getString(R.string.global_search_return_null), Toast.LENGTH_SHORT);
                return;
            } else if (scan.isSearchInNode() && scan.getNode() != null) {
                LogManager.addLog(String.format(getString(R.string.global_search_by_node_result),
                        scan.getNode().getName()), Toast.LENGTH_LONG);
            }
            // уведомляем, если не смогли поискать в зашифрованных ветках
            if (scan.isExistCryptedNodes()) {
                LogManager.addLog(R.string.found_crypted_nodes, Toast.LENGTH_SHORT);
            }
            LogManager.addLog(String.format(getString(R.string.global_search_end), found.size()));
            viewPagerAdapter.getFoundFragment().setFounds(found, scan);
            viewPagerAdapter.notifyDataSetChanged(); // для обновления title у страницы
            setFoundPageVisibility(true);
            viewPager.setCurrent(MainViewPager.PAGE_FOUND);
        }
    }

    /**
     * Обработчик двойного нажатия на экране.
     */
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            toggleFullscreen();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }
    }

    /**
     *
     */
    public static final Creator<MainActivity> CREATOR = new Creator<MainActivity>() {
        @Override
        public MainActivity createFromParcel(Parcel in) {
            return new MainActivity(in);
        }

        @Override
        public MainActivity[] newArray(int size) {
            return new MainActivity[size];
        }
    };
}

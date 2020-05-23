package com.gee12.mytetroid.activities;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.adapters.MainPagerAdapter;
import com.gee12.mytetroid.adapters.NodesListAdapter;
import com.gee12.mytetroid.adapters.TagsListAdapter;
import com.gee12.mytetroid.crypt.CryptManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.DatabaseConfig;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.data.SyncManager;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.AskDialogs;
import com.gee12.mytetroid.views.IntentDialog;
import com.gee12.mytetroid.views.MainViewPager;
import com.gee12.mytetroid.views.NodeAskDialogs;
import com.gee12.mytetroid.views.SearchViewListener;
import com.gee12.mytetroid.views.SearchViewXListener;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import lib.folderpicker.FolderPicker;
import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListView;
import pl.openrnd.multilevellistview.NestType;
import pl.openrnd.multilevellistview.OnItemClickListener;
import pl.openrnd.multilevellistview.OnItemLongClickListener;

//import android.widget.SearchView;

public class MainActivity extends TetroidActivity implements IMainView {

    public static final int REQUEST_CODE_OPEN_STORAGE = 1;
    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 2;
    public static final int REQUEST_CODE_RECORD_ACTIVITY = 3;
    public static final int REQUEST_CODE_SEARCH_ACTIVITY = 4;
    public static final int REQUEST_CODE_SYNC_STORAGE = 5;
    public static final int REQUEST_CODE_FILE_PICKER = 6;

    public static final int REQUEST_CODE_PERMISSION_WRITE_STORAGE = 1;
    public static final int REQUEST_CODE_PERMISSION_WRITE_TEMP = 2;
    public static final String EXTRA_CUR_NODE_IS_NOT_NULL = "EXTRA_CUR_NODE_IS_NOT_NULL";

    private DrawerLayout mDrawerLayout;
    private MultiLevelListView mListViewNodes;
    private NodesListAdapter mListAdapterNodes;
    private ListView mListViewTags;
    private TagsListAdapter mListAdapterTags;
    private TetroidNode mCurNode;
    private TetroidTag mCurTag;
    private LinearLayout mLayoutProgress;
    private TextView mTextViewProgress;
    private TextView mTextViewNodesEmpty;
    private TextView mTextViewTagsEmpty;
    private android.widget.SearchView mSearchViewNodes;
    private android.widget.SearchView mSearchViewTags;
    private boolean mIsRecordsFiltered;
    private SearchView mSearchViewRecords;
    private MenuItem mMenuItemSearchViewRecords;
    private MenuItem mMenuItemGlobalSearch;
    private MenuItem mMenuItemStorageSync;
    private MenuItem mMenuItemStorageInfo;
    private boolean mIsAlreadyTryDecrypt;
    private boolean mIsStorageLoaded;

    private Intent mReceivedIntent;
    private MainPagerAdapter mViewPagerAdapter;
    private MainViewPager mViewPager;
    private PagerTabStrip mTitleStrip;
    private boolean mIsActivityCreated;
    private boolean mIsLoadStorageAfterSync;
    private TetroidFile mTempFileToOpen;
    private boolean isNodeOpening = false;


    public MainActivity() {
        super();
    }

    public MainActivity(Parcel in) {
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mReceivedIntent = getIntent();

        // выдвигающиеся панели
        this.mDrawerLayout = findViewById(R.id.drawer_layout);
        // задаем кнопку (стрелку) управления шторкой
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                // ?
//                if (drawerView == )
//                closeNodesSearchView();
            }
        };
        mDrawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // обработчик нажатия на экране, когда ветка не выбрана
        mDrawerLayout.setOnTouchListener(this);

        // страницы (главная и найдено)
        this.mViewPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), this, gestureDetector);
        this.mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(mViewPagerAdapter);
//        mViewPager.setGestureDetector(mGestureDetector);

        this.mTitleStrip = mViewPager.findViewById(R.id.pager_title_strip);
        setFoundPageVisibility(false);
        mViewPager.addOnPageChangeListener(new ViewPagerListener());

//        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
//        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
//        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        // список веток
        mListViewNodes = findViewById(R.id.list_view_nodes);
        mListViewNodes.setOnItemClickListener(onNodeClickListener);
        mListViewNodes.setOnItemLongClickListener(onNodeLongClickListener);
//        registerForContextMenu(mListViewNodes.getListView());
        this.mTextViewNodesEmpty = findViewById(R.id.nodes_text_view_empty);
//        mListViewNodes.setEmptyView(mTextViewNodesEmpty);
        // список меток
        this.mListViewTags = findViewById(R.id.tags_list_view);
        mListViewTags.setOnItemClickListener(onTagClicklistener);
        mListViewTags.setOnItemLongClickListener(onTagLongClicklistener);
        this.mTextViewTagsEmpty = findViewById(R.id.tags_text_view_empty);
        mListViewTags.setEmptyView(mTextViewTagsEmpty);

        this.mLayoutProgress = findViewById(R.id.layout_progress);
        this.mTextViewProgress = findViewById(R.id.progress_text);

        NavigationView nodesNavView = mDrawerLayout.findViewById(R.id.nav_view_left);
        View vNodesHeader = nodesNavView.getHeaderView(0);
        this.mSearchViewNodes = vNodesHeader.findViewById(R.id.search_view_nodes);
//        this.tvNodesHeader = nodesHeader.findViewById(R.id.text_view_nodes_header);
        initNodesView(mSearchViewNodes, vNodesHeader);

        NavigationView tagsNavView = mDrawerLayout.findViewById(R.id.nav_view_right);
        View vTagsHeader = tagsNavView.getHeaderView(0);
        this.mSearchViewTags = vTagsHeader.findViewById(R.id.search_view_tags);
//        this.tvTagsHeader = tagsHeader.findViewById(R.id.text_view_tags_header);
        initTagsView(vTagsHeader);

    }

    @Override
    public void onMainPageCreated() {
        // инициализация
        SettingsManager.init(this);
        mViewPagerAdapter.getMainFragment().onSettingsInited();
        setMenuItemsVisible();
        LogManager.init(this, SettingsManager.getLogPath(), SettingsManager.isWriteLogToFile());
        LogManager.addLog(String.format(getString(R.string.log_app_start_mask), Utils.getVersionName(this)));
        startInitStorage();
    }

    /**
     * Начало загрузки хранилища.
     */
    private void startInitStorage() {
        this.mIsStorageLoaded = false;
        this.mIsAlreadyTryDecrypt = false;
        String storagePath = SettingsManager.getStoragePath();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkWriteExtStoragePermission()) {
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
     * Обработчик события после загрузки хранилища.
     */
    private void afterStorageInited() {
        checkReceivedIntent(mReceivedIntent);
    }

    /**
     * Перезагрузка хранилища (при изменении пути в настройках).
     */
    private void reinitStorage() {
        closeFoundFragment();
        mViewPagerAdapter.getMainFragment().clearView();
        startInitStorage();
    }

    /**
     * Проверка разрешения на запись во внешнюю память.
     * @return
     */
//    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkWriteExtStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // нужно объяснить пользователю зачем нужно разрешение
                AskDialogs.showRequestWriteExtStorageDialog(this, () -> requestWriteExtStorage());
            } else {
                requestWriteExtStorage();
            }
            return false;
        }
        return true;
    }

    /**
     * Запрос разрешения на запись во внешнюю память.
     * @return
     */
    private void requestWriteExtStorage() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                REQUEST_CODE_PERMISSION_WRITE_STORAGE);
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
                        MainActivity.this.mIsLoadStorageAfterSync = true;
                        startStorageSync(storagePath);
                    }

                    @Override
                    public void onCancel() {
                        initStorage(storagePath);
                    }
                });
            } else {
                this.mIsLoadStorageAfterSync = true;
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

        LogManager.addLog(getString(R.string.log_start_storage_sync) + command);
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
            LogManager.addLog(R.string.log_sync_successful, Toast.LENGTH_SHORT);
            if (mIsLoadStorageAfterSync)
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
            LogManager.addLog(getString(R.string.log_sync_failed), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            if (mIsLoadStorageAfterSync) {
                AskDialogs.showSyncDoneDialog(this, false, new AskDialogs.IApplyResult() {
                    @Override
                    public void onApply() {
                        initStorage(storagePath);
                    }
                });
            }
        }
        this.mIsLoadStorageAfterSync = false;
    }

    /**
     * Загрузка хранилища по указанному пути.
     * @param storagePath Путь хранилища
     */
    private void initStorage(String storagePath) {
        if (DataManager.init(this, storagePath)) {
            LogManager.addLog(getString(R.string.log_storage_settings_inited) + storagePath);
            mDrawerLayout.openDrawer(Gravity.LEFT);
            // сохраняем путь к хранилищу, если загрузили его в первый раз
            if (SettingsManager.isLoadLastStoragePath()) {
                SettingsManager.setStoragePath(storagePath);
            }
            // нужно ли выделять ветку, выбранную в прошлый раз
            // (обязательно после initGUI)
//            TetroidNode nodeToSelect = null;
  /*          String nodeId = SettingsManager.getSelectedNodeId();
            if (nodeId != null) {
                nodeToSelect = DataManager.getNode(nodeId);
                // если нашли, отображаем
//                if (nodeToSelect != null) {
//                    showNode(nodeToSelect);
//                }
            }*/
            // Если хранилище зашифровано, то пытаемся расшифровать его сразу, если:
            // 1) хэш пароля сохранен локально
            // или
            // 2) установлена опция "Спрашивать пароль при старте"
//            if (DataManager.isExistsCryptedNodes()) {

            /*if (DataManager.isCrypted()
                    && (SettingsManager.isSaveMiddlePassHashLocal()
                    || SettingsManager.getWhenAskPass().equals(getString(R.string.pref_when_ask_password_on_start)))) {
                    decryptStorage(nodeToSelect);
            } else {
                // иначе просто загружаем хранилище, даже если оно зашифровано
                initStorage(nodeToSelect, false);
            }*/
            if (DataManager.isCrypted()) {
                decryptStorage();
            } else {
                initStorage(null, false);
            }
        } else {
            LogManager.addLog(getString(R.string.log_failed_storage_init) + DataManager.getStoragePath(),
                    LogManager.Types.WARNING, Toast.LENGTH_LONG);
            mDrawerLayout.openDrawer(Gravity.LEFT);
            initGUI(false);
        }
    }

    /**
     * Вызывается при:
     * 1) запуске приложения, если есть зашифрованные ветки и установлен isAskPasswordOnStart
     * 2) --запуске приложения, если выделение было сохранено на зашифрованной ветке
     * 3) --при выделении зашифрованной ветки
     */
    private void decryptStorage() {
        String middlePassHash;
        // пароль сохранен локально?
        if (SettingsManager.isSaveMiddlePassHashLocal()
                && (middlePassHash = SettingsManager.getMiddlePassHash()) != null) {
            // проверяем
            try {
                if (DataManager.checkMiddlePassHash(middlePassHash)) {
//                    decryptStorage(middlePassHash, true, null);
                    DataManager.initCryptPass(middlePassHash, true);
                    initStorage(null, true);
                } else {
                    LogManager.addLog(R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                    if (!mIsAlreadyTryDecrypt) {
                        mIsAlreadyTryDecrypt = true;
                        initStorage(null, false);
                    }
                }
            } catch (DatabaseConfig.EmptyFieldException ex) {
                // если поля в INI-файле для проверки пустые
                // спрашиваем "continue anyway?"
                AskDialogs.showEmptyPassCheckingFieldDialog(this, ex.getFieldName(),
//                        node -> decryptStorage(SettingsManager.getMiddlePassHash(), true, null));
                        () -> {
                            DataManager.initCryptPass(middlePassHash, true);
                            initStorage(null, true);
                        });
            }
        } else if (SettingsManager.getWhenAskPass().equals(getString(R.string.pref_when_ask_password_on_start))) {
            // спрашиваем пароль, если нужно расшифровывать на старте
            askPassword(null);
        } else {
            // просто загружаем без расшифровки, если не сохранен пароль и его не нужно спрашивать на старте
            initStorage(null, false);
        }
    }

    /**
     * Отображения запроса пароля от хранилища.
     * @param node
     */
    void askPassword(final TetroidNode node) {
        LogManager.addLog(R.string.log_show_pass_dialog);
        // выводим окно с запросом пароля в асинхронном режиме
        AskDialogs.showPassEnterDialog(this, node, false, new AskDialogs.IPassInputResult() {
            @Override
            public void applyPass(final String pass, TetroidNode node) {
                // подтверждение введенного пароля
                DataManager.checkPass(MainActivity.this, pass, () -> {
                    DataManager.setStoragePass(pass);
                    initStorage(node, true);
                }, R.string.log_pass_is_incorrect);

                /*try {
                    if (DataManager.checkPass(pass)) {
                        String passHash = CryptManager.passToHash(pass);
                        // сохраняем хэш пароля
                        if (SettingsManager.isSaveMiddlePassHashLocal())
                            SettingsManager.setMiddlePassHash(passHash);
                        else
                            CryptManager.setMiddlePassHash(passHash);

//                        decryptStorage(pass, false, node);
                        DataManager.initCryptPass(pass, false);
                        initStorage(node, true);
                    } else {
                        LogManager.addLog(R.string.log_pass_is_incorrect, Toast.LENGTH_LONG);
                        askPassword(node);
                    }
                } catch (DataManager.EmptyFieldException e) {
                    // если поля в INI-файле для проверки пустые
                    LogManager.addLog(e);
                    // спрашиваем "continue anyway?"
                    AskDialogs.showEmptyPassCheckingFieldDialog(MainActivity.this, e.getFieldName(), () -> {
                        // TODO: пароль не сохраняем
                        //  а спрашиваем нормально ли расшифровались данные, и потом сохраняем
                        *//*String passHash = CryptManager.passToHash(pass);
                        if (SettingsManager.isSaveMiddlePassHashLocal())
                            SettingsManager.setMiddlePassHash(passHash);
                        else
                            CryptManager.setMiddlePassHash(passHash);*//*

//                        decryptStorage(pass, false, node1);
                        DataManager.initCryptPass(pass, false);
                        initStorage(node, true);
                    });
                }*/
            }

            @Override
            public void cancelPass() {
                // Если при первой загрузке хранилища установлена текущей зашифрованная ветка (node),
                // и пароль не сохраняли, то нужно его спросить.
                // Но если пароль вводить отказались, то просто грузим хранилище как есть
                // (только в первый раз, затем перезагружать не нужно)
                if (!mIsAlreadyTryDecrypt && !mIsStorageLoaded) {
                    mIsAlreadyTryDecrypt = true;
                    initStorage(node, false);
                }
            }
        });
    }

    /**
     * Расшифровка хранилища.
     * @param pass
     * @param isMiddleHash
     * @param nodeToSelect Ветка для выбора при удачной расшифровке
     */
    /*private void decryptStorage(String pass, boolean isMiddleHash, TetroidNode nodeToSelect) {
        if (isMiddleHash) {
            CryptManager.initFromMiddleHash(pass, DataManager.getInstance());
        } else {
            CryptManager.initFromPass(pass, DataManager.getInstance());
        }
        initStorage(nodeToSelect, true);
    }*/

    /**
     * Непосредственная расшифровка (если зашифровано) или чтение данных хранилища.
     * @param node
     * @param isDecrypt
     */
    private void initStorage(TetroidNode node, boolean isDecrypt) {
        if (isDecrypt && DataManager.isNodesExist()) {
            // расшифровываем зашифрованные ветки уже загруженного дерева
            if (DataManager.decryptStorage()) {
                LogManager.addLog(R.string.log_storage_decrypted);
            } else {
                LogManager.addLog(getString(R.string.log_errors_during_decryption), Toast.LENGTH_LONG);
            }
            mListAdapterNodes.notifyDataSetChanged();
            mListAdapterTags.setDataItems(DataManager.getTags());

            if (node != null) {
                showNode(node);
            }
        } else {
            LogManager.addLog(getString(R.string.log_start_storage_loading) + DataManager.getStoragePath());
            new MainActivity.ReadStorageTask().execute(isDecrypt);
        }
    }

    /**
     * Первоначальная инициализация списков веток, записей, файлов, меток
     * @param res Результат загрузки хранилища.
     */
    private void initGUI(boolean res) {
        // добавляем к результату загрузки проверку на пустоту списка веток
        List<TetroidNode> rootNodes = DataManager.getRootNodes();
        res = (res && rootNodes != null);
        if (res) {
            boolean isEmpty = rootNodes.isEmpty();
            // список веток
            this.mListAdapterNodes = new NodesListAdapter(this, onNodeHeaderClickListener);
            mListViewNodes.setAdapter(mListAdapterNodes);
//            mListAdapterNodes.setDataItems(rootNodes);

            // выбираем ветку, выбранную в прошлый раз
            boolean nodesAdapterInited = false;
            TetroidNode nodeToSelect = null;
            if (SettingsManager.isKeepSelectedNode() && !isEmpty) {
                String nodeId = SettingsManager.getSelectedNodeId();
                if (nodeId != null) {
                    nodeToSelect = DataManager.getNode(nodeId);
                    if (nodeToSelect != null) {
//                        mListAdapterNodes.extendNode();
                        Stack<TetroidNode> expandNodes = DataManager.createNodesHierarchy(nodeToSelect);
                        mListAdapterNodes.setDataItems(rootNodes, expandNodes);
                        nodesAdapterInited = true;
                    }
                }
            }
            if (!nodesAdapterInited) {
                mListAdapterNodes.setDataItems(rootNodes);
            }

            if (!isEmpty) {
                // списки записей, файлов
                mViewPagerAdapter.getMainFragment().initListAdapters(this);
                if (nodeToSelect != null) {
                    showNode(nodeToSelect);
                }

                // список меток
                this.mListAdapterTags = new TagsListAdapter(this, DataManager.getTags());
                mListViewTags.setAdapter(mListAdapterTags);
                mTextViewTagsEmpty.setText(R.string.log_tags_is_missing);
            }
            setListEmptyViewState(mTextViewNodesEmpty, isEmpty, R.string.title_nodes_is_missing);
        } else {
            setListEmptyViewState(mTextViewNodesEmpty, true, R.string.log_storage_load_error);
        }
        setMenuItemsAvailable(res);
    }

    private void setMenuItemsVisible() {
        ViewUtils.setVisibleIfNotNull(mMenuItemStorageSync, SettingsManager.isSyncStorage());
    }

    private void setMenuItemsAvailable(boolean isAvailable) {
        int vis = (isAvailable) ? View.VISIBLE : View.INVISIBLE;
        mSearchViewNodes.setVisibility(vis);
        mSearchViewTags.setVisibility(vis);
        ViewUtils.setEnabledIfNotNull(mMenuItemGlobalSearch, isAvailable);
        ViewUtils.setEnabledIfNotNull(mMenuItemStorageSync, isAvailable);
        ViewUtils.setEnabledIfNotNull(mMenuItemStorageInfo, isAvailable);
        ViewUtils.setEnabledIfNotNull(mMenuItemSearchViewRecords, isAvailable);
    }

    /**
     * Открытие активности для первоначального выбора пути хранилища в файловой системе.
     */
    private void showFolderChooser() {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra("title", getString(R.string.title_storage_folder));
        intent.putExtra("location", SettingsManager.getStoragePath());
        startActivityForResult(intent, REQUEST_CODE_OPEN_STORAGE);
    }

    /**
     * Отображение записей ветки по Id.
     * @param nodeId
     */
    public void showNode(String nodeId) {
        TetroidNode node = DataManager.getNode(nodeId);
        if (node != null) {
            showNode(node);
        } else {
            LogManager.addLog(getString(R.string.log_not_found_node_id) + nodeId, LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Отображение записей ветки.
     * @param node
     */
    private void showNode(TetroidNode node) {
        if (node == null)
            return;
        // проверка нужно ли расшифровать ветку перед отображением
        if (!node.isNonCryptedOrDecrypted()) {
//            decryptStorage(node);
            askPassword(node);
            // выходим, т.к. запрос пароля будет в асинхронном режиме
            return;
        }
        LogManager.addLog(getString(R.string.log_open_node) + node.getId());
        // сбрасываем фильтрацию при открытии ветки
        if (mIsRecordsFiltered && !mSearchViewRecords.isIconified()) {
            // сбрасываем SearchView;
            // но т.к. при этом срабатывает событие onClose, нужно избежать повторной загрузки
            // полного списка записей в его обработчике с помощью проверки isNodeOpening
            this.isNodeOpening = true;
//          mSearchViewRecords.onActionViewCollapsed();
            mSearchViewRecords.setQuery("", false);
            mSearchViewRecords.setIconified(true);
            this.isNodeOpening = false;
        }
        this.mCurNode = node;
        mViewPagerAdapter.getMainFragment().setCurNode(node);
        showRecords(node.getRecords(), MainPageFragment.MAIN_VIEW_NODE_RECORDS, false);
    }

    /**
     * Отображение записей по метке.
     * @param position Индекс метки в списке
     */
    private void showTagRecords(int position) {
//        String tag = (String)mListAdapterTags.getItem(position);
//        TetroidTag tag = (TetroidTag) mListAdapterTags.getItem(position);
        TetroidTag tag = mListAdapterTags.getItem(position).getValue();
        showTag(tag);
    }

    private void showTag(TetroidTag tag) {
        if (tag == null) {
            LogManager.addLog("Переданная метка пуста (null)", LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        this.mCurNode = null;
        this.mCurTag = tag;
        LogManager.addLog(getString(R.string.log_open_tag_records) + tag);
        showRecords(tag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS);
    }

    private void showRecords(List<TetroidRecord> records, int viewId) {
        showRecords(records, viewId, false);
    }

    private void showRecords(List<TetroidRecord> records, int viewId, boolean isFiltered) {
        this.mIsRecordsFiltered = isFiltered;
        mDrawerLayout.closeDrawers();
        mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
        mViewPagerAdapter.getMainFragment().showRecords(records, viewId);
    }

    @Override
    public void openRecordFolder(TetroidRecord record) {
        DataManager.openRecordFolder(this, record);
    }

    @Override
    public void openRecord(TetroidRecord record) {
        openRecord(record.getId());
    }

    @Override
    public void updateTags() {
        mListAdapterTags.setDataItems(DataManager.getTags());
    }

    @Override
    public void updateNodes() {
        mListAdapterNodes.notifyDataSetChanged();
    }

    public void openRecord(String recordId) {
        Bundle bundle = new Bundle();
        bundle.putString(RecordActivity.EXTRA_OBJECT_ID, recordId);
        openRecord(bundle);
    }

    public void openRecord(TetroidRecord record, ArrayList<Uri> imagesUri) {
        Bundle bundle = new Bundle();
        bundle.putString(RecordActivity.EXTRA_OBJECT_ID, record.getId());
        bundle.putParcelableArrayList(RecordActivity.EXTRA_IMAGES_URI, imagesUri);
        openRecord(bundle);
    }

    public void openRecord(Bundle bundle) {
        ViewUtils.startActivity(this, RecordActivity.class, bundle, Intent.ACTION_MAIN, 0,
                REQUEST_CODE_RECORD_ACTIVITY);
    }

    /**
     * Отрытие прикрепленного файла.
     * Если файл нужно расшифровать во временные каталог, спрашиваем разрешение
     * на запись во внешнее хранилище.
     *
     * FIXME: Разрешение WRITE_EXTERNAL_STORAGE уже просить не нужно, т.к. оно запрашивается при запуске
     *
     * @param file
     */
    @Override
    public void openAttach(TetroidFile file) {
//        if (Build.VERSION.SDK_INT >= 23) {
            // если файл нужно расшифровать во временный каталог, нужно разрешение на запись
            if (file.getRecord().isCrypted() && SettingsManager.isDecryptFilesInTemp()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                this.mTempFileToOpen = file;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION_WRITE_TEMP);
                return;
            }
//        }
        // расшифровываем без запроса разрешения во время выполнения, т.к. нужные разрешения
        // уже были выданы при установке приложения
        DataManager.openAttach(this, file);
    }

    /**
     * Обработчик клика на заголовке ветки с подветками.
     */
    NodesListAdapter.OnNodeHeaderClickListener onNodeHeaderClickListener = new NodesListAdapter.OnNodeHeaderClickListener() {
        @Override
        public void onClick(TetroidNode node) {
            showNode(node);
        }

        @Override
        public boolean onLongClick(View view, TetroidNode node, int pos) {
            showNodePopupMenu(view, node, pos);
            return true;
        }
    };

    /**
     * Обработчик клика на "конечной" ветке (без подветок).
     */
    private OnItemClickListener onNodeClickListener = new OnItemClickListener() {

        /**
         * Клик на конечной ветке.
         */
        @Override
        public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            showNode((TetroidNode)item);
        }

        /**
         * Клик на родительской ветке.
         */
        @Override
        public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
/*          // это событие обрабатывается с помощью OnNodeHeaderClickListener, чтобы разделить клик
            // на заголовке и на стрелке раскрытия/закрытия ветки
            TetroidNode node = (TetroidNode) item;
            if (!node.isNonCryptedOrDecrypted()) {
                decryptStorage(node);
                // как остановить дальнейшее выполнение, чтобы не стабатывал Expander?
//                return;
            }*/
        }
    };

    /**
     * Обработчик долгого клика на ветках.
     */
    private OnItemLongClickListener onNodeLongClickListener = (parent, view, item, itemInfo, pos) -> {
        if (parent != mListViewNodes)
            return;
        TetroidNode node = (TetroidNode) item;
        if (node == null) {
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        showNodePopupMenu(view, node, pos);
    };

    /**
     * Обработчик клика на метке.
     */
    private AdapterView.OnItemClickListener onTagClicklistener = (parent, view, position, id) -> {
        showTagRecords(position);
    };

    /**
     * Обработчик долгого клика на метке.
     */
    private AdapterView.OnItemLongClickListener onTagLongClicklistener = (parent, view, position, id) -> {
        Map.Entry<String, TetroidTag> tagEntry = mListAdapterTags.getItem(position);
        if (tagEntry != null) {
            showTagPopupMenu(view, tagEntry.getValue());
        }
        return true;
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

        new SearchViewListener(mSearchViewNodes) {
            @Override
            public void onClose() {
                // ничего не делать, если хранилище не было загружено
                if (mListAdapterNodes == null)
                    return;
                mListAdapterNodes.setDataItems(DataManager.getRootNodes());
                setListEmptyViewState(mTextViewNodesEmpty, DataManager.getRootNodes().isEmpty(), R.string.title_nodes_is_missing);
                tvHeader.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSearch() {
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
        mListAdapterNodes.setDataItems(found);
        setListEmptyViewState(mTextViewNodesEmpty, found.isEmpty(),
                String.format(getString(R.string.search_nodes_not_found_mask), query));
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
        new SearchViewListener(mSearchViewTags) {
            @Override
            public void onClose() {
                searchInTags(null, false);
                tvHeader.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSearch() {
                tvHeader.setVisibility(View.GONE);
            }

            @Override
            public void onQuerySubmit(String query) {
                searchInTags(query, true);
            }
        };
    }

    private void searchInTags(String query, boolean isSearch) {
//        List<TetroidTag> tags;
        Map<String,TetroidTag> tags;
        if (isSearch) {
            LogManager.addLog(String.format(getString(R.string.search_tags_by_query), query));
            tags = ScanManager.searchInTags(DataManager.getTags(), query);
        } else {
            tags = DataManager.getTags();
        }
        mListAdapterTags.setDataItems(tags);
        if (tags.isEmpty()) {
            mTextViewTagsEmpty.setText((isSearch)
                    ? String.format(getString(R.string.search_tags_not_found_mask), query)
                    : getString(R.string.log_tags_is_missing));
        }
    }

    /**
     * Виджет поиска по записям ветки / прикрепленным к записи файлам.
     * @param menuItem
     */
    private void initRecordsSearchView(MenuItem menuItem) {
        // Associate searchable configuration with the SearchView
        this.mSearchViewRecords = (SearchView) menuItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchViewRecords.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchViewRecords.setIconifiedByDefault(true);
        mSearchViewRecords.setQueryRefinementEnabled(true);

        new SearchViewXListener(mSearchViewRecords) {
            @Override
            public void onSearchClick() { }
            @Override
            public void onQuerySubmit(String query) {
                searchInMainPage(query);
            }
            @Override
            public void onSuggestionSelectOrClick(String query) {
//                searchInMainPage(query);
                mSearchViewRecords.setQuery(query, true);
            }
            @Override
            public void onClose() {
                // "сбрасываем" фильтрацию, но не для только что открытых веток
                // (т.к. при открытии ветки вызывается setIconified=false, при котором вызывается это событие,
                // что приводит к повторному открытию списка записей)
                if (!isNodeOpening) {
                    switch (mViewPagerAdapter.getMainFragment().getCurMainViewId()) {
                        case MainPageFragment.MAIN_VIEW_NODE_RECORDS:
                            if (mCurNode != null) {
                                showRecords(mCurNode.getRecords(), MainPageFragment.MAIN_VIEW_NODE_RECORDS);
                            }
                            break;
                        case MainPageFragment.MAIN_VIEW_TAG_RECORDS:
                            if (mCurTag != null) {
                                showRecords(mCurTag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS);
                            }
                            break;
                        // пока по файлам не ищем
               /* case MainPageFragment.MAIN_VIEW_RECORD_FILES:
                    TetroidRecord curRecord = mViewPagerAdapter.getMainFragment().getCurRecord();
                    if (curRecord != null) {
                        mViewPagerAdapter.getMainFragment().showRecordFiles(curRecord);
                    }
                    break;*/
                    }
                }
                MainActivity.this.mIsRecordsFiltered = false;
            }
        };
    }

    /**
     *
     * @param curPage
     */
    private void changeToolBarByPage(int curPage) {
        if (curPage == MainViewPager.PAGE_MAIN) {
            mViewPagerAdapter.getMainFragment().restoreLastMainToolbarState();
            setRecordsSearchViewVisibility(true);
        } else {
            updateMainToolbar(MainPageFragment.MAIN_VIEW_GLOBAL_FOUND, null);
        }
    }

    /**
     *
     * @param viewId
     */
    @Override
    public void updateMainToolbar(int viewId, String title) {
        boolean showRecordsSearch;

        switch (viewId) {
            case MainPageFragment.MAIN_VIEW_GLOBAL_FOUND:
                title = getString(R.string.title_global_search);
                showRecordsSearch = false;
                break;
            case MainPageFragment.MAIN_VIEW_NONE:
                title = null;
                showRecordsSearch = false;
                break;
            case MainPageFragment.MAIN_VIEW_NODE_RECORDS:
                title = ((mCurNode != null) ? mCurNode.getName() : "");
                showRecordsSearch = true;
                break;
            case MainPageFragment.MAIN_VIEW_TAG_RECORDS:
                title = ((mCurTag != null) ? mCurTag.getName() : "");
                showRecordsSearch = true;
                break;
//            case MainPageFragment.VIEW_FOUND_RECORDS:
//                showRecordsSearch = true;
//                break;
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
            default:
                showRecordsSearch = false;
        }
        setTitle(title);
        setSubtitle(viewId);
        setRecordsSearchViewVisibility(showRecordsSearch);
//        setRecordsSearchViewVisibility(showRecordsSearch, viewId);
    }

    /**
     * Установка подзаголовка активности, указывающим на тип отображаемого объекта.
     * @param viewId
     */
    private void setSubtitle(int viewId) {
        String[] titles = getResources().getStringArray(R.array.view_type_titles);
        // преобразуем идентификатор view в индекс заголовка
        int titleId = viewId - 1;
        if (titleId >= 0 && titleId < titles.length) {
            tvSubtitle.setVisibility(View.VISIBLE);
            tvSubtitle.setText(titles[titleId]);
        }
        else if (titleId < 0) {
            tvSubtitle.setVisibility(View.GONE);
        }
    }

    public void setRecordsSearchViewVisibility(boolean isVisible) {
        mMenuItemSearchViewRecords.setVisible(isVisible);
    }

    public void setFoundPageVisibility(boolean isVisible) {
        if (!isVisible)
            mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
        mViewPager.setPagingEnabled(isVisible);
        mTitleStrip.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    /**
     * Создание ветки.
     * @param parentNode Родительская ветка
     * @param pos Позиция в списке родительской ветки
     * @param isSubNode Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    private void createNode(TetroidNode parentNode, int pos, boolean isSubNode) {
        NodeAskDialogs.createNodeDialog(this, null, (name) -> {
            TetroidNode trueParentNode = (isSubNode) ? parentNode : parentNode.getParentNode();
            TetroidNode node = DataManager.createNode(name, trueParentNode);
            if (node != null) {
                if (mListAdapterNodes.addItem(pos, isSubNode)) {
//                    LogManager.addLog(getString(R.string.node_was_created), LogManager.Types.INFO, Toast.LENGTH_SHORT);
                    TetroidLog.addOperResLog(TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
                } else {
                    LogManager.addLog(getString(R.string.log_create_node_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                }
            } else {
//                LogManager.addLog(getString(R.string.log_create_node_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                TetroidLog.addOperErrorLog(TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
            }
        });
    }

    /**
     * Копирование ссылки на ветку в буфер обмена.
     */
    private void copyNodeLink(TetroidNode node) {
        if (node != null) {
            String url = node.createUrl();
            Utils.writeToClipboard(this, getString(R.string.link_to_node), url);
            LogManager.addLog(getString(R.string.title_link_was_copied) + url, LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Копирование ссылки на метку в буфер обмена.
     */
    private void copyTagLink(TetroidTag tag) {
        if (tag != null) {
            String url = tag.createUrl();
            Utils.writeToClipboard(this, getString(R.string.link_to_tag), url);
            LogManager.addLog(getString(R.string.title_link_was_copied) + url, LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Переименование ветки.
     * @param node
     */
    private void renameNode(TetroidNode node) {
        NodeAskDialogs.createNodeDialog(this, node, (name) -> {
            if (DataManager.editNodeFields(node, name)) {
//                LogManager.addLog(getString(R.string.node_was_renamed), LogManager.Types.INFO, Toast.LENGTH_SHORT);
                TetroidLog.addOperResLog(TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
                mListAdapterNodes.notifyDataSetChanged();
                if (mCurNode == node) {
                    setTitle(name);
                }
            } else {
//                LogManager.addLog(getString(R.string.log_record_edit_fields_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                TetroidLog.addOperErrorLog(TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
            }
        });
    }

    /**
     * Удаление ветки.
     * @param node
     */
    private void deleteNode(TetroidNode node, int pos) {
        if (node == null)
            return;
        // запрет на удаление последней ветки в корне
        if (node.getLevel() == 0 && DataManager.getRootNodes().size() == 1) {
            LogManager.addLog(R.string.log_cannot_delete_root_node, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            return;
        }

        NodeAskDialogs.deleteNode(this, () -> {
            boolean res = DataManager.deleteNode(node);
            onDeleteNodeResult(node, res, pos, false);
        });
    }

    private void onDeleteNodeResult(TetroidNode node, boolean res, int pos, boolean isCutted) {
        if (res) {
            // удаляем элемент внутри списка
            if (mListAdapterNodes.deleteItem(pos)) {
                TetroidLog.addOperResLog(TetroidLog.Objs.NODE, (!isCutted) ? TetroidLog.Opers.DELETE : TetroidLog.Opers.CUT);
            } else {
                LogManager.addLog(getString(R.string.log_node_delete_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            }
            // убираем список записей удаляемой ветки
            if (mCurNode == node || isNodeInNode(mCurNode, node)) {
                mViewPagerAdapter.getMainFragment().clearView();
                this.mCurNode = null;
            }
        } else {
            TetroidLog.addOperErrorLog(TetroidLog.Objs.NODE, (!isCutted) ? TetroidLog.Opers.DELETE : TetroidLog.Opers.CUT);
        }
    }

    /**
     * Проверка содержится ли ветка node в ветке nodeAsParent.
     * @param node
     * @param nodeAsParent
     * @return
     */
    public static boolean isNodeInNode(TetroidNode node, TetroidNode nodeAsParent) {
        if (node == null || nodeAsParent == null)
            return false;
        if (node.getParentNode() != null) {
            if (node.getParentNode().equals(nodeAsParent))
                return true;
            else
                return isNodeInNode(node.getParentNode(), nodeAsParent);
        }
        return false;
    }

    /**
     * Перемещение ветки вверх/вниз по списку.
     * @param node
     * @param pos Позиция элемента в списке
     * @param isUp
     */
    private void moveNode(TetroidNode node, int pos, boolean isUp) {
        if (node == null)
            return;
        TetroidNode parentNode = node.getParentNode();
        List<TetroidNode> subNodes = (parentNode != null) ? parentNode.getSubNodes() : DataManager.getRootNodes();
        if (subNodes.size() > 0) {
            int posInNode = subNodes.indexOf(node);
            int res = DataManager.swapTetroidObjects(subNodes, posInNode, isUp);
            if (res > 0) {
                // меняем местами элементы внутри списка
                if (mListAdapterNodes.swapItems(pos, posInNode, (isUp) ? posInNode-1 : posInNode+1)) {
                    TetroidLog.addOperResLog(TetroidLog.Objs.NODE, TetroidLog.Opers.MOVE);
                } else {
                    LogManager.addLog(getString(R.string.log_node_move_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                }
            } else if (res < 0) {
                TetroidLog.addOperErrorLog(TetroidLog.Objs.NODE, TetroidLog.Opers.MOVE);
            }
        }
    }

    /**
     * Развернуть все подветки у ветки.
     * @param pos
     */
    private void expandSubNodes(int pos) {
        mListAdapterNodes.extendNodeSubnodes(pos, NestType.MULTIPLE);
    }

    /**
     * Свернуть все подветки у ветки.
     * @param pos
     */
    private void collapseSubNodes(int pos) {

        // TODO: реализовать

    }

    /**
     * Копирование ветки.
     * @param node
     */
    private void copyNode(TetroidNode node) {
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(node);
        TetroidLog.addOperResLog(TetroidLog.Objs.NODE, TetroidLog.Opers.COPY);
    }

    /**
     * Вырезание ветки из родительской ветки.
     * @param node
     */
    private void cutNode(TetroidNode node, int pos) {
        // добавляем в "буфер обмена"
        TetroidClipboard.cut(node);
        // удаляем ветку из родительской ветки вместе с записями
        boolean res = DataManager.cutNode(node);
        onDeleteNodeResult(node, res, pos, false);
    }

    /**
     * Вставка ветки.
     * @param parentNode Родительская ветка
     * @param pos Позиция в списке родительской ветки
     * @param isSubNode Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    private void insertNode(TetroidNode parentNode, int pos, boolean isSubNode) {
        // на всякий случай проверяем тип
        if (!TetroidClipboard.hasObject(FoundType.TYPE_NODE))
            return;
        // достаем объект из "буфера обмена"
        TetroidClipboard clipboard = TetroidClipboard.getInstance();
        // вставляем с попыткой восстановить каталог записи
        TetroidNode node = (TetroidNode) clipboard.getObject();
        boolean isCutted = clipboard.isCutted();
        TetroidNode trueParentNode = (isSubNode) ? parentNode : parentNode.getParentNode();

        if (DataManager.insertNode(node, trueParentNode, isCutted)) {
            if (mListAdapterNodes.addItem(pos, isSubNode)) {
                TetroidLog.addOperResLog(TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
            } else {
                LogManager.addLog(getString(R.string.log_create_node_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            }
        } else {
            TetroidLog.addOperErrorLog(TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
        }
    }

    private void encryptNode(TetroidNode node) {
        checkStoragePass(node, () -> {
            // сначала расшифровываем хранилище
            if (DataManager.isCrypted()) {
                initStorage(null, true);
            }
            if (DataManager.encryptNode(node)) {
                TetroidLog.addOperResLog(TetroidLog.Objs.NODE, TetroidLog.Opers.ENCRYPT);
            } else {
                TetroidLog.addOperErrorLog(TetroidLog.Objs.NODE, TetroidLog.Opers.ENCRYPT);
            }
            mListAdapterNodes.notifyDataSetChanged();
        });
    }

    private void noEncryptNode(TetroidNode node) {
        checkStoragePass(node, () -> {
            // сначала расшифровываем хранилище
            if (DataManager.isCrypted()) {
                initStorage(null, true);
            }
            if (DataManager.nocryptNode(node)) {
                TetroidLog.addOperResLog(TetroidLog.Objs.NODE, TetroidLog.Opers.DECRYPT);
            } else {
                TetroidLog.addOperErrorLog(TetroidLog.Objs.NODE, TetroidLog.Opers.DECRYPT);
            }
            mListAdapterNodes.notifyDataSetChanged();
        });
    }

    /**
     * Асинхронная проверка имеется ли сохраненный пароль и его запрос при необходимости.
     * @param node
     * @param callback Действие после проверки пароля
     */
    private void checkStoragePass(TetroidNode node, AskDialogs.IApplyResult callback) {
        //if (SettingsManager.isSaveMiddlePassHashLocal()) {
            // получаем сохраненный пароль
            String middlePassHash;
            if ((middlePassHash = SettingsManager.getMiddlePassHash()) != null
                || (middlePassHash = CryptManager.getMiddlePassHash()) != null) {
                // пароль сохранен, проверяем
                try {
                    if (DataManager.checkMiddlePassHash(middlePassHash)) {
                        DataManager.initCryptPass(middlePassHash, true);
                        callback.onApply();
                    } else {
                        LogManager.addLog(R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                        // спрашиваем пароль
                        askPassword(node, callback);
                    }
                } catch (DatabaseConfig.EmptyFieldException ex) {
                    // если поля в INI-файле для проверки пустые
    //                if (DataManager.isExistsCryptedNodes()) {
                    if (DataManager.isCrypted()) {
                        final String hash = middlePassHash;
                        // спрашиваем "continue anyway?"
                        AskDialogs.showEmptyPassCheckingFieldDialog(this, ex.getFieldName(), () -> {
                            DataManager.initCryptPass(hash, true);
                            callback.onApply();
                        });
                    } else {
                        // если нет зашифрованных веток, но пароль сохранен
                        DataManager.initCryptPass(middlePassHash, true);
                        callback.onApply();
                    }
                }
//            } else {
//                // пароль не сохранен, вводим
//                askPassword(node, callback);
//            }
        } else {
            // спрашиваем или задаем пароль
            askPassword(node, callback);
        }
    }


    /**
     * Отображения запроса пароля от хранилища.
     * @param node
     */
    void askPassword(final TetroidNode node, AskDialogs.IApplyResult callback) {
        LogManager.addLog(R.string.log_show_pass_dialog);
        boolean isNewPass = !DataManager.isCrypted();
        // выводим окно с запросом пароля в асинхронном режиме
        AskDialogs.showPassEnterDialog(this, node, isNewPass, new AskDialogs.IPassInputResult() {
            @Override
            public void applyPass(final String pass, TetroidNode node) {
                if (isNewPass) {
                    LogManager.addLog(R.string.log_start_set_storage_pass);

                    // TODO: сохраняем пароль в database.ini

                    if (DataManager.savePass(pass)) {
                        LogManager.addLog(R.string.log_storage_pass_setted);
                    } else {
                        LogManager.addLog(R.string.log_storage_pass_set_error, LogManager.Types.ERROR);
                    }
                    DataManager.setStoragePass(pass);
                    callback.onApply();
                } else {
                    DataManager.checkPass(MainActivity.this, pass, () -> {
                        DataManager.setStoragePass(pass);
                        callback.onApply();
                    }, R.string.log_pass_is_incorrect);

                    /*try {
                        // подтверждение введенного пароля
                        if (DataManager.checkPass(pass)) {
                            String passHash = CryptManager.passToHash(pass);
                            // сохраняем хэш пароля
                            if (SettingsManager.isSaveMiddlePassHashLocal())
                                SettingsManager.setMiddlePassHash(passHash);
                            else
                                CryptManager.setMiddlePassHash(passHash);

                            DataManager.initCryptPass(pass, false);
                            callback.onApply();
                        } else {
                            LogManager.addLog(R.string.log_pass_is_incorrect, Toast.LENGTH_LONG);
                            askPassword(node, callback);
                        }
                    } catch (DataManager.EmptyFieldException e) {
                        // если поля в INI-файле для проверки пустые
                        LogManager.addLog(e);
                        // спрашиваем "continue anyway?"
                        AskDialogs.showEmptyPassCheckingFieldDialog(MainActivity.this, e.getFieldName(), () -> {
                            // TODO: пароль не сохраняем
                            //  а спрашиваем нормально ли расшифровались данные, и потом сохраняем
                            *//*String passHash = CryptManager.passToHash(pass);
                            if (SettingsManager.isSaveMiddlePassHashLocal())
                                SettingsManager.setMiddlePassHash(passHash);
                            else
                                CryptManager.setMiddlePassHash(passHash);*//*

                            DataManager.initCryptPass(pass, false);
                            callback.onApply();
                        });
                    }*/
                }
            }

            @Override
            public void cancelPass() {

            }
        });
    }


    /**
     * Отображение всплывающего (контексного) меню ветки.
     *
     * FIXME: Заменить на использование AlertDialog ? (чтобы посередине экрана)
     *
     * @param v
     * @param node
     */
    private void showNodePopupMenu(View v, TetroidNode node, int pos) {
        PopupMenu popupMenu = new PopupMenu(this, v); //, Gravity.CENTER_HORIZONTAL);
        popupMenu.inflate(R.menu.node_context);

        Menu menu = popupMenu.getMenu();
        TetroidNode parentNode = node.getParentNode();
        boolean isNonCrypted = node.isNonCryptedOrDecrypted();
        activateMenuItem(menu.findItem(R.id.action_expand_node), node.isExpandable() && isNonCrypted);
        activateMenuItem(menu.findItem(R.id.action_create_node), isNonCrypted);
        activateMenuItem(menu.findItem(R.id.action_create_subnode), isNonCrypted);
        activateMenuItem(menu.findItem(R.id.action_rename), isNonCrypted);
//        activateMenuItem(menu.findItem(R.id.action_collapse_node), node.isExpandable());
        activateMenuItem(menu.findItem(R.id.action_move_up), pos > 0);
        int nodesCount = ((parentNode != null) ? parentNode.getSubNodes() : DataManager.getRootNodes()).size();
        activateMenuItem(menu.findItem(R.id.action_move_down), pos < nodesCount - 1);
        boolean canInsert = TetroidClipboard.hasObject(FoundType.TYPE_NODE);
        activateMenuItem(menu.findItem(R.id.action_insert), canInsert);
        activateMenuItem(menu.findItem(R.id.action_insert_subnode), canInsert && isNonCrypted);
        activateMenuItem(menu.findItem(R.id.action_copy), isNonCrypted);
        boolean canCutDel = node.getLevel() > 0 || DataManager.getRootNodes().size() > 1;
        activateMenuItem(menu.findItem(R.id.action_cut), canCutDel && isNonCrypted);
        activateMenuItem(menu.findItem(R.id.action_delete), canCutDel);
        activateMenuItem(menu.findItem(R.id.action_encrypt_node), !node.isCrypted());
        boolean canNoCrypt = node.isCrypted() && (parentNode == null || !parentNode.isCrypted());
        activateMenuItem(menu.findItem(R.id.action_no_encrypt_node), canNoCrypt);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_open_node:
                    showNode(node);
                    return true;
                case R.id.action_create_subnode:
                    createNode(node, pos, true);
                    return true;
                case R.id.action_create_node:
                    createNode(node, pos, false);
                    return true;
                case R.id.action_rename:
                    renameNode(node);
                    return true;
                case R.id.action_node_icon:
                    return true;
                case R.id.action_copy_link:
                    copyNodeLink(node);
                    return true;
                case R.id.action_encrypt_node:
                    encryptNode(node);
                    return true;
                case R.id.action_no_encrypt_node:
                    noEncryptNode(node);
                    return true;
                case R.id.action_expand_node:
                    expandSubNodes(pos);
                    return true;
                case R.id.action_collapse_node:
                    collapseSubNodes(pos);
                    return true;
                case R.id.action_move_up:
                    moveNode(node, pos, true);
                    return true;
                case R.id.action_move_down:
                    moveNode(node, pos, false);
                    return true;
                case R.id.action_copy:
                    copyNode(node);
                    return true;
                case R.id.action_cut:
                    cutNode(node, pos);
                    return true;
                case R.id.action_insert:
                    insertNode(node, pos, false);
                    return true;
                case R.id.action_insert_subnode:
                    insertNode(node, pos, true);
                    return true;
                case R.id.action_delete:
                    deleteNode(node, pos);
                    return true;
                default:
                    return false;
            }
        });
        // для отображения иконок
        MenuPopupHelper menuHelper = new MenuPopupHelper(this, (MenuBuilder) menu, v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    private void activateMenuItem(MenuItem menuItem, boolean isActivate) {
//        menuItem.setEnabled(isActivate);
//        menuItem.getIcon().setAlpha((isActivate) ? 255 : 130);
        menuItem.setVisible(isActivate);
    }

    /**
     * Отображение всплывающего (контексного) меню метки.
     *
     * FIXME: Заменить на использование AlertDialog ? (чтобы посередине экрана)
     *
     * @param v
     * @param tag
     */
    private void showTagPopupMenu(View v, TetroidTag tag) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.tag_context);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_open_tag:
                    showTag(tag);
                    return true;
//                case R.id.action_rename_tag:
//                    renameTag(tag);
//                    return true;
                case R.id.action_copy_link:
                    copyTagLink(tag);
                    return true;
                default:
                    return false;
            }
        });
        // для отображения иконок
        MenuPopupHelper menuHelper = new MenuPopupHelper(this, (MenuBuilder) popupMenu.getMenu(), v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    /**
     *
     */
    @Override
    public void openFilePicker() {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra("title", R.string.title_select_file_to_upload);
        intent.putExtra("location", DataManager.getLastFolderOrDefault(this, false));
        intent.putExtra("pickFiles", true);
        startActivityForResult(intent, REQUEST_CODE_FILE_PICKER);
    }

    /**
     * Обработка возвращаемого результата других активностей.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            // перезагружаем хранилище, если изменили путь
            if (SettingsManager.isAskReloadStorage) {
                SettingsManager.isAskReloadStorage = false;
                AskDialogs.showReloadStorageDialog(this, () -> reinitStorage());
            }
            // скрываем пункт меню Синхронизация, если отключили
            ViewUtils.setVisibleIfNotNull(mMenuItemStorageSync, SettingsManager.isSyncStorage());
        } else if (requestCode == REQUEST_CODE_RECORD_ACTIVITY) {
            onRecordActivityResult(resultCode, data);
        } else if (requestCode == REQUEST_CODE_SEARCH_ACTIVITY && resultCode == RESULT_OK) {
            ScanManager scan = data.getParcelableExtra(SearchActivity.EXTRA_KEY_SCAN_MANAGER);
            startGlobalSearch(scan);
        } else if (requestCode == REQUEST_CODE_OPEN_STORAGE && resultCode == RESULT_OK) {
            String folderFullName = data.getStringExtra("data");
            initOrSyncStorage(folderFullName);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(folderFullName);
        } else if (requestCode == REQUEST_CODE_SYNC_STORAGE) {
            onSyncStorageFinish(resultCode == RESULT_OK);
        } else if (requestCode == REQUEST_CODE_FILE_PICKER && resultCode == RESULT_OK) {
            String fileFullName = data.getStringExtra("data");
            mViewPagerAdapter.getMainFragment().attachFile(fileFullName);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(FileUtils.getFileFolder(fileFullName));
        }

    }

    /**
     * Обработка возвращаемого результата активности записи.
     * @param data
     * @param resCode
     */
    private void onRecordActivityResult(int resCode, Intent data) {
        if (data == null) {
            return;
        }
        // обновляем списки, если редактировали свойства записи
        if (data.getBooleanExtra(RecordActivity.EXTRA_IS_FIELDS_EDITED, false)) {
            mViewPagerAdapter.getMainFragment().onRecordFieldsUpdated();
        }
        switch (resCode) {
            case RecordActivity.RESULT_REINIT_STORAGE:
                if (data.getBooleanExtra(RecordActivity.EXTRA_IS_RELOAD_STORAGE, false)) {
                    reinitStorage();
                }
                break;
            case RecordActivity.RESULT_OPEN_RECORD:
                String recordId = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                if (recordId != null) {
                    openRecord(recordId);
                }
                break;
            case RecordActivity.RESULT_OPEN_NODE:
                String nodeId = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                if (nodeId != null) {
                    showNode(nodeId);
                }
                break;
            case RecordActivity.RESULT_SHOW_FILES:
                String recordId2 = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                TetroidRecord record = DataManager.getRecord(recordId2);
                if (record != null) {
                    mViewPagerAdapter.getMainFragment().showRecordFiles(record);
                }
                break;
            case RecordActivity.RESULT_SHOW_TAG:
                String tagName = data.getStringExtra(RecordActivity.EXTRA_TAG_NAME);
                TetroidTag tag = DataManager.getTag(tagName);
                if (tag != null) {
                    showTag(tag);
                } else {
                    LogManager.addLog(String.format(getString(R.string.search_tag_not_found_mask), tagName),
                            LogManager.Types.WARNING, Toast.LENGTH_LONG);
                }
                break;
            case RecordActivity.RESULT_DELETE_RECORD:
                String recordId3 = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                if (recordId3 != null) {
                    TetroidRecord record2 = DataManager.getRecord(recordId3);
                    mViewPagerAdapter.getMainFragment().deleteRecordExactly(record2);
                }
                break;
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
        int type = found.getType();
        switch (type) {
            case FoundType.TYPE_RECORD:
                openRecord((TetroidRecord)found);
                break;
            case FoundType.TYPE_FILE:
                mViewPagerAdapter.getMainFragment().showRecordFiles(((TetroidFile)found).getRecord());
                break;
            case FoundType.TYPE_NODE:
                showNode((TetroidNode)found);
                break;
            case FoundType.TYPE_TAG:
                showTag((TetroidTag)found);
                break;
        }
        if (type != FoundType.TYPE_RECORD) {
            mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
        }
    }

    @Override
    public void closeFoundFragment() {
        setFoundPageVisibility(false);
    }

    @Override
    public void reSearch() {
        showGlobalSearchActivity();
    }

    @Override
    public void openMainPage() {
        mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_WRITE_STORAGE: {
                if (permGranted) {
                    startInitStorage();
                } else {
                    LogManager.addLog(R.string.log_missing_read_ext_storage_permissions, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
                }
            } break;
            case REQUEST_CODE_PERMISSION_WRITE_TEMP: {
                if (permGranted) {
                    openAttach(mTempFileToOpen);
                } else {
                    LogManager.addLog(R.string.log_missing_write_ext_storage_permissions, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
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
        checkReceivedIntent(intent);
        super.onNewIntent(intent);
    }

    /**
     * Проверка входящего Intent.
     */
    private void checkReceivedIntent(final Intent intent) {
        String action;
        if (intent == null || (action = intent.getAction()) == null) {
            return;
        }
        if (action.equals(Intent.ACTION_SEARCH)) {
            // обработка результата голосового поиска
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchViewRecords.setQuery(query, true);

        } else if (action.equals(Intent.ACTION_SEND)) {
            // прием текста/изображения из другого приложения
            String type = intent.getType();
            if (type == null) {
                return;
            }
            String text = null;
            boolean isText = false;
            ArrayList<Uri> uris = null;
            if (type.startsWith("text/")) {
                // текст
                isText = true;
                text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text == null) {
                    LogManager.addLog(R.string.log_not_passed_text, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.addLog(getString(R.string.log_receiving_intent_text), LogManager.Types.INFO);
            } else if (type.startsWith("image/")) {
                // изображение
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri == null) {
                    LogManager.addLog(R.string.log_not_passed_image_uri, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.addLog(String.format(getString(R.string.log_receiving_intent_image_mask), imageUri), LogManager.Types.INFO);
                uris = new ArrayList<>();
                uris.add(imageUri);
            }
            showIntentDialog(intent, isText, text, uris);

        } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            // прием нескольких изображений из другого приложения
            String type = intent.getType();
            if (type == null) {
                return;
            }
            if (type.startsWith("image/")) {
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (uris == null) {
                    LogManager.addLog(R.string.log_not_passed_image_uri, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.addLog(String.format(getString(R.string.log_receiving_intent_images_mask), uris.size()), LogManager.Types.INFO);
                showIntentDialog(intent, false, null, uris);
            }
        }
    }

    private void showIntentDialog(Intent intent, boolean isText, String text, ArrayList<Uri> imagesUri) {

        IntentDialog.createDialog(this, isText, item -> {
            if (item.isCreate()) {
                // имя записи
                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                String url = null;
                if (Build.VERSION.SDK_INT >= 17) {
                    url = intent.getStringExtra(Intent.EXTRA_ORIGINATING_URI);
                }
                // создаем запись
                TetroidRecord record = DataManager.createRecord(subject, url, text);
                if (record == null) {
                    return;
                }
                // открываем ветку, в которую добавили запись
                showNode(record.getNode());
                // обновляем список записей, меток, и количества записей ветки
                mViewPagerAdapter.getMainFragment().addNewRecord(record, isText && !item.isAttach());
                // загружаем изображения в каталоги записи
                if (!isText) {
                    if (!item.isAttach()) {
                        // запускаем активность записи с командой вставки изображений после загрузки
                        openRecord(record, imagesUri);

                    } else {
                        // прикрепляем изображения как файлы
                        boolean hasError = false;
                        for (Uri uri : imagesUri) {
                            if (DataManager.attachFile(FileUtils.getPathFromUri(this, uri), record) == null) {
                                hasError = true;
                            }
                        }
                        if (hasError) {
                            LogManager.addLog(R.string.log_files_attach_error, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                        }
                        // обновляем список файлов
                        mDrawerLayout.closeDrawers();
                        mViewPagerAdapter.getMainFragment().showRecordFiles(record);
                    }
                }
            } else {
                // TODO: реализовать выбор имеющихся записей
            }
        });

    }

    /**
     * Поиск по записям, меткам или файлам (смотря какой список активен в данный момент).
     * @param query
     */
    private void searchInMainPage(String query) {
        TetroidSuggestionProvider.saveRecentQuery(this, query);
        searchInMainPage(query, mViewPagerAdapter.getMainFragment().getCurMainViewId());
    }

    private void searchInMainPage(String query, int viewId) {
        switch (viewId) {
            case MainPageFragment.MAIN_VIEW_NODE_RECORDS:
                searchInNodeRecords(query);
                break;
            case MainPageFragment.MAIN_VIEW_TAG_RECORDS:
                searchInTagRecords(query);
                break;
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
                searchInRecordFiles(query);
                break;
//            case MainPageFragment.VIEW_FOUND_RECORDS:
//                int lastVIewId = mViewPagerAdapter.getMainFragment().getLastViewId();
//                if (viewId != lastVIewId)
//                    searchInMainPage(query, lastVIewId);
//                break;
        }
    }

    private void searchInNodeRecords(String query) {
        if (mCurNode != null) {
            searchInRecords(query, mCurNode.getRecords(), MainPageFragment.MAIN_VIEW_NODE_RECORDS);
        } else {
            LogManager.addLog(R.string.search_records_search_select_node, Toast.LENGTH_LONG);
        }
    }

    private void searchInTagRecords(String query) {
        if (mCurTag != null) {
            searchInRecords(query, mCurTag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS);
        } else {
            LogManager.addLog(R.string.search_records_select_tag, Toast.LENGTH_LONG);
        }
    }

    private void searchInRecords(String query, List<TetroidRecord> records, int viewId) {
        String log = (viewId == MainPageFragment.MAIN_VIEW_NODE_RECORDS)
                ? String.format(getString(R.string.search_records_in_node_by_query), mCurNode.getName(), query)
                : String.format(getString(R.string.search_records_in_tag_by_query), mCurTag.getName(), query);
        LogManager.addLog(log);
        List<TetroidRecord> found = ScanManager.searchInRecordsNames(records, query);
//        showRecords(found, MainPageFragment.VIEW_FOUND_RECORDS);
        showRecords(found, viewId, true);
        if (found.isEmpty()) {
            String emptyText = (viewId == MainPageFragment.MAIN_VIEW_NODE_RECORDS)
                    ? String.format(getString(R.string.search_records_in_node_not_found_mask), query, mCurNode.getName())
                    : String.format(getString(R.string.search_records_in_tag_not_found_mak), query, mCurTag.getName());
            mViewPagerAdapter.getMainFragment().setRecordsEmptyViewText(emptyText);
        }
    }

    private void searchInRecordFiles(String query) {
        TetroidRecord curRecord = mViewPagerAdapter.getMainFragment().getCurRecord();
        if (curRecord != null) {
            searchInFiles(query, curRecord);
        } else {
            LogManager.addLog(getString(R.string.log_cur_record_is_not_set), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    private void searchInFiles(String query, TetroidRecord record) {
        LogManager.addLog(String.format(getString(R.string.search_files_by_query), record.getName(), query));
        List<TetroidFile> found = ScanManager.searchInFiles(record.getAttachedFiles(), query);
        mViewPagerAdapter.getMainFragment().showRecordFiles(found);
        if (found.isEmpty()) {
            mViewPagerAdapter.getMainFragment().setFilesEmptyViewText(
                    String.format(getString(R.string.search_files_not_found_mask), query));
        }
    }

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else if (mViewPager.getCurrentItem() == MainViewPager.PAGE_MAIN
                && !mViewPagerAdapter.getMainFragment().onBackPressed()
            || mViewPager.getCurrentItem() == MainViewPager.PAGE_FOUND
                && !mViewPagerAdapter.getFoundFragment().onBackPressed()) {
            if (SettingsManager.isConfirmAppExit()) {
                askForExit();
            } else {
                onBeforeExit();
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
//        mViewPagerAdapter.getMainFragment().onCreateOptionsMenu(menu);
        this.mMenuItemGlobalSearch = menu.findItem(R.id.action_global_search);
        this.mMenuItemStorageSync = menu.findItem(R.id.action_storage_sync);
        ViewUtils.setVisibleIfNotNull(mMenuItemStorageSync, SettingsManager.isSyncStorage());
        this.mMenuItemStorageInfo = menu.findItem(R.id.action_storage_info);
        this.mMenuItemSearchViewRecords = menu.findItem(R.id.action_search_records);
        initRecordsSearchView(mMenuItemSearchViewRecords);

        // для отображения иконок
        if (menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        //
        this.mIsActivityCreated = true;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mViewPagerAdapter.getMainFragment().onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
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
                showNode(mCurNode);
                return true;
            case R.id.action_fullscreen:
                App.toggleFullscreen(MainActivity.this);
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
                ViewUtils.startActivity(this, InfoActivity.class, null);
                return true;
            case R.id.action_about_app:
                ViewUtils.startActivity(this, AboutActivity.class, null);
                return true;
            default:
                if (mViewPagerAdapter.getMainFragment().onOptionsItemSelected(id)) {
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Обработчик смены страниц ViewPager.
     */
    private class ViewPagerListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }
        @Override
        public void onPageSelected(int i) {
            if (mIsActivityCreated) {
                changeToolBarByPage(i);
            }
        }
        @Override
        public void onPageScrollStateChanged(int i) {
        }
    }

    private void askForExit() {
        AskDialogs.showExitDialog(this, () -> {
            onBeforeExit();
            finish();
        });
    }

    private void onBeforeExit() {
        // сохраняем выбранную ветку
        if (SettingsManager.isKeepSelectedNode()) {
            String nodeId = (mCurNode != null) ? mCurNode.getId() : null;
            SettingsManager.setSelectedNodeId(nodeId);
        }
    }

    private void showGlobalSearchActivity() {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(EXTRA_CUR_NODE_IS_NOT_NULL, (mCurNode != null));
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
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            mTextViewProgress.setText(R.string.title_storage_loading);
            mLayoutProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {
            boolean isDecrypt = booleans[0];
            return DataManager.readStorage(isDecrypt);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mDrawerLayout.openDrawer(Gravity.LEFT);
            mLayoutProgress.setVisibility(View.INVISIBLE);
            if (res) {
                MainActivity.this.mIsStorageLoaded = true;
                LogManager.addLog(getString(R.string.log_storage_loaded) + DataManager.getStoragePath(), Toast.LENGTH_SHORT);
            } else {
                LogManager.addLog(getString(R.string.log_failed_storage_load) + DataManager.getStoragePath(),
                        LogManager.Types.WARNING, Toast.LENGTH_LONG);
            }
            // инициализация контролов
            initGUI(res);
            // действия после загрузки хранилища
            if (res) {
                afterStorageInited();
            }

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
//            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            mTextViewProgress.setText(R.string.global_searching);
            mLayoutProgress.setVisibility(View.VISIBLE);

            LogManager.addLog(String.format(getString(R.string.global_search_start), scan.getQuery()));
        }

        @Override
        protected HashMap<ITetroidObject, FoundType> doInBackground(Void... voids /*ScanManager... scans*/) {
//            ScanManager scan = scans[0];
            return scan.globalSearch(mCurNode);
        }

        @Override
        protected void onPostExecute(HashMap<ITetroidObject,FoundType> found) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
//            mDrawerLayout.openDrawer(Gravity.LEFT);
            mLayoutProgress.setVisibility(View.INVISIBLE);
            if (found == null) {
                LogManager.addLog(getString(R.string.log_global_search_return_null), Toast.LENGTH_SHORT);
                return;
            } else if (scan.isSearchInNode() && scan.getNode() != null) {
                LogManager.addLog(String.format(getString(R.string.global_search_by_node_result),
                        scan.getNode().getName()), Toast.LENGTH_LONG);
            }
            // уведомляем, если не смогли поискать в зашифрованных ветках
            if (scan.isExistCryptedNodes()) {
                LogManager.addLog(R.string.log_found_crypted_nodes, Toast.LENGTH_SHORT);
            }
            LogManager.addLog(String.format(getString(R.string.global_search_end), found.size()));
            mViewPagerAdapter.getFoundFragment().setFounds(found, scan);
            mViewPagerAdapter.notifyDataSetChanged(); // для обновления title у страницы
            setFoundPageVisibility(true);
            mViewPager.setCurrent(MainViewPager.PAGE_FOUND);
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

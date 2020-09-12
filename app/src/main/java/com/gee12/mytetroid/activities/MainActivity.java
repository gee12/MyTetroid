package com.gee12.mytetroid.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Parcel;
import android.text.Spanned;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.FileObserverService;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TaskStage;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.TetroidTask;
import com.gee12.mytetroid.adapters.MainPagerAdapter;
import com.gee12.mytetroid.adapters.NodesListAdapter;
import com.gee12.mytetroid.adapters.TagsListAdapter;
import com.gee12.mytetroid.data.AttachesManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.DatabaseConfig;
import com.gee12.mytetroid.data.FavoritesManager;
import com.gee12.mytetroid.data.ICallback;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.PINManager;
import com.gee12.mytetroid.data.PassManager;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.data.SyncManager;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.NodeAskDialogs;
import com.gee12.mytetroid.dialogs.PassDialogs;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.fragments.SettingsFragment;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;
import com.gee12.mytetroid.model.ReceivedData;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.IntentDialog;
import com.gee12.mytetroid.views.MainViewPager;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.SearchViewListener;
import com.gee12.mytetroid.views.SearchViewXListener;
import com.gee12.mytetroid.views.StorageChooserDialog;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    public static final int REQUEST_CODE_CREATE_STORAGE = 2;
    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 3;
    public static final int REQUEST_CODE_RECORD_ACTIVITY = 4;
    public static final int REQUEST_CODE_SEARCH_ACTIVITY = 5;
    public static final int REQUEST_CODE_SYNC_STORAGE = 6;
    public static final int REQUEST_CODE_FILE_PICKER = 7;
    public static final int REQUEST_CODE_FOLDER_PICKER = 8;

    public static final int REQUEST_CODE_PERMISSION_WRITE_STORAGE = 1;
    public static final int REQUEST_CODE_PERMISSION_WRITE_TEMP = 2;
    public static final String EXTRA_CUR_NODE_IS_NOT_NULL = "EXTRA_CUR_NODE_IS_NOT_NULL";
    public static final String EXTRA_QUERY = "EXTRA_QUERY";

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
    private SearchView mSearchViewRecords;
    private MenuItem mMenuItemSearchViewRecords;
    private MenuItem mMenuItemGlobalSearch;
    private MenuItem mMenuItemStorageSync;
    private MenuItem mMenuItemStorageInfo;
    private MenuItem mMenuItemStorageReload;
    private MainPagerAdapter mViewPagerAdapter;
    private MainViewPager mViewPager;
    private PagerTabStrip mTitleStrip;
    private View mFavoritesNode;
    private Button mLoadStorageButton;

    private boolean mIsAlreadyTryDecrypt;
    private Intent mReceivedIntent;
    private boolean mIsActivityCreated;
    private boolean mIsLoadStorageAfterSync;
    private TetroidFile mTempFileToOpen;
    private boolean mIsDropRecordsFiltering = true;
    private ScanManager mLastScan;
    private TetroidTask mCurTask;
    private String mLastSearchQuery;
    private boolean mIsStorageChangingHandled;
    private ICallback mOutsideChangingHandler;

    /**
     *
     */
    private static Activity instance;
    public static Activity getInstance() {
        return instance;
    }

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

        instance = this;

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

        // обработчик изменения структуры хранилища извне
        mOutsideChangingHandler = res -> {
            // проверяем, не был ли запущен обработчик второй раз подряд
            if (!mIsStorageChangingHandled) {
                MainActivity.this.mIsStorageChangingHandled = true;
                MainActivity.this.runOnUiThread(() -> {
                    LogManager.log(R.string.ask_storage_changed_outside, LogManager.Types.INFO);
                    // выводим уведомление
                    AskDialogs.showYesNoDialog(MainActivity.this, new Dialogs.IApplyCancelDismissResult() {
                        @Override
                        public void onCancel() {
                        }

                        @Override
                        public void onApply() {
                            reloadStorage();
                        }

                        @Override
                        public void onDismiss() {
                            MainActivity.this.mIsStorageChangingHandled = false;
                        }
                    }, R.string.ask_storage_changed_outside);
                });
            }
        };

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

        // ветки
        mListViewNodes = findViewById(R.id.list_view_nodes);
        mListViewNodes.setOnItemClickListener(onNodeClickListener);
        mListViewNodes.setOnItemLongClickListener(onNodeLongClickListener);
//        registerForContextMenu(mListViewNodes.getListView());
        this.mTextViewNodesEmpty = findViewById(R.id.nodes_text_view_empty);
//        mListViewNodes.setEmptyView(mTextViewNodesEmpty);

        NavigationView nodesNavView = mDrawerLayout.findViewById(R.id.nav_view_left);
        View vNodesHeader = nodesNavView.getHeaderView(0);
        this.mSearchViewNodes = vNodesHeader.findViewById(R.id.search_view_nodes);
        mSearchViewNodes.setVisibility(View.GONE);
        initNodesView(mSearchViewNodes, vNodesHeader);

        // метки
        this.mListViewTags = findViewById(R.id.tags_list_view);
        mListViewTags.setOnItemClickListener(onTagClicklistener);
        mListViewTags.setOnItemLongClickListener(onTagLongClicklistener);
        this.mTextViewTagsEmpty = findViewById(R.id.tags_text_view_empty);
        mListViewTags.setEmptyView(mTextViewTagsEmpty);

        this.mLayoutProgress = findViewById(R.id.layout_progress);
        this.mTextViewProgress = findViewById(R.id.progress_text);

        NavigationView tagsNavView = mDrawerLayout.findViewById(R.id.nav_view_right);
        View vTagsHeader = tagsNavView.getHeaderView(0);
        this.mSearchViewTags = vTagsHeader.findViewById(R.id.search_view_tags);
        mSearchViewTags.setVisibility(View.GONE);
        final TextView tvHeader = vTagsHeader.findViewById(R.id.text_view_tags_header);
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

        // избранное
        this.mFavoritesNode = findViewById(R.id.node_favorites);
        this.mLoadStorageButton = findViewById(R.id.button_load);
        mFavoritesNode.setVisibility(View.GONE);
        mLoadStorageButton.setVisibility(View.GONE);
        if (App.isFullVersion()) {
            mFavoritesNode.setOnClickListener(v -> showFavorites());
            mLoadStorageButton.setOnClickListener(v -> {
                loadAllNodes();
            });
        }
    }

    /**
     *
     */
    @Override
    public void onMainPageCreated() {
        // инициализация
        SettingsManager.init(getApplicationContext());
        mViewPagerAdapter.getMainFragment().onSettingsInited();
        setMenuItemsVisible();
        LogManager.init(this, SettingsManager.getLogPath(), SettingsManager.isWriteLogToFile());
        LogManager.log(String.format(getString(R.string.log_app_start_mask), Utils.getVersionName(this)));
        if (SettingsManager.isCopiedFromFree()) {
            LogManager.log(R.string.log_settings_copied_from_free, LogManager.Types.INFO);
        }
        startInitStorage(false);
    }

    /**
     * Начало загрузки хранилища.
     *
     * @isLoadLastForced Загружать по сохраненнному пути, даже если не установлена опция isLoadLastStoragePath.
     */
    private void startInitStorage(boolean isLoadLastForced) {
        this.mIsAlreadyTryDecrypt = false;

        if (!PermissionManager.checkWriteExtStoragePermission(this, REQUEST_CODE_PERMISSION_WRITE_STORAGE)) {
            return;
        }

        String storagePath = SettingsManager.getStoragePath();
        if (storagePath != null && SettingsManager.isLoadLastStoragePath() || isLoadLastForced) {
            initOrSyncStorage(storagePath);
        } else {
            StorageChooserDialog.createDialog(this, isNew -> showStorageFolderChooser(isNew));
        }
    }

    /**
     * Обработчик события после загрузки хранилища.
     */
    private void afterStorageInited() {
        // проверяем входящий Intent после загрузки
        checkReceivedIntent(mReceivedIntent);
        // запускаем отслеживание изменения структуры хранилища
        startStorageTreeObserver();
    }

    /**
     * Обработчик изменения структуры хранилища извне.
     */
    private void startStorageTreeObserver() {
        if (SettingsManager.isCheckOutsideChanging()) {
            this.mIsStorageChangingHandled = false;
//            TetroidFileObserver.startStorageObserver(mOutsideChangingHandler);
            Bundle bundle = new Bundle();
            bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START);
            bundle.putString(FileObserverService.EXTRA_FILE_PATH, DataManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME);
            bundle.putInt(FileObserverService.EXTRA_EVENT_MASK, FileObserver.MODIFY);
            FileObserverService.sendCommand(this, bundle);
        }
    }

    /**
     * Перезагрузка хранилища (при изменении пути в настройках).
     */
    private void reinitStorage() {
        closeFoundFragment();
        mViewPagerAdapter.getMainFragment().clearView();
        startInitStorage(true);
    }

    /**
     * Перезагрузка хранилища.
     */
    private void reloadStorageAsk() {
        AskDialogs.showReloadStorageDialog(this, false, false, () -> {
            reloadStorage();
        });
    }

    private void reloadStorage() {
        // сохраняем id выбранной ветки
        saveLastSelectedNode();
        // перезагружаем хранилище
        reinitStorage();
    }

    /**
     * Загрузка всех веток, когда загружено только избранное.
     */
    private void loadAllNodes() {
        if (DataManager.isCrypted()) {
//            decryptStorage(FavoritesManager.FAVORITES_NODE, false, false, false);
            // FIXME: не передаем node=FAVORITES_NODE, т.к. тогда хранилище сразу расшифровуется без запроса ПИН-кода
            //  По-идее, нужно остановить null, но сразу расшифровывать хранилище, если до этого уже
            //    вводили ПИН-код (для расшифровки избранной записи)
            //  Т.Е. сохранять признак того, что ПИН-крд уже вводили в этой "сессии"
            decryptStorage(null, false, false, false);
        } else {
            initStorage(null, false, false, false);
        }
    }

    /**
     * Создание нового хранилища в указанном расположении.
     *
     * @param storagePath //     * @param checkDirIsEmpty
     */
    private void createStorage(String storagePath/*, boolean checkDirIsEmpty*/) {
        /*if (checkDirIsEmpty) {
            if (!FileUtils.isDirEmpty(new File(storagePath))) {
                AskDialogs.showYesDialog(this, () -> {
                    createStorage(storagePath, false);
                }, R.string.ask_dir_not_empty);
                return;
            }
        }*/
        if (DataManager.init(this, storagePath, true)) {
            closeFoundFragment();
            mViewPagerAdapter.getMainFragment().clearView();
            mDrawerLayout.openDrawer(Gravity.LEFT);
            // сохраняем путь к хранилищу
//            if (SettingsManager.isLoadLastStoragePath()) {
            SettingsManager.setStoragePath(storagePath);
//            }
            initGUI(DataManager.createDefault(), false, false);
//            LogManager.log(getString(R.string.log_storage_created) + mStoragePath, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            TetroidLog.logOperRes(TetroidLog.Objs.STORAGE, TetroidLog.Opers.CREATE, "", Toast.LENGTH_SHORT);
        } else {
            mDrawerLayout.openDrawer(Gravity.LEFT);
            initGUI(false, false, false);
//            LogManager.log(getString(R.string.log_failed_storage_create) + mStoragePath, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.logOperErrorMore(TetroidLog.Objs.STORAGE, TetroidLog.Opers.CREATE, Toast.LENGTH_LONG);
        }
    }

    /**
     * Проверка нужно ли синхронизировать хранилище перед загрузкой.
     *
     * @param storagePath
     */
    private void initOrSyncStorage(final String storagePath) {
        if (SettingsManager.isSyncStorage() && SettingsManager.isSyncBeforeInit()) {
            // спрашиваем о необходимости запуска синхронизации, если установлена опция
            if (SettingsManager.isAskBeforeSync()) {
                AskDialogs.showSyncRequestDialog(this, new Dialogs.IApplyCancelResult() {
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
     *
     * @param storagePath
     */
    private void startStorageSync(String storagePath) {
        String command = SettingsManager.getSyncCommand();
        Intent intent = SyncManager.createCommandSender(this, storagePath, command);

        LogManager.log(getString(R.string.log_start_storage_sync) + command);
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
     *
     * @param res
     */
    private void onSyncStorageFinish(boolean res) {
        final String storagePath = SettingsManager.getStoragePath();
        if (res) {
            LogManager.log(R.string.log_sync_successful, Toast.LENGTH_SHORT);
            if (mIsLoadStorageAfterSync)
                initStorage(storagePath);
            else {
                AskDialogs.showSyncDoneDialog(this, true, () -> initStorage(storagePath));
            }
        } else {
            LogManager.log(getString(R.string.log_sync_failed), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            if (mIsLoadStorageAfterSync) {
                AskDialogs.showSyncDoneDialog(this, false, () -> initStorage(storagePath));
            }
        }
        this.mIsLoadStorageAfterSync = false;
    }

    /**
     * Загрузка хранилища по указанному пути.
     *
     * @param storagePath Путь хранилища
     */
    private void initStorage(String storagePath) {
        // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
        boolean isFavorites = !DataManager.isLoaded() && SettingsManager.isLoadFavoritesOnly()
                || (DataManager.isLoaded() && DataManager.isFavoritesMode());

        if (DataManager.init(this, storagePath, false)) {
            LogManager.log(getString(R.string.log_storage_settings_inited) + storagePath);
            mDrawerLayout.openDrawer(Gravity.LEFT);
            // сохраняем путь к хранилищу
//            if (SettingsManager.isLoadLastStoragePath()) {
            SettingsManager.setStoragePath(storagePath);
//            }
            if (DataManager.isCrypted() /*&& !isFavorites*/) {
                // сначала устанавливаем пароль, а потом загружаем (с расшифровкой)
                //decryptStorage(null);
                decryptStorage(null, false, isFavorites, true);
            } else {
                // загружаем
                initStorage(null, false, isFavorites, true);
            }
        } else {
            LogManager.log(getString(R.string.log_failed_storage_init) + storagePath,
                    LogManager.Types.ERROR, Toast.LENGTH_LONG);
            mDrawerLayout.openDrawer(Gravity.LEFT);
            initGUI(false, isFavorites, false);
        }
    }

    /**
     * Получение пароля и расшифровка хранилища. Вызывается при:
     * 1) запуске приложения, если есть зашифрованные ветки и сохранен пароль
     * 2) запуске приложения, если есть зашифрованные ветки и установлен isAskPasswordOnStart
     * 3) запуске приложения, если выделение было сохранено на зашифрованной ветке
     * 4) выборе зашифрованной ветки
     * 5) выборе зашифрованной записи в избранном
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isNodeOpening   Вызвана ли функция при попытке открытия зашифрованной ветки
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isOpenLastNode  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId(),
     *                        или ветку с избранным (если именно она передана в node)
     */
    private void decryptStorage(TetroidNode node, boolean isNodeOpening, boolean isOnlyFavorites, boolean isOpenLastNode) {
        String middlePassHash;
        // пароль сохранен локально?
        if (SettingsManager.isSaveMiddlePassHashLocal()
                && (middlePassHash = SettingsManager.getMiddlePassHash()) != null) {
            // проверяем
            try {
                if (PassManager.checkMiddlePassHash(middlePassHash)) {
                    DataManager.initCryptPass(middlePassHash, true);
                    // запрос ПИН-кода
                    PINManager.askPINCode(this, isNodeOpening, () -> {
                        initStorage(node, true, isOnlyFavorites, isOpenLastNode);
                    });

                } else if (isNodeOpening) {
                    // спрашиваем пароль
                    askPassword(node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
                } else {
                    LogManager.log(R.string.log_wrong_saved_pass, Toast.LENGTH_LONG);
                    if (!mIsAlreadyTryDecrypt) {
                        mIsAlreadyTryDecrypt = true;
                        initStorage(node, false, isOnlyFavorites, isOpenLastNode);
                    }
                }
            } catch (DatabaseConfig.EmptyFieldException ex) {
                // если поля в INI-файле для проверки пустые
                LogManager.log(ex);
                // спрашиваем "continue anyway?"
                PassDialogs.showEmptyPassCheckingFieldDialog(this, ex.getFieldName(), new Dialogs.IApplyCancelResult() {
                            @Override
                            public void onApply() {
                                DataManager.initCryptPass(middlePassHash, true);
                                // запрос ПИН-кода
                                PINManager.askPINCode(MainActivity.this, isNodeOpening, () -> {
                                    initStorage(node, true, isOnlyFavorites, isOpenLastNode);
                                });
                            }

                    @Override
                            public void onCancel() {
                                if (!isNodeOpening) {
                                    // загружаем хранилище без пароля
                                    initStorage(node, false, isOnlyFavorites, isOpenLastNode);
                                }
                            }
                        }
                );
            }
        } else if (SettingsManager.getWhenAskPass().equals(getString(R.string.pref_when_ask_password_on_start))
                || isNodeOpening) {
            // если пароль не сохранен, то спрашиваем его, когда также:
            //      * если нужно расшифровывать хранилище сразу на старте
            //      * если функция вызвана во время открытия зашифрованной ветки
            askPassword(node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
        } else {
            // тогда просто загружаем хранилище без расшифровки, если:
            //      * не сохранен пароль
            //      * пароль не нужно спрашивать на старте
            //      * функция не вызвана во время открытия зашифрованной ветки
            initStorage(node, false, isOnlyFavorites, isOpenLastNode);
        }
    }

    /**
     * Отображения запроса пароля от хранилища.
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isNodeOpening   Вызвана ли функция при попытке открытия зашифрованной ветки
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isOpenLastNode  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
     *                        или ветку с избранным (если именно она передана в node)
     */
    private void askPassword(final TetroidNode node, boolean isNodeOpening, boolean isOnlyFavorites, boolean isOpenLastNode) {
        LogManager.log(R.string.log_show_pass_dialog);
        // выводим окно с запросом пароля в асинхронном режиме
        PassDialogs.showPassEnterDialog(this, node, false, new PassDialogs.IPassInputResult() {
            @Override
            public void applyPass(final String pass, TetroidNode node) {
                // подтверждение введенного пароля
                PassManager.checkPass(MainActivity.this, pass, (res) -> {
                    if (res) {
                        PassManager.initPass(pass);
                        PINManager.askPINCode(MainActivity.this, isNodeOpening, () -> {
                            initStorage(node, true, isOnlyFavorites, isOpenLastNode);
                        });
                    } else {
                        // повторяем запрос
                        askPassword(node, isNodeOpening, isOnlyFavorites, isOpenLastNode);
                    }
                }, R.string.log_pass_is_incorrect);
            }

            @Override
            public void cancelPass() {
                // Если при первой загрузке хранилища установлена текущей зашифрованная ветка (node),
                // и пароль не сохраняли, то нужно его спросить.
                // Но если пароль вводить отказались, то просто грузим хранилище как есть
                // (только в первый раз, затем перезагружать не нужно)
                if (!mIsAlreadyTryDecrypt && !DataManager.isLoaded()) {
                    mIsAlreadyTryDecrypt = true;
                    initStorage(node, false, isOnlyFavorites, isOpenLastNode);
                }
            }
        });
    }

    /**
     * Непосредственная расшифровка (если зашифровано) или чтение данных хранилища.
     *
     * @param node            Зашифрованная ветка, которую нужно открыть после засшифровки.
     * @param isDecrypt       Нужно ли вызвать процесс расшифровки хранилища.
     * @param isOnlyFavorites Нужно ли загружать только избранные записи
     * @param isOpenLastNode  Нужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
     *                        или ветку с избранным (если именно она передана в node)
     */
    private void initStorage(TetroidNode node, boolean isDecrypt, boolean isOnlyFavorites, boolean isOpenLastNode) {
        // расшифровуем хранилище только в том случаем, если:
        //  1) не используем проверку ПИН-кода
        //  2) используем проверку ПИН-кода, при этом расшифровуем с открытием конкретной <b>зашифрованной</b> ветки
        //   (или ветки Избранное)
        isDecrypt = isDecrypt
                && (!PINManager.isRequestPINCode()
                || PINManager.isRequestPINCode() && node != null
                && (node.isCrypted() || node.equals(FavoritesManager.FAVORITES_NODE)));
        if (isDecrypt && DataManager.isNodesExist()) {
            // расшифровываем уже загруженное хранилище
            this.mCurTask = new DecryptStorageTask(node).run();
        } else {
            // загружаем хранилище впервые, с расшифровкой
            TetroidLog.logOperStart(TetroidLog.Objs.STORAGE, TetroidLog.Opers.LOAD);
            this.mCurTask = new ReadStorageTask(isDecrypt, isOnlyFavorites, isOpenLastNode).run();
        }
    }

    private void afterStorageDecrypted(TetroidNode node) {
        updateNodes();
        updateTags();

        if (node != null) {
            if (node == FavoritesManager.FAVORITES_NODE) {
                showFavorites();
            } else {
                showNode(node);
            }
        }

        checkReceivedIntent(mReceivedIntent);
    }

    /**
     * Первоначальная инициализация списков веток, записей, файлов, меток
     *
     * @param res             Результат загрузки хранилища.
     * @param isOnlyFavorites
     * @param isOpenLastNode  Нужно ли загружать ветку, сохраненную в опции getLastNodeId(),
     *                        или ветку с избранными записями
     */
    private void initGUI(boolean res, boolean isOnlyFavorites, boolean isOpenLastNode) {
        // избранные записи
        mLoadStorageButton.setVisibility((res && isOnlyFavorites) ? View.VISIBLE : View.GONE);
        mListViewNodes.setVisibility((!isOnlyFavorites) ? View.VISIBLE : View.GONE);
        mFavoritesNode.setVisibility((res && App.isFullVersion()) ? View.VISIBLE : View.GONE);
        mTextViewNodesEmpty.setVisibility(View.GONE);
        if (res && App.isFullVersion()) {
            updateFavorites();
        }

        if (isOnlyFavorites) {
            // обработка только "ветки" избранных записей
            if (res) {
                // списки записей, файлов
                mViewPagerAdapter.getMainFragment().initListAdapters(this);
                showFavorites();
            } else {
                setListEmptyViewState(mTextViewNodesEmpty, true, R.string.log_storage_load_error);
            }
        } else {
            // список веток
            this.mListAdapterNodes = new NodesListAdapter(this, onNodeHeaderClickListener);
            mListViewNodes.setAdapter(mListAdapterNodes);
            // список меток
            this.mListAdapterTags = new TagsListAdapter(this);
            mListViewTags.setAdapter(mListAdapterTags);

            // добавляем к результату загрузки проверку на пустоту списка веток
            List<TetroidNode> rootNodes = DataManager.getRootNodes();
            res = (res && rootNodes != null);
            if (res) {
                boolean isEmpty = rootNodes.isEmpty();
                // выбираем ветку, выбранную в прошлый раз
                boolean nodesAdapterInited = false;
                TetroidNode nodeToSelect = null;
                if (SettingsManager.isKeepLastNode() && !isEmpty && isOpenLastNode) {
                    String nodeId = SettingsManager.getLastNodeId();
                    if (nodeId != null) {
                        if (nodeId.equals(FavoritesManager.FAVORITES_NODE.getId())) {
                            nodeToSelect = FavoritesManager.FAVORITES_NODE;
                        } else {
                            nodeToSelect = NodesManager.getNode(nodeId);
                            if (nodeToSelect != null) {
                                Stack<TetroidNode> expandNodes = NodesManager.createNodesHierarchy(nodeToSelect);
                                mListAdapterNodes.setDataItems(rootNodes, expandNodes);
                                nodesAdapterInited = true;
                            }
                        }
                    }
                } else {
                    // очищаем заголовок, список
                    mViewPagerAdapter.getMainFragment().clearView();
                }
                if (!nodesAdapterInited) {
                    mListAdapterNodes.setDataItems(rootNodes);
                }

                if (!isEmpty) {
                    // списки записей, файлов
                    mViewPagerAdapter.getMainFragment().initListAdapters(this);
                    if (nodeToSelect != null) {
                        if (nodeToSelect == FavoritesManager.FAVORITES_NODE) {
                            showFavorites();
                        } else {
                            showNode(nodeToSelect);
                        }
                    }

                    // список меток
                    this.mListAdapterTags.setDataItems(DataManager.getTags());
                    mTextViewTagsEmpty.setText(R.string.log_tags_is_missing);
                }
                setListEmptyViewState(mTextViewNodesEmpty, isEmpty, R.string.title_nodes_is_missing);
            } else {
                setListEmptyViewState(mTextViewNodesEmpty, true, R.string.log_storage_load_error);
            }
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
        ViewUtils.setEnabledIfNotNull(mMenuItemStorageReload, isAvailable);
        ViewUtils.setEnabledIfNotNull(mMenuItemSearchViewRecords, isAvailable);
    }

    /**
     * Открытие активности для первоначального выбора пути хранилища в файловой системе.
     */
    private void showStorageFolderChooser(boolean isNew) {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, getString(R.string.title_storage_folder));
        intent.putExtra(FolderPicker.EXTRA_LOCATION, SettingsManager.getStoragePath());
        startActivityForResult(intent, (isNew) ? REQUEST_CODE_CREATE_STORAGE : REQUEST_CODE_OPEN_STORAGE);
    }

    @Override
    public void openNode(TetroidNode node) {
        showNode(node);
    }

    /**
     * Отображение записей ветки по Id.
     *
     * @param nodeId
     */
    public void showNode(String nodeId) {
        TetroidNode node = NodesManager.getNode(nodeId);
        if (node != null) {
            showNode(node);
        } else {
            LogManager.log(getString(R.string.log_not_found_node_id) + nodeId, LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Отображение записей ветки.
     *
     * @param node
     */
    private void showNode(TetroidNode node) {
        if (node == null)
            return;
        // проверка нужно ли расшифровать ветку перед отображением
        if (!node.isNonCryptedOrDecrypted()) {
            if (PINManager.isRequestPINCode()) {

                // TODO: сначала просто проверяем пароль
                //  затем спрашиваем ПИН,
                //  а потом уже расшифровываем (!)

                //  Т.е. опять все засунуть в decryptStorage() (?)
                /*PINManager.askPINCode(this, true, () -> {
                    // расшифровываем хранилище
                    decryptStorage(node, true, false, false);
                    showNode(node);
                });*/
                decryptStorage(node, true, false, false);
            } else {
                askPassword(node, true, false, false);
            }
            // выходим, т.к. запрос пароля будет в асинхронном режиме
            return;
        }
        LogManager.log(getString(R.string.log_open_node) + TetroidLog.getIdString(node));
        this.mCurNode = node;
        setCurNode(node);
        showRecords(node.getRecords(), MainPageFragment.MAIN_VIEW_NODE_RECORDS);
    }

    /**
     * Установка и выделение текущей ветки.
     *
     * @param node
     */
    private void setCurNode(TetroidNode node) {
        mViewPagerAdapter.getMainFragment().setCurNode(node);
        mListAdapterNodes.setCurNode(node);
        mListAdapterNodes.notifyDataSetChanged();
        if (node != null && App.isFullVersion()) {
            setFavorIsCurNode(false);
        }
    }

    /**
     * Управление подсветкой ветки Избранное.
     *
     * @param isCur Текущая ветка?
     */
    private void setFavorIsCurNode(boolean isCur) {
        mFavoritesNode.setBackgroundColor(ContextCompat.getColor(this,
                (isCur) ? R.color.colorCurNode : R.color.transparent));
    }

    /**
     * Отображение списка избранных записей.
     */
    private void showFavorites() {
        // проверка нужно ли расшифровать ветку перед отображением
        /*if (FavoritesManager.isCryptedAndNonDecrypted()) {
            // запрос пароля в асинхронном режиме
            askPassword(FavoritesManager.FAVORITES_NODE);
        } else*/
        {
            // выделяем ветку Избранное, только если загружено не одно Избранное
            if (!App.IsLoadedFavoritesOnly) {
                setCurNode(null);
                setFavorIsCurNode(true);
            }
            showRecords(FavoritesManager.getFavoritesRecords(), MainPageFragment.MAIN_VIEW_FAVORITES);
        }
    }

    /**
     * Отображение записей по метке.
     *
     * @param position Индекс метки в списке
     */
    private void showTagRecords(int position) {
        TetroidTag tag = mListAdapterTags.getItem(position).getValue();
        showTag(tag);
    }

    private void showTag(TetroidTag tag) {
        if (tag == null) {
            LogManager.log(R.string.log_tag_is_null, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        setCurNode(null);
        this.mCurTag = tag;
        LogManager.log(getString(R.string.log_open_tag_records) + tag);
        showRecords(tag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS);
    }

    /**
     * Отображение списка записей.
     *
     * @param records
     * @param viewId
     */
    private void showRecords(List<TetroidRecord> records, int viewId) {
        showRecords(records, viewId, true);
    }

    /**
     * Отображение списка записей.
     *
     * @param records
     * @param viewId
     * @param dropSearch Нужно ли закрыть фильтрацию SearchView
     */
    private void showRecords(List<TetroidRecord> records, int viewId, boolean dropSearch) {
        // сбрасываем фильтрацию при открытии списка записей
        if (dropSearch && !mSearchViewRecords.isIconified()) {
            // сбрасываем SearchView;
            // но т.к. при этом срабатывает событие onClose, нужно избежать повторной загрузки
            // полного списка записей в его обработчике с помощью проверки mIsDropRecordsFiltering
            this.mIsDropRecordsFiltering = false;
//          mSearchViewRecords.onActionViewCollapsed();
            mSearchViewRecords.setQuery("", false);
            mSearchViewRecords.setIconified(true);
            this.mIsDropRecordsFiltering = true;
        }

        mDrawerLayout.closeDrawers();
        mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
        mViewPagerAdapter.getMainFragment().showRecords(records, viewId);
    }

    private void showRecordFiles(TetroidRecord record) {
        checkCurNode(record);
        mViewPagerAdapter.getMainFragment().showRecordFiles(record);
    }

    /**
     * Обновление текущей ветки, если еще не установлена.
     *
     * @param record
     */
    private void checkCurNode(TetroidRecord record) {
        TetroidNode node = record.getNode();
        if (node != mCurNode) {
            setCurNode(node);
        }
    }

    @Override
    public void openRecordFolder(TetroidRecord record) {
        if (!RecordsManager.openRecordFolder(this, record)) {
            LogManager.log(R.string.log_missing_file_manager, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void openRecord(TetroidRecord record) {
        if (record == null) {
            LogManager.log(R.string.log_record_is_null, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        // проверка нужно ли расшифровать избранную запись перед отображением
        // (т.к. в избранной ветке записи могут быть нерасшифрованные)
        if (record.isFavorite() && !record.isNonCryptedOrDecrypted()) {
            // запрос пароля в асинхронном режиме
            if (PINManager.isRequestPINCode()) {
                decryptStorage(FavoritesManager.FAVORITES_NODE, true, SettingsManager.isLoadFavoritesOnly(), true);
            } else {
                askPassword(FavoritesManager.FAVORITES_NODE, true, SettingsManager.isLoadFavoritesOnly(), true);
            }
        } else {
            openRecord(record.getId());
        }
    }

    /**
     * Обновление списка веток.
     */
    @Override
    public void updateNodes() {
        mListAdapterNodes.notifyDataSetChanged();
    }

    /**
     * Обновление списка меток.
     */
    @Override
    public void updateTags() {
        mListAdapterTags.setDataItems(DataManager.getTags());
    }

    /**
     * Обновление ветки Избранное.
     */
    @Override
    public void updateFavorites() {
        List<TetroidRecord> favorites = FavoritesManager.getFavoritesRecords();
        if (favorites == null)
            return;
        int size = favorites.size();

        TextView tvName = findViewById(R.id.favorites_name);
        tvName.setTextColor(ContextCompat.getColor(this,
                (size > 0) ? R.color.colorBaseText : R.color.colorLightText));
        TextView favoritesCountView = findViewById(R.id.favorites_count);
        favoritesCountView.setText(String.format(Locale.getDefault(), "[%d]", size));

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
     * <p>
     * FIXME: Разрешение WRITE_EXTERNAL_STORAGE просить не нужно,
     * т.к. оно и так запрашивается при загрузке хранилища.
     *
     * @param file
     */
    @SuppressLint("MissingPermission")
    @Override
    public void openAttach(TetroidFile file) {
//        if (Build.VERSION.SDK_INT >= 23) {
        // если файл нужно расшифровать во временный каталог, нужно разрешение на запись
        if (file.getRecord().isCrypted() && SettingsManager.isDecryptFilesInTemp()
                && !PermissionManager.writeExtStoragePermGranted(this)) {
            this.mTempFileToOpen = file;
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            LogManager.log(getString(R.string.log_request_perm) + permission, LogManager.Types.INFO);
            ActivityCompat.requestPermissions(this,
                    new String[]{permission}, REQUEST_CODE_PERMISSION_WRITE_TEMP);
            return;
        }
//        }
        // расшифровываем без запроса разрешения во время выполнения, т.к. нужные разрешения
        // уже были выданы при установке приложения
        AttachesManager.openAttach(this, file);
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
            showNode((TetroidNode) item);
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
            LogManager.log(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
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
     * @param nodesHeader
     */
    private void initNodesView(final android.widget.SearchView searchView, View nodesHeader) {
        final TextView tvHeader = nodesHeader.findViewById(R.id.text_view_nodes_header);
        final ImageView ivIcon = nodesHeader.findViewById(R.id.image_view_app_icon);

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
        LogManager.log(String.format(getString(R.string.search_nodes_by_query), query));
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
        setListEmptyViewState(tvEmpty, isVisible, getString(stringId));
    }

    private void setListEmptyViewState(TextView tvEmpty, boolean isVisible, String string) {
        tvEmpty.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        tvEmpty.setText(string);
    }

    private void searchInTags(String query, boolean isSearch) {
        Map<String, TetroidTag> tags;
        if (isSearch) {
            LogManager.log(String.format(getString(R.string.search_tags_by_query), query));
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
     *
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
            public void onSearchClick() {
            }

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
                // "сбрасываем" фильтрацию, но не для только что открытых списков записей
                // (т.к. при открытии списка записей вызывается setIconified=false, при котором вызывается это событие,
                // что приводит к повторному открытию списка записей)
                if (mIsDropRecordsFiltering) {
                    switch (mViewPagerAdapter.getMainFragment().getCurMainViewId()) {
                        case MainPageFragment.MAIN_VIEW_NODE_RECORDS:
                            if (mCurNode != null) {
                                showRecords(mCurNode.getRecords(), MainPageFragment.MAIN_VIEW_NODE_RECORDS, false);
                            }
                            break;
                        case MainPageFragment.MAIN_VIEW_TAG_RECORDS:
                            if (mCurTag != null) {
                                showRecords(mCurTag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS, false);
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
//                MainActivity.this.mIsRecordsFiltered = false;
            }
        };
    }

    /**
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
            case MainPageFragment.MAIN_VIEW_FAVORITES:
                title = getString(R.string.title_favorites);
                showRecordsSearch = false;
                break;
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
     *
     * @param viewId
     */
    private void setSubtitle(int viewId) {
        if (viewId != MainPageFragment.MAIN_VIEW_GLOBAL_FOUND) {
            String[] titles = getResources().getStringArray(R.array.view_type_titles);
            // преобразуем идентификатор view в индекс заголовка
            int titleId = viewId - 1;
            if (titleId >= 0 && titleId < titles.length) {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setTextSize(12);
                tvSubtitle.setText(titles[titleId]);
            } else /*if (titleId < 0)*/ {
                tvSubtitle.setVisibility(View.GONE);
            }
        } else if (mLastScan != null) {
            setSubtitle("\"" + mLastScan.getQuery() + "\"");
        } else {
            tvSubtitle.setVisibility(View.GONE);
        }
    }

    /**
     * Установка подзаголовка активности.
     *
     * @param s
     */
    private void setSubtitle(String s) {
        tvSubtitle.setVisibility(View.VISIBLE);
        tvSubtitle.setTextSize(16);
        tvSubtitle.setText(s);
    }

    public void setRecordsSearchViewVisibility(boolean isVisible) {
        mMenuItemSearchViewRecords.setVisible(isVisible);
    }

    public void setFoundPageVisibility(boolean isVisible) {
        if (!isVisible) {
            mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
        }
        mViewPager.setPagingEnabled(isVisible);
        mTitleStrip.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    /**
     * Создание ветки.
     *
     * @param parentNode Родительская ветка
     * @param pos        Позиция в списке родительской ветки
     * @param isSubNode  Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    private void createNode(TetroidNode parentNode, int pos, boolean isSubNode) {
        NodeAskDialogs.createNodeDialog(this, null, (name) -> {
            TetroidNode trueParentNode = (isSubNode) ? parentNode : parentNode.getParentNode();
            TetroidNode node = NodesManager.createNode(name, trueParentNode);
            if (node != null) {
                if (mListAdapterNodes.addItem(pos, isSubNode)) {
                    TetroidLog.logOperRes(TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
                } else {
                    LogManager.log(getString(R.string.log_create_node_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                }
            } else {
                TetroidLog.logOperErrorMore(TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
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
            LogManager.log(getString(R.string.title_link_was_copied) + url, LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.log(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Копирование ссылки на метку в буфер обмена.
     */
    private void copyTagLink(TetroidTag tag) {
        if (tag != null) {
            String url = tag.createUrl();
            Utils.writeToClipboard(this, getString(R.string.link_to_tag), url);
            LogManager.log(getString(R.string.title_link_was_copied) + url, LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.log(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Переименование ветки.
     *
     * @param node
     */
    private void renameNode(TetroidNode node) {
        NodeAskDialogs.createNodeDialog(this, node, (name) -> {
            if (NodesManager.editNodeFields(node, name)) {
                TetroidLog.logOperRes(TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
//                mListAdapterNodes.notifyDataSetChanged();
                updateNodes();
                if (mCurNode == node) {
                    setTitle(name);
                }
            } else {
                TetroidLog.logOperErrorMore(TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
            }
        });
    }

    /**
     * Удаление ветки.
     *
     * @param node
     */
    private void deleteNode(TetroidNode node, int pos) {
        if (node == null)
            return;
        // запрет на удаление последней ветки в корне
        if (node.getLevel() == 0 && DataManager.getRootNodes().size() == 1) {
            LogManager.log(R.string.log_cannot_delete_root_node, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            return;
        }

        NodeAskDialogs.deleteNode(this, () -> {
            boolean res = NodesManager.deleteNode(node);
            onDeleteNodeResult(node, res, pos, false);
        });
    }

    private void onDeleteNodeResult(TetroidNode node, boolean res, int pos, boolean isCutted) {
        if (res) {
            // удаляем элемент внутри списка
            if (mListAdapterNodes.deleteItem(pos)) {
                TetroidLog.logOperRes(TetroidLog.Objs.NODE, (!isCutted) ? TetroidLog.Opers.DELETE : TetroidLog.Opers.CUT);
            } else {
                LogManager.log(getString(R.string.log_node_delete_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            }
            // убираем список записей удаляемой ветки
            if (mCurNode == node || isNodeInNode(mCurNode, node)) {
                mViewPagerAdapter.getMainFragment().clearView();
                this.mCurNode = null;
            }
            if (node.isCrypted()) {
                // проверяем существование зашифрованных веток
                checkExistenceCryptedNodes();
            }
        } else {
            TetroidLog.logOperErrorMore(TetroidLog.Objs.NODE, (!isCutted) ? TetroidLog.Opers.DELETE : TetroidLog.Opers.CUT);
        }
    }

    /**
     * Проверка содержится ли ветка node в ветке nodeAsParent.
     *
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
     *
     * @param node
     * @param pos  Позиция элемента в списке
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
                if (mListAdapterNodes.swapItems(pos, posInNode, (isUp) ? posInNode - 1 : posInNode + 1)) {
                    TetroidLog.logOperRes(TetroidLog.Objs.NODE, TetroidLog.Opers.MOVE);
                } else {
                    LogManager.log(getString(R.string.log_node_move_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                }
            } else if (res < 0) {
                TetroidLog.logOperErrorMore(TetroidLog.Objs.NODE, TetroidLog.Opers.MOVE);
            }
        }
    }

    /**
     * Развернуть все подветки у ветки.
     *
     * @param pos
     */
    private void expandSubNodes(int pos) {
        mListAdapterNodes.extendNodeSubnodes(pos, NestType.MULTIPLE);
    }

    /**
     * Копирование ветки.
     *
     * @param node
     */
    private void copyNode(TetroidNode node) {
        if (NodesManager.hasNonDecryptedNodes(node)) {
            Message.show(this, getString(R.string.log_enter_pass_first), Toast.LENGTH_LONG);
            return;
        }
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(node);
        TetroidLog.logOperRes(TetroidLog.Objs.NODE, TetroidLog.Opers.COPY);
    }

    /**
     * Вырезание ветки из родительской ветки.
     *
     * @param node
     */
    private void cutNode(TetroidNode node, int pos) {
        if (NodesManager.hasNonDecryptedNodes(node)) {
            Message.show(this, getString(R.string.log_enter_pass_first), Toast.LENGTH_LONG);
            return;
        }
        // добавляем в "буфер обмена"
        TetroidClipboard.cut(node);
        // удаляем ветку из родительской ветки вместе с записями
        boolean res = NodesManager.cutNode(node);
        onDeleteNodeResult(node, res, pos, false);
    }

    /**
     * Вставка ветки.
     *
     * @param parentNode Родительская ветка
     * @param pos        Позиция в списке родительской ветки
     * @param isSubNode  Если true, значит как подветка, иначе рядом с выделенной веткой
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

        if (NodesManager.insertNode(node, trueParentNode, isCutted)) {
            if (mListAdapterNodes.addItem(pos, isSubNode)) {
                TetroidLog.logOperRes(TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
            } else {
                LogManager.log(getString(R.string.log_create_node_list_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            }
        } else {
            TetroidLog.logOperErrorMore(TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
        }
    }

    private void encryptNode(TetroidNode node) {
        PassManager.checkStoragePass(this, node, new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
                mCurTask = new CryptNodeTask(node, true).run();
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private void dropEncryptNode(TetroidNode node) {
        PassManager.checkStoragePass(this, node, new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
                mCurTask = new CryptNodeTask(node, false).run();
            }

            @Override
            public void onCancel() {
            }
        });
    }

    /**
     * Проверка существования зашифрованных веток.
     */
    private void checkExistenceCryptedNodes() {
        if (!NodesManager.isExistCryptedNodes(true)) {
            AskDialogs.showYesDialog(this, () -> PassManager.clearSavedPass(),
                    R.string.ask_clear_pass_database_ini);
        }
    }


    /**
     * Отображение всплывающего (контексного) меню ветки.
     * <p>
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
//        activateMenuItem(menu.findItem(R.id.action_create_node), isNonCrypted);
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
        activateMenuItem(menu.findItem(R.id.action_info), isNonCrypted);

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
                    dropEncryptNode(node);
                    return true;
                case R.id.action_expand_node:
                    expandSubNodes(pos);
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
                case R.id.action_info:
                    NodeAskDialogs.createNodeInfoDialog(this, node);
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
     * <p>
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
        intent.putExtra(FolderPicker.EXTRA_TITLE, getString(R.string.title_select_file_to_upload));
        intent.putExtra(FolderPicker.EXTRA_LOCATION, DataManager.getLastFolderOrDefault(this, false));
        intent.putExtra(FolderPicker.EXTRA_PICK_FILES, true);
        startActivityForResult(intent, REQUEST_CODE_FILE_PICKER);
    }

    /**
     *
     */
    @Override
    public void openFolderPicker() {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, getString(R.string.title_save_file_to));
        intent.putExtra(FolderPicker.EXTRA_LOCATION, DataManager.getLastFolderOrDefault(this, false));
        intent.putExtra(FolderPicker.EXTRA_PICK_FILES, false);
        startActivityForResult(intent, REQUEST_CODE_FOLDER_PICKER);
    }

    /**
     * Обработка возвращаемого результата других активностей.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            // проверяем нужно ли отслеживать структуру хранилища
//            TetroidFileObserver.startOrStopObserver(SettingsManager.isCheckOutsideChanging(), mOutsideChangingHandler);
            Bundle bundle = new Bundle();
            bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START_OR_STOP);
            bundle.putBoolean(FileObserverService.EXTRA_IS_START, SettingsManager.isCheckOutsideChanging());
            FileObserverService.sendCommand(this, bundle);

            // скрываем пункт меню Синхронизация, если отключили
            ViewUtils.setVisibleIfNotNull(mMenuItemStorageSync, SettingsManager.isSyncStorage());

            if (data != null) {
                // перезагружаем хранилище, если изменили путь
                if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_REINIT_STORAGE, false)) {
                    boolean toCreate = data.getBooleanExtra(SettingsFragment.EXTRA_IS_CREATE_STORAGE, false);
                    AskDialogs.showReloadStorageDialog(this, toCreate, true, () -> {
                        if (toCreate) {
                            createStorage(SettingsManager.getStoragePath()/*, true*/);
                        } else {
                            reinitStorage();
                        }
                    });
                } else if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_PASS_CHANGED, false)) {
                    // обновляем списки, т.к. хранилище должно было расшифроваться
                    updateNodes();
                    updateTags();
                }
            }
        } else if (requestCode == REQUEST_CODE_RECORD_ACTIVITY) {
            onRecordActivityResult(resultCode, data);
        } else if (requestCode == REQUEST_CODE_SEARCH_ACTIVITY && resultCode == RESULT_OK) {
            ScanManager scan = data.getParcelableExtra(SearchActivity.EXTRA_KEY_SCAN_MANAGER);
            startGlobalSearch(scan);
        } else if ((requestCode == REQUEST_CODE_OPEN_STORAGE || requestCode == REQUEST_CODE_CREATE_STORAGE)
                && resultCode == RESULT_OK) {
            String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
            if (requestCode == REQUEST_CODE_OPEN_STORAGE)
                initOrSyncStorage(folderPath);
            else
                createStorage(folderPath/*, true*/);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(folderPath);
        } else if (requestCode == REQUEST_CODE_SYNC_STORAGE) {
            onSyncStorageFinish(resultCode == RESULT_OK);
        } else if (requestCode == REQUEST_CODE_FILE_PICKER && resultCode == RESULT_OK) {
            String fileFullName = data.getStringExtra(FolderPicker.EXTRA_DATA);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(FileUtils.getFileFolder(fileFullName));
            this.mCurTask = new AttachFileTask(mViewPagerAdapter.getMainFragment().getCurRecord()).run(fileFullName);
        } else if (requestCode == REQUEST_CODE_FOLDER_PICKER && resultCode == RESULT_OK) {
            String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(folderPath);
            this.mCurTask = new SaveFileTask(mViewPagerAdapter.getMainFragment().getCurFile()).run(folderPath);
        }

    }

    /**
     * Обработка возвращаемого результата активности записи.
     *
     * @param data
     * @param resCode
     */
    private void onRecordActivityResult(int resCode, Intent data) {
        if (data == null) {
            return;
        }
        // проверяем нужно ли отслеживать структуру хранилища
//        TetroidFileObserver.startOrStopObserver(SettingsManager.isCheckOutsideChanging(), mOutsideChangingHandler);
        Bundle bundle = new Bundle();
        bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START_OR_STOP);
        bundle.putBoolean(FileObserverService.EXTRA_IS_START, SettingsManager.isCheckOutsideChanging());
        FileObserverService.sendCommand(this, bundle);

        // обновляем списки, если редактировали свойства записи
        if (data.getBooleanExtra(RecordActivity.EXTRA_IS_FIELDS_EDITED, false)) {
            mViewPagerAdapter.getMainFragment().onRecordFieldsUpdated();
        }
        switch (resCode) {
            case RecordActivity.RESULT_REINIT_STORAGE:
                if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_CREATE_STORAGE, false)) {
                    createStorage(SettingsManager.getStoragePath()/*, true*/);
                } else {
                    reinitStorage();
                }
                break;
            case RecordActivity.RESULT_PASS_CHANGED:
                if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_PASS_CHANGED, false)) {
                    // обновляем списки, т.к. хранилище должно было расшифроваться
                    updateNodes();
                    updateTags();
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
                TetroidRecord record = RecordsManager.getRecord(recordId2);
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
                    LogManager.log(String.format(getString(R.string.search_tag_not_found_mask), tagName),
                            LogManager.Types.WARNING, Toast.LENGTH_LONG);
                }
                break;
            case RecordActivity.RESULT_DELETE_RECORD:
                String recordId3 = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                if (recordId3 != null) {
                    TetroidRecord record2 = RecordsManager.getRecord(recordId3);
                    mViewPagerAdapter.getMainFragment().deleteRecordExactly(record2);
                }
                break;
        }
    }

    /**
     * Запуск глобального поиска.
     *
     * @param scan
     */
    private void startGlobalSearch(ScanManager scan) {
        this.mLastScan = scan;
        this.mCurTask = new GlobalSearchTask(scan).run();
    }

    /**
     * Открытие объекта из поисковой выдачи в зависимости от его типа.
     *
     * @param found
     */
    @Override
    public void openFoundObject(ITetroidObject found) {
        int type = found.getType();
        switch (type) {
            case FoundType.TYPE_RECORD:
                openRecord((TetroidRecord) found);
                break;
            case FoundType.TYPE_FILE:
                showRecordFiles(((TetroidFile) found).getRecord());
                break;
            case FoundType.TYPE_NODE:
                showNode((TetroidNode) found);
                break;
            case FoundType.TYPE_TAG:
                showTag((TetroidTag) found);
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
    public void research() {
        showGlobalSearchActivity(null);
    }

    @Override
    public void openMainPage() {
        mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_WRITE_STORAGE: {
                if (permGranted) {
                    LogManager.log(R.string.log_write_ext_storage_perm_granted, LogManager.Types.INFO);
                    startInitStorage(false);
                } else {
                    LogManager.log(R.string.log_missing_read_ext_storage_permissions, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
                }
            }
            break;
            case REQUEST_CODE_PERMISSION_WRITE_TEMP: {
                if (permGranted) {
                    LogManager.log(R.string.log_write_ext_storage_perm_granted, LogManager.Types.INFO);
                    openAttach(mTempFileToOpen);
                } else {
                    LogManager.log(R.string.log_missing_write_ext_storage_permissions, LogManager.Types.WARNING, Toast.LENGTH_SHORT);
                }
            }
        }
    }

    /**
     * Вызывается при submit на RecordsSearchView (вместо пересоздания всей активности).
     *
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
        if (action.equals(FileObserverService.ACTION_OBSERVER_EVENT_COME)) {
            // обработка внешнего изменения дерева записей
            mOutsideChangingHandler.run(true);
        } else if (action.equals(Intent.ACTION_SEARCH)) {
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
                    LogManager.log(R.string.log_not_passed_text, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.log(getString(R.string.log_receiving_intent_text), LogManager.Types.INFO);
            } else if (type.startsWith("image/")) {
                // изображение
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri == null) {
                    LogManager.log(R.string.log_not_passed_image_uri, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.log(String.format(getString(R.string.log_receiving_intent_image_mask), imageUri), LogManager.Types.INFO);
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
                    LogManager.log(R.string.log_not_passed_image_uri, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.log(String.format(getString(R.string.log_receiving_intent_images_mask), uris.size()), LogManager.Types.INFO);
                showIntentDialog(intent, false, null, uris);
            }
        }
    }

    /**
     *
     * @param intent
     * @param isText
     * @param text
     * @param imagesUri
     */
    private void showIntentDialog(Intent intent, boolean isText, String text, ArrayList<Uri> imagesUri) {

        if (App.IsLoadedFavoritesOnly) {
            // если загружено только избранное, то нужно сначала загрузить все ветки,
            // чтобы добавить текст/картинку в одну из записей или в новую запись одной из веток
            Spanned mes = Utils.fromHtml(String.format(getString(R.string.text_load_nodes_before_receive_mask),
                    getString((isText) ? R.string.word_received_text :
                            (imagesUri != null && imagesUri.size() > 1) ? R.string.word_received_images
                                    : R.string.word_received_image)));
            Dialogs.showAlertDialog(this, mes,
                    () -> {
                        // сохраняем Intent и загружаем хранилище
                        this.mReceivedIntent = intent;
                        loadAllNodes();
                    });
            return;
        }

        IntentDialog.createDialog(this, isText, (receivedData) -> {
            if (receivedData.isCreate()) {
                // получаем какую-нибудь ветку
                final TetroidNode node = NodesManager.getDefaultNode();
                if (node != null) {
                    if (node.isCrypted()) {
                        PassManager.checkStoragePass(this, node, new Dialogs.IApplyCancelResult() {
                            @Override
                            public void onApply() {
                                // расшифровуем хранилище, если ветка зашифрована
                                if (DataManager.isCrypted()) {
                                    initStorage(null, true, false, false);
                                }
                                if (DataManager.isDecrypted()) {
                                    createRecordFromIntent(intent, isText, text, imagesUri, receivedData, node);
                                }
                            }
                            @Override
                            public void onCancel() {
                            }
                        });
                    } else {
                        createRecordFromIntent(intent, isText, text, imagesUri, receivedData, node);
                    }
                } else {
                    LogManager.log(R.string.log_no_nodes_in_storage, LogManager.Types.ERROR);
                }
            } else {
                // TODO: реализовать выбор имеющихся записей
            }
        });

    }

    private void createRecordFromIntent(Intent intent, boolean isText, String text, ArrayList<Uri> imagesUri,
                                        ReceivedData receivedData, TetroidNode node) {
        // имя записи
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String url = null;
        if (Build.VERSION.SDK_INT >= 17) {
            url = intent.getStringExtra(Intent.EXTRA_ORIGINATING_URI);
        }
        // создаем запись
        TetroidRecord record = RecordsManager.createRecord(subject, url, text, node);
        if (record == null) {
            return;
        }
        // открываем ветку, в которую добавили запись
        showNode(record.getNode());
        // обновляем список записей, меток, и количества записей ветки
        mViewPagerAdapter.getMainFragment().addNewRecord(record, isText && !receivedData.isAttach());
        // загружаем изображения в каталоги записи
        if (!isText) {
            if (!receivedData.isAttach()) {
                // запускаем активность записи с командой вставки изображений после загрузки
                openRecord(record, imagesUri);

            } else {
                // прикрепляем изображения как файлы
                boolean hasError = false;
                for (Uri uri : imagesUri) {
                    if (AttachesManager.attachFile(FileUtils.getPathFromUri(this, uri), record) == null) {
                        hasError = true;
                    }
                }
                if (hasError) {
                    LogManager.log(R.string.log_files_attach_error, LogManager.Types.WARNING, Toast.LENGTH_LONG);
                }
                // обновляем список файлов
                mDrawerLayout.closeDrawers();
                mViewPagerAdapter.getMainFragment().showRecordFiles(record);
            }
        }
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
            LogManager.log(R.string.search_records_search_select_node, Toast.LENGTH_LONG);
        }
    }

    private void searchInTagRecords(String query) {
        if (mCurTag != null) {
            searchInRecords(query, mCurTag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS);
        } else {
            LogManager.log(R.string.search_records_select_tag, Toast.LENGTH_LONG);
        }
    }

    private void searchInRecords(String query, List<TetroidRecord> records, int viewId) {
        String log = (viewId == MainPageFragment.MAIN_VIEW_NODE_RECORDS)
                ? String.format(getString(R.string.search_records_in_node_by_query), mCurNode.getName(), query)
                : String.format(getString(R.string.search_records_in_tag_by_query), mCurTag.getName(), query);
        LogManager.log(log);
        List<TetroidRecord> found = ScanManager.searchInRecordsNames(records, query);
//        showRecords(found, MainPageFragment.VIEW_FOUND_RECORDS);
        showRecords(found, viewId, false);
        if (found.isEmpty()) {
            String emptyText = (viewId == MainPageFragment.MAIN_VIEW_NODE_RECORDS)
                    ? String.format(getString(R.string.search_records_in_node_not_found_mask), query, mCurNode.getName())
                    : String.format(getString(R.string.search_records_in_tag_not_found_mak), query, mCurTag.getName());
            mViewPagerAdapter.getMainFragment().setRecordsEmptyViewText(emptyText);
            this.mLastSearchQuery = query;
            mViewPagerAdapter.getMainFragment().showGlobalSearchButton(true);
        } else {
            mViewPagerAdapter.getMainFragment().showGlobalSearchButton(false);
        }
    }

    private void searchInRecordFiles(String query) {
        TetroidRecord curRecord = mViewPagerAdapter.getMainFragment().getCurRecord();
        if (curRecord != null) {
            searchInFiles(query, curRecord);
        } else {
            LogManager.log(getString(R.string.log_cur_record_is_not_set), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    private void searchInFiles(String query, TetroidRecord record) {
        LogManager.log(String.format(getString(R.string.search_files_by_query), record.getName(), query));
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
        if (mCurTask != null && mCurTask.isRunning()) {
            // если выполняется задание, то не реагируем на нажатие кнопки Back
            return;
        }
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
        this.mMenuItemStorageReload = menu.findItem(R.id.action_storage_reload);
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
            case R.id.action_record_node:
                showNode(mCurNode);
                return true;
            case R.id.action_global_search:
                showGlobalSearchActivity(null);
                return true;
            case R.id.action_storage_sync:
                startStorageSync(DataManager.getStoragePath());
                return true;
            case R.id.action_storage_info:
                showStorageInfoActivity();
                return true;
            case R.id.action_storage_reload:
                reloadStorageAsk();
                return true;
//            case R.id.action_fullscreen:
//                toggleFullscreen(false);
//                return true;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, REQUEST_CODE_SETTINGS_ACTIVITY);
                return true;
//            case R.id.action_about_app:
//                ViewUtils.startActivity(this, AboutActivity.class, null);
//                return true;
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
        LogManager.log(R.string.log_app_exit, LogManager.Types.INFO);
        // сохраняем выбранную ветку
        saveLastSelectedNode();
        // останавливаем отслеживание изменения структуры хранилища
//        TetroidFileObserver.stopObserver();
        FileObserverService.sendCommand(this, FileObserverService.ACTION_STOP);

        // очищаем память
        DataManager.destruct();
    }

    /**
     * Сохранение последней выбранной ветки.
     */
    private void saveLastSelectedNode() {
        if (SettingsManager.isKeepLastNode()) {
            TetroidNode curNode =
                    (mViewPagerAdapter.getMainFragment().getCurMainViewId() == MainPageFragment.MAIN_VIEW_FAVORITES)
                            ? FavoritesManager.FAVORITES_NODE : mCurNode;
            String nodeId = (curNode != null) ? curNode.getId() : null;
            SettingsManager.setLastNodeId(nodeId);
        }
    }

    private void showStorageInfoActivity() {
        if (App.IsLoadedFavoritesOnly) {
            Message.show(this, getString(R.string.title_need_load_nodes), Toast.LENGTH_LONG);
        } else {
            ViewUtils.startActivity(this, InfoActivity.class, null);
        }
    }

    private void showGlobalSearchActivity(String query) {
        if (App.IsLoadedFavoritesOnly) {
            Message.show(this, getString(R.string.title_need_load_nodes), Toast.LENGTH_LONG);
        } else {
            Intent intent = new Intent(this, SearchActivity.class);
            if (query != null) {
                intent.putExtra(EXTRA_QUERY, query);
            }
            intent.putExtra(EXTRA_CUR_NODE_IS_NOT_NULL, (mCurNode != null));
            startActivityForResult(intent, REQUEST_CODE_SEARCH_ACTIVITY);
        }
    }

    @Override
    public void showGlobalSearchWithQuery() {
        showGlobalSearchActivity(mLastSearchQuery);
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

    private boolean taskPreExecute(int sRes) {
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        boolean isDrawerOpened = mDrawerLayout.isDrawerOpen(Gravity.LEFT);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mTextViewProgress.setText(sRes);
        mLayoutProgress.setVisibility(View.VISIBLE);
        return isDrawerOpened;
    }

    private void taskPostExecute(boolean isDrawerOpened) {
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (isDrawerOpened) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
        mLayoutProgress.setVisibility(View.INVISIBLE);
    }

    /**
     * Задание (параллельный поток), в котором выполняется загрузка хранилища.
     */
    private class ReadStorageTask extends TetroidTask<Void,Void,Boolean> {

        boolean mIsDecrypt;
        boolean mIsFavoritesOnly;
        boolean mIsOpenLastNode;

        ReadStorageTask (boolean isDecrypt, boolean isFavorites, boolean isOpenLastNode) {
            super(MainActivity.this);
            this.mIsDecrypt = isDecrypt;
            this.mIsFavoritesOnly = isFavorites;
            this.mIsOpenLastNode = isOpenLastNode;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.task_storage_loading);
        }

        @Override
        protected Boolean doInBackground(Void... values) {
            return DataManager.readStorage(mIsDecrypt, mIsFavoritesOnly);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            taskPostExecute(true);
            if (res) {
                // устанавливаем глобальную переменную
                App.IsLoadedFavoritesOnly = mIsFavoritesOnly;
                String mes = getString((mIsFavoritesOnly)
                        ? R.string.log_storage_favor_loaded_mask
                            : (mIsDecrypt) ? R.string.log_storage_loaded_decrypted_mask
                                : R.string.log_storage_loaded_mask);
                LogManager.log(String.format(mes, DataManager.getStorageName()), Toast.LENGTH_SHORT);
            } else {
                LogManager.log(getString(R.string.log_failed_storage_load) + DataManager.getStoragePath(),
                        LogManager.Types.WARNING, Toast.LENGTH_LONG);
            }
            // инициализация контролов
            initGUI(res, mIsFavoritesOnly, mIsOpenLastNode);
            // действия после загрузки хранилища
            if (res) {
                afterStorageInited();
            }
        }
    }

    /**
     * Задание, в котором выполняется расшифровка уже загруженного хранилища.
     */
    private class DecryptStorageTask extends TetroidTask<Void,Void,Boolean> {

        boolean mIsDrawerOpened;
        TetroidNode mNode;

        DecryptStorageTask(TetroidNode node) {
            super(MainActivity.this);
            this.mNode = node;
        }

        @Override
        protected void onPreExecute() {
            this.mIsDrawerOpened = taskPreExecute(R.string.task_storage_decrypting);
            TetroidLog.logOperStart(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT);
        }

        @Override
        protected Boolean doInBackground(Void... values) {
            return DataManager.decryptStorage(false);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            taskPostExecute(mIsDrawerOpened);
            if (res) {
                LogManager.log(R.string.log_storage_decrypted, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            } else {
                TetroidLog.logDuringOperErrors(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, Toast.LENGTH_LONG);
            }
            afterStorageDecrypted(mNode);
        }
    }

    /**
     * Задание, в котором выполняется зашифровка/сброс шифровки веток.
     */
    public class CryptNodeTask extends TetroidTask<Void,String,Integer> {

        boolean mIsDrawerOpened;
        TetroidNode mNode;
        boolean mIsEncrypt;
        boolean mWasCrypted;
        TetroidLog.Opers mOper;

        CryptNodeTask(TetroidNode node, boolean isEncrypt) {
            super(MainActivity.this);
            this.mNode = node;
            this.mIsEncrypt = isEncrypt;
            this.mWasCrypted = node.isCrypted();
            this.mOper = (isEncrypt) ? TetroidLog.Opers.ENCRYPT : TetroidLog.Opers.DROPCRYPT;
        }

        @Override
        protected void onPreExecute() {
            this.mIsDrawerOpened = taskPreExecute(
                    (mIsEncrypt) ? R.string.task_node_encrypting : R.string.task_node_drop_crypting);
            TetroidLog.logOperStart(TetroidLog.Objs.NODE,
                    (mIsEncrypt) ? TetroidLog.Opers.ENCRYPT : TetroidLog.Opers.DROPCRYPT, mNode);
        }

        @Override
        protected Integer doInBackground(Void... values) {
            // сначала расшифровываем хранилище
            if (DataManager.isCrypted()) {
                setStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, TaskStage.Stages.START);
                if (DataManager.decryptStorage(false)) {
                    setStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, TaskStage.Stages.SUCCESS);
                } else {
                    setStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, TaskStage.Stages.FAILED);
                    return -2;
                }
            }
            // только если хранилище расшифровано
            if (DataManager.isDecrypted()) {
                setStage(TetroidLog.Objs.NODE, mOper, TaskStage.Stages.START);
                return (((mIsEncrypt) ? DataManager.encryptNode(mNode) : DataManager.dropCryptNode(mNode))) ? 1 : -1;
            }
            return 0;
        }

        private void setStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage) {
            TaskStage taskStage = new TaskStage(CryptNodeTask.class, obj, oper, stage);
            String mes = TetroidLog.logTaskStage(taskStage);
            publishProgress(mes);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String mes = values[0];
            mTextViewProgress.setText(mes);
        }

        @Override
        protected void onPostExecute(Integer res) {
            taskPostExecute(mIsDrawerOpened);
            if (res > 0) {
                TetroidLog.logOperRes(TetroidLog.Objs.NODE, mOper);
                if (!mIsEncrypt && mWasCrypted) {
                    // проверяем существование зашифрованных веток
                    checkExistenceCryptedNodes();
                }
            } else {
                TetroidLog.logOperErrorMore(TetroidLog.Objs.NODE, mOper);
            }
            afterStorageDecrypted(mNode);
        }
    }

    /**
     * Задание, в котором выполняется прикрепление нового файла в записи.
     */
    public class AttachFileTask extends TetroidTask<String,Void,TetroidFile> {

        TetroidRecord mRecord;

        AttachFileTask(TetroidRecord record) {
            super(MainActivity.this);
            this.mRecord = record;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.task_attach_file);
        }

        @Override
        protected TetroidFile doInBackground(String... values) {
            String fileFullName = values[0];
            return AttachesManager.attachFile(fileFullName, mRecord);
        }

        @Override
        protected void onPostExecute(TetroidFile res) {
            taskPostExecute(false);
            mViewPagerAdapter.getMainFragment().attachFile(res);
        }
    }

    /**
     * Задание, в котором выполняется сохранение файла по выбранному пути.
     */
    public class SaveFileTask extends TetroidTask<String,Void,Boolean> {

        TetroidFile mFile;

        SaveFileTask(TetroidFile file) {
            super(MainActivity.this);
            this.mFile = file;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.task_file_saving);
        }

        @Override
        protected Boolean doInBackground(String... values) {
            String path = values[0];
            return AttachesManager.saveFile(mFile, path);
        }

        @Override
        protected void onPostExecute(Boolean res) {
            taskPostExecute(false);
            mViewPagerAdapter.getMainFragment().onSaveFileResult(res);
        }
    }

    /**
     * Задание, в котором выполняется глобальный поиск по объектам.
     */
    private class GlobalSearchTask extends TetroidTask<Void, Void,HashMap<ITetroidObject,FoundType>> {

        private ScanManager mScan;

        GlobalSearchTask(ScanManager scan) {
            super(MainActivity.this);
            this.mScan = scan;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.global_searching);
            LogManager.log(String.format(getString(R.string.global_search_start), mScan.getQuery()));
        }

        @Override
        protected HashMap<ITetroidObject, FoundType> doInBackground(Void... values) {
            return mScan.globalSearch(mCurNode);
        }

        @Override
        protected void onPostExecute(HashMap<ITetroidObject,FoundType> found) {
            taskPostExecute(false);
            if (found == null) {
                LogManager.log(getString(R.string.log_global_search_return_null), Toast.LENGTH_SHORT);
                return;
            } else if (mScan.isSearchInNode() && mScan.getNode() != null) {
                LogManager.log(String.format(getString(R.string.global_search_by_node_result),
                        mScan.getNode().getName()), Toast.LENGTH_SHORT);
            }
            // уведомляем, если не смогли поискать в зашифрованных ветках
            if (mScan.isExistCryptedNodes()) {
                LogManager.log(R.string.log_found_crypted_nodes, Toast.LENGTH_SHORT);
            }
            LogManager.log(String.format(getString(R.string.global_search_end), found.size()));
            mViewPagerAdapter.getFoundFragment().setFounds(found, mScan);
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

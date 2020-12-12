package com.gee12.mytetroid.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.TetroidTask2;
import com.gee12.mytetroid.adapters.MainPagerAdapter;
import com.gee12.mytetroid.adapters.NodesListAdapter;
import com.gee12.mytetroid.adapters.TagsListAdapter;
import com.gee12.mytetroid.data.AttachesManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.FavoritesManager;
import com.gee12.mytetroid.data.ICallback;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.PassManager;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.data.StorageManager;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.NodeDialogs;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.fragments.SettingsFragment;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TaskStage;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;
import com.gee12.mytetroid.model.ReceivedData;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.services.FileObserverService;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.UriUtils;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.IntentDialog;
import com.gee12.mytetroid.views.MainViewPager;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.SearchViewXListener;
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

/**
 * Главная активность приложения со списком веток, записей и меток.
 */
public class MainActivity extends TetroidActivity implements IMainView {

    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 3;
    public static final int REQUEST_CODE_RECORD_ACTIVITY = 4;
    public static final int REQUEST_CODE_SEARCH_ACTIVITY = 5;
    public static final int REQUEST_CODE_FILE_PICKER = 7;
    public static final int REQUEST_CODE_FOLDER_PICKER = 8;

    public static final String EXTRA_CUR_NODE_IS_NOT_NULL = "EXTRA_CUR_NODE_IS_NOT_NULL";
    public static final String EXTRA_QUERY = "EXTRA_QUERY";

    private DrawerLayout mDrawerLayout;
    private MultiLevelListView mListViewNodes;
    private NodesListAdapter mListAdapterNodes;
    private ListView mListViewTags;
    private TagsListAdapter mListAdapterTags;
    private TetroidNode mCurNode;
    private TetroidTag mCurTag;
    private TextView mTextViewNodesEmpty;
    private TextView mTextViewTagsEmpty;
    private SearchView mSearchViewNodes;
    private SearchView mSearchViewTags;
    private SearchView mSearchViewRecords;
    private Menu mOptionsMenu;
    private MainPagerAdapter mViewPagerAdapter;
    private MainViewPager mViewPager;
    private PagerTabStrip mTitleStrip;
    private View mFavoritesNode;
    private Button mLoadStorageButton;

    private boolean mIsActivityCreated;
    private boolean mIsLoadStorageAfterSync;
    private TetroidFile mTempFileToOpen;
    private boolean mIsDropRecordsFiltering = true;
    private ScanManager mLastScan;
    private String mLastSearchQuery;
    private boolean mIsStorageChangingHandled;
    private ICallback mOutsideChangingHandler;

    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mBroadcastManager;


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

        // выдвигающиеся панели
        this.mDrawerLayout = findViewById(R.id.drawer_layout);
        // задаем кнопку (стрелку) управления шторкой
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
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
                    LogManager.log(this, R.string.ask_storage_changed_outside, ILogger.Types.INFO);
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

        NavigationView nodesNavView = mDrawerLayout.findViewById(R.id.nav_view_left);
        View vNodesHeader = nodesNavView.getHeaderView(0);
        this.mSearchViewNodes = vNodesHeader.findViewById(R.id.search_view_nodes);
        mSearchViewNodes.setVisibility(View.GONE);
        initNodesSeachView(mSearchViewNodes, vNodesHeader);

        // метки
        this.mListViewTags = findViewById(R.id.tags_list_view);
        mListViewTags.setOnItemClickListener(onTagClicklistener);
        mListViewTags.setOnItemLongClickListener(onTagLongClicklistener);
        this.mTextViewTagsEmpty = findViewById(R.id.tags_text_view_empty);
        mListViewTags.setEmptyView(mTextViewTagsEmpty);

        NavigationView tagsNavView = mDrawerLayout.findViewById(R.id.nav_view_right);
        View vTagsHeader = tagsNavView.getHeaderView(0);
        this.mSearchViewTags = vTagsHeader.findViewById(R.id.search_view_tags);
        mSearchViewTags.setVisibility(View.GONE);
        initTagsSearchView(mSearchViewTags, vTagsHeader);

        // избранное
        this.mFavoritesNode = findViewById(R.id.node_favorites);
        this.mLoadStorageButton = findViewById(R.id.button_load);
        mFavoritesNode.setVisibility(View.GONE);
        mLoadStorageButton.setVisibility(View.GONE);
        if (App.isFullVersion()) {
            mFavoritesNode.setOnClickListener(v -> showFavorites());
            mLoadStorageButton.setOnClickListener(v -> {
                StorageManager.loadAllNodes(this);
            });
        }

        initBroadcastReceiver();
    }

    /**
     * Приемник сигнала о внешнем изменении дерева записей.
     */
    private void initBroadcastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(FileObserverService.ACTION_OBSERVER_EVENT_COME)) {
                    // обработка внешнего изменения дерева записей
                    mOutsideChangingHandler.run(true);
                }
            }
        };
        this.mBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FileObserverService.ACTION_OBSERVER_EVENT_COME);
        mBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    /**
     * Обработчик события, когда создался главный фрагмент активности.
     */
    @Override
    public void onMainPageCreated() {
        // принудительно запускаем создание пунтов меню уже после отработки onCreate
        afterOnCreate();
        //
        this.mIsActivityCreated = true;
    }

/*    private void setMenuItemsDefVisible() {
        ViewUtils.setVisibleIfNotNull(mMenuItemStorageSync, SettingsManager.isSyncStorage(this));
    }*/

    /**
     * Обработчик события, когда создались все элементы интерфейса.
     * Вызывается из onCreateOptionsMenu(), т.к. пункты меню, судя по всему, создаются в последнюю очередь.
     */
    @Override
    protected void onGUICreated() {
        // инициализация
        App.init(this);
        mViewPagerAdapter.getMainFragment().onSettingsInited();
//        setMenuItemsDefVisible();

        if (StorageManager.isLoaded()) {
            // тут ничего не пишем.
            // код отображения загруженного хранилище находится в onGUICreated(),
            //  который вызывается после создания пунктов меню активности

            // инициализация контролов
            initGUI(true, StorageManager.isFavoritesMode(), SettingsManager.isKeepLastNode(this));
            // действия после загрузки хранилища
            afterStorageLoaded(true);

        } else {
            // загружаем хранилище, если еще не загружано
            StorageManager.startInitStorage(this, this, false);
        }
    }

    /**
     * Старт загрузки хранилища.
     */
    @Override
    protected void loadStorage(String folderPath) {
        boolean isLoadLastForced = false;
        boolean isCheckFavorMode = true;
        if (folderPath == null) {
            StorageManager.startInitStorage(this, this, isLoadLastForced, isCheckFavorMode);
        } else {
            StorageManager.initOrSyncStorage(this, folderPath, isCheckFavorMode);
        }
    }

    /**
     * Обработчик события после загрузки хранилища.
     */
    public void afterStorageLoaded(boolean res) {
        if (res) {
            // проверяем входящий Intent после загрузки
            checkReceivedIntent(mReceivedIntent);
            // запускаем отслеживание изменения структуры хранилища
            startStorageTreeObserver();
        }
    }

    /**
     * Обработчик изменения структуры хранилища извне.
     */
    private void startStorageTreeObserver() {
        if (SettingsManager.isCheckOutsideChanging(this)) {
            // запускаем мониторинг, только если хранилище загружено
            if (StorageManager.isLoaded()) {
                this.mIsStorageChangingHandled = false;
//            TetroidFileObserver.startStorageObserver(mOutsideChangingHandler);
                Bundle bundle = new Bundle();
                bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START);
                bundle.putString(FileObserverService.EXTRA_FILE_PATH, StorageManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME);
                bundle.putInt(FileObserverService.EXTRA_EVENT_MASK, FileObserver.MODIFY);
                FileObserverService.sendCommand(this, bundle);
            }
        } else {
            FileObserverService.sendCommand(this, FileObserverService.ACTION_STOP);
            FileObserverService.stop(this);
        }
    }

    /**
     * Перезагрузка хранилища (при изменении пути в настройках).
     */
    private void reinitStorage() {
        closeFoundFragment();
        mViewPagerAdapter.getMainFragment().clearView();
        StorageManager.startInitStorage(this, this, true);
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
     * Создание нового хранилища в указанном расположении.
     *
     * @param storagePath
//     * @param checkDirIsEmpty
     */
    @Override
    protected void createStorage(String storagePath/*, boolean checkDirIsEmpty*/) {
        if (StorageManager.createStorage(this, storagePath)) {
            closeFoundFragment();
            mViewPagerAdapter.getMainFragment().clearView();
            mDrawerLayout.openDrawer(Gravity.LEFT);
            // сохраняем путь к хранилищу
//            if (SettingsManager.isLoadLastStoragePath()) {
            /*SettingsManager.setStoragePath(storagePath);*/
//            }
            initGUI(DataManager.createDefault(this), false, false);
//            LogManager.log(getString(R.string.log_storage_created) + mStoragePath, LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            mDrawerLayout.openDrawer(Gravity.LEFT);
            initGUI(false, false, false);
//            LogManager.log(getString(R.string.log_failed_storage_create) + mStoragePath, LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Обработка результата синхронизации хранилища.
     *
     * @param res
     */
    private void onSyncStorageFinish(boolean res) {
        final String storagePath = SettingsManager.getStoragePath(this);
        if (res) {
            LogManager.log(this, R.string.log_sync_successful, Toast.LENGTH_SHORT);
            if (mIsLoadStorageAfterSync)
                initStorage(storagePath);
            else {
                AskDialogs.showSyncDoneDialog(this, true, () -> initStorage(storagePath));
            }
        } else {
            LogManager.log(this, getString(R.string.log_sync_failed), ILogger.Types.WARNING, Toast.LENGTH_LONG);
            if (mIsLoadStorageAfterSync) {
                AskDialogs.showSyncDoneDialog(this, false, () -> initStorage(storagePath));
            }
        }
        this.mIsLoadStorageAfterSync = false;
    }

    private void initStorage(String storagePath) {
        // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
        boolean isFavorites = !DataManager.isLoaded() && SettingsManager.isLoadFavoritesOnly(this)
                || (DataManager.isLoaded() && DataManager.isFavoritesMode());

        if (StorageManager.initStorage(this, storagePath)) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        } else {
            mDrawerLayout.openDrawer(Gravity.LEFT);
            initGUI(false, isFavorites, false);
        }
    }

    public void afterStorageDecrypted(TetroidNode node) {
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
     * @param res   Результат загрузки хранилища.
     * @param isOnlyFavorites
     * @param isOpenLastNode  Нужно ли загружать ветку, сохраненную в опции getLastNodeId(),
     *                        или ветку с избранными записями
     */
    @Override
    public void initGUI(boolean res, boolean isOnlyFavorites, boolean isOpenLastNode) {
        // избранные записи
        mLoadStorageButton.setVisibility((res && isOnlyFavorites) ? View.VISIBLE : View.GONE);
        mListViewNodes.setVisibility((!isOnlyFavorites) ? View.VISIBLE : View.GONE);
        mFavoritesNode.setVisibility((res && App.isFullVersion()) ? View.VISIBLE : View.GONE);
        mTextViewNodesEmpty.setVisibility(View.GONE);
        if (res && App.isFullVersion()) {
            updateFavorites();
        }
        // элементы фильтра веток и меток
        ViewUtils.setVisibleIfNotNull(mSearchViewNodes, !isOnlyFavorites);
        ViewUtils.setVisibleIfNotNull(mSearchViewTags, !isOnlyFavorites);

        if (isOnlyFavorites) {
            // обработка только "ветки" избранных записей
            if (res) {
                // списки записей, файлов
                mViewPagerAdapter.getMainFragment().initListAdapters(this);
                showFavorites();
                // список меток
                mTextViewTagsEmpty.setText(R.string.title_load_all_nodes);
                setListEmptyViewState(mTextViewNodesEmpty, true, R.string.title_load_all_nodes);
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
                if (SettingsManager.isKeepLastNode(this) && !isEmpty && isOpenLastNode) {
                    String nodeId = SettingsManager.getLastNodeId(this);
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
        updateOptionsMenu();
    }

    /**
     * Открытие записей ветки.
     * Реализация метода интерфейса IMainView.
     * @param node
     */
    @Override
    public void openNode(TetroidNode node) {
        showNode(node);
    }

    /**
     * Открытие записей ветки по Id.
     *
     * @param nodeId
     */
    public void showNode(String nodeId) {
        TetroidNode node = NodesManager.getNode(nodeId);
        if (node != null) {
            showNode(node);
        } else {
            LogManager.log(this, getString(R.string.log_not_found_node_id) + nodeId, ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Открытие записей ветки.
     * @param node
     */
    private void showNode(TetroidNode node) {
        if (node == null)
            return;
        // проверка нужно ли расшифровать ветку перед отображением
        if (StorageManager.onNodeDecrypt(this, node))
            return;

        LogManager.log(this, getString(R.string.log_open_node) + TetroidLog.getIdString(this, node));
        this.mCurNode = node;
        setCurNode(node);
        showRecords(node.getRecords(), MainPageFragment.MAIN_VIEW_NODE_RECORDS);
    }

    /**
     * Открытие ветки записи.
     * Если активен режим "Только избранное", то открытие списка избранных записей.
     */
    private void showRecordNode(TetroidRecord record) {
        if (StorageManager.isFavoritesMode()) {
            showFavorites();
        } else if (record != null) {
            showNode(record.getNode());
        } else {
            showNode(mCurNode);
        }
    }

    /**
     * Установка и выделение текущей ветки.
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
     * @param position Индекс метки в списке
     */
    private void showTagRecords(int position) {
        TetroidTag tag = mListAdapterTags.getItem(position).getValue();
        showTag(tag);
    }

    private void showTag(TetroidTag tag) {
        if (tag == null) {
            LogManager.log(this, R.string.log_tag_is_null, ILogger.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        setCurNode(null);
        this.mCurTag = tag;
        LogManager.log(this, getString(R.string.log_open_tag_records) + tag);
        showRecords(tag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS);
    }

    /**
     * Отображение списка записей.
     * @param records
     * @param viewId
     */
    private void showRecords(List<TetroidRecord> records, int viewId) {
        showRecords(records, viewId, true);
    }

    /**
     * Отображение списка записей.
     * @param records
     * @param viewId
     * @param dropSearch Нужно ли закрыть фильтрацию SearchView
     */
    private void showRecords(List<TetroidRecord> records, int viewId, boolean dropSearch) {
        // сбрасываем фильтрацию при открытии списка записей
        if (dropSearch && mSearchViewRecords != null && !mSearchViewRecords.isIconified()) {
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
            LogManager.log(this, R.string.log_missing_file_manager, Toast.LENGTH_LONG);
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

    /**
     * Открытие записи.
     * Реализация метода интерфейса IMainView.
     * @param record
     */
    @Override
    public void openRecord(TetroidRecord record) {
        if (record == null) {
            LogManager.log(this, R.string.log_record_is_null, ILogger.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        // проверка нужно ли расшифровать избранную запись перед отображением
        // (т.к. в избранной ветке записи могут быть нерасшифрованные)
        if (!StorageManager.onRecordDecrypt(this, record)) {
            openRecord(record.getId());
        }
    }

    /**
     * Открытие записи по Id.
     * @param recordId
     */
    public void openRecord(String recordId) {
        Bundle bundle = new Bundle();
        bundle.putString(RecordActivity.EXTRA_OBJECT_ID, recordId);
        openRecord(bundle);
    }

    /**
     * Открытие записи с последующим добавлением в ее содержимое изображений.
     * @param recordId
     * @param imagesUri
     */
    public void openRecordWithImages(String recordId, ArrayList<Uri> imagesUri) {
        Bundle bundle = new Bundle();
        bundle.putString(RecordActivity.EXTRA_OBJECT_ID, recordId);
        bundle.putParcelableArrayList(RecordActivity.EXTRA_IMAGES_URI, imagesUri);
        openRecord(bundle);
    }

    /**
     *
     * @param recordId
     */
    public void openRecordWithAttachedFiles(String recordId) {
        Bundle bundle = new Bundle();
        bundle.putString(RecordActivity.EXTRA_OBJECT_ID, recordId);
        bundle.putString(RecordActivity.EXTRA_ATTACHED_FILES, "");
        openRecord(bundle);
    }

    /**
     * Открытие активности RecordActivity.
     * @param bundle
     */
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
        if (file.getRecord().isCrypted() && SettingsManager.isDecryptFilesInTemp(this)
                && !PermissionManager.writeExtStoragePermGranted(this)) {
            this.mTempFileToOpen = file;
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            LogManager.log(this, getString(R.string.log_request_perm) + permission, ILogger.Types.INFO);
            ActivityCompat.requestPermissions(this,
                    new String[]{permission}, StorageManager.REQUEST_CODE_PERMISSION_WRITE_TEMP);
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
            LogManager.log(this, getString(R.string.log_get_item_is_null), ILogger.Types.ERROR, Toast.LENGTH_LONG);
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
     * Настройка элемента для фильтра веток.
     * @param nodesHeader
     */
    private void initNodesSeachView(final SearchView searchView, View nodesHeader) {
        final TextView tvHeader = nodesHeader.findViewById(R.id.text_view_nodes_header);
        final ImageView ivIcon = nodesHeader.findViewById(R.id.image_view_app_icon);

        new SearchViewXListener(searchView) {
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
            public void onSearchClick() {
                tvHeader.setVisibility(View.GONE);
                ivIcon.setVisibility(View.GONE);
            }

            @Override
            public void onQuerySubmit(String query) {
                searchInNodesNames(query);
            }

            @Override
            public void onSuggestionSelectOrClick(String query) {
                searchInNodesNames(query);
            }
        };
    }

    /**
     * Настройка элемента для фильтра меток.
     * @param tagsHeader
     */
    private void initTagsSearchView(final SearchView searchView, View tagsHeader) {
        final TextView tvHeader = tagsHeader.findViewById(R.id.text_view_tags_header);

        new SearchViewXListener(searchView) {
            @Override
            public void onClose() {
                searchInTags(null, false);
                tvHeader.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSearchClick() {
                tvHeader.setVisibility(View.GONE);
            }

            @Override
            public void onQuerySubmit(String query) {
                searchInTags(query, true);
            }

            @Override
            public void onSuggestionSelectOrClick(String query) {
                searchInTags(query, true);
            }
        };
    }

    /**
     * Фильтр веток по названию ветки.
     * @param query
     */
    private void searchInNodesNames(String query) {
        LogManager.log(this, String.format(getString(R.string.filter_nodes_by_query), query));
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

    /**
     * Фильтр меток по названию.
     * @param query
     * @param isSearch Если false, то происходит сброс фильтра.
     */
    private void searchInTags(String query, boolean isSearch) {
        Map<String, TetroidTag> tags;
        if (isSearch) {
            LogManager.log(this, String.format(getString(R.string.filter_tags_by_query), query));
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
     * Изменение ToolBar по текущей странице.
     * @param curPage
     */
    private void changeToolBarByPage(int curPage) {
        if (curPage == MainViewPager.PAGE_MAIN) {
            mViewPagerAdapter.getMainFragment().restoreLastMainToolbarState();
//            setRecordsSearchViewVisibility(true);
//            invalidateOptionsMenu();
        } else {
            updateMainToolbar(MainPageFragment.MAIN_VIEW_GLOBAL_FOUND, null);
        }
    }

    /**
     * Установка заголовка и подзаголовка ToolBar.
     * @param viewId
     */
    @Override
    public void updateMainToolbar(int viewId, String title) {
        switch (viewId) {
            case MainPageFragment.MAIN_VIEW_GLOBAL_FOUND:
                title = getString(R.string.title_global_search);
                break;
            case MainPageFragment.MAIN_VIEW_NONE:
                title = null;
                break;
            case MainPageFragment.MAIN_VIEW_NODE_RECORDS:
                title = ((mCurNode != null) ? mCurNode.getName() : "");
                break;
            case MainPageFragment.MAIN_VIEW_TAG_RECORDS:
                title = ((mCurTag != null) ? mCurTag.getName() : "");
                break;
//            case MainPageFragment.VIEW_FOUND_RECORDS:
//                break;
            case MainPageFragment.MAIN_VIEW_FAVORITES:
                title = getString(R.string.title_favorites);
                break;
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
            default:
        }
        setTitle(title);
        setSubtitle(viewId);
        updateOptionsMenu();
    }

    /**
     * Установка подзаголовка активности, указывающим на тип отображаемого объекта.
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

    public void setFoundPageVisibility(boolean isVisible) {
        if (!isVisible) {
            mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
        }
        mViewPager.setPagingEnabled(isVisible);
        mTitleStrip.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    /**
     * Создание ветки.
     * @param parentNode Родительская ветка
     * @param pos        Позиция в списке родительской ветки
     * @param isSubNode  Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    private void createNode(TetroidNode parentNode, int pos, boolean isSubNode) {
        NodeDialogs.createNodeDialog(this, null, (name) -> {
            TetroidNode trueParentNode = (isSubNode) ? parentNode : parentNode.getParentNode();
            TetroidNode node = NodesManager.createNode(this, name, trueParentNode);
            if (node != null) {
                if (mListAdapterNodes.addItem(pos, isSubNode)) {
                    TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE, node, false);
                } else {
                    LogManager.log(this, getString(R.string.log_create_node_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
                }
            } else {
                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
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
            LogManager.log(this, getString(R.string.title_link_was_copied) + url, ILogger.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.log(this, getString(R.string.log_get_item_is_null), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Копирование ссылки на метку в буфер обмена.
     */
    private void copyTagLink(TetroidTag tag) {
        if (tag != null) {
            String url = tag.createUrl();
            Utils.writeToClipboard(this, getString(R.string.link_to_tag), url);
            LogManager.log(this, getString(R.string.title_link_was_copied) + url, ILogger.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.log(this, getString(R.string.log_get_item_is_null), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Переименование ветки.
     * @param node
     */
    private void renameNode(TetroidNode node) {
        NodeDialogs.createNodeDialog(this, node, (name) -> {
            if (NodesManager.editNodeFields(this, node, name)) {
                TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
//                mListAdapterNodes.notifyDataSetChanged();
                updateNodes();
                if (mCurNode == node) {
                    setTitle(name);
                }
            } else {
                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
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
            LogManager.log(this, R.string.log_cannot_delete_root_node, ILogger.Types.INFO, Toast.LENGTH_SHORT);
            return;
        }

        NodeDialogs.deleteNode(this, () -> {
            boolean res = NodesManager.deleteNode(this, node);
            onDeleteNodeResult(node, res, pos, false);
        });
    }

    private void onDeleteNodeResult(TetroidNode node, boolean res, int pos, boolean isCutted) {
        if (res) {
            // удаляем элемент внутри списка
            if (mListAdapterNodes.deleteItem(pos)) {
                TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, (!isCutted) ? TetroidLog.Opers.DELETE : TetroidLog.Opers.CUT);
            } else {
                LogManager.log(this, getString(R.string.log_node_delete_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
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
            TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, (!isCutted) ? TetroidLog.Opers.DELETE : TetroidLog.Opers.CUT);
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
            int res = DataManager.swapTetroidObjects(this, subNodes, posInNode, isUp);
            if (res > 0) {
                // меняем местами элементы внутри списка
                if (mListAdapterNodes.swapItems(pos, posInNode, (isUp) ? posInNode - 1 : posInNode + 1)) {
                    TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.MOVE);
                } else {
                    LogManager.log(this, getString(R.string.log_node_move_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
                }
            } else if (res < 0) {
                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.MOVE);
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
     * Копирование ветки.
     * @param node
     */
    private void copyNode(TetroidNode node) {
        if (NodesManager.hasNonDecryptedNodes(node)) {
            Message.show(this, getString(R.string.log_enter_pass_first), Toast.LENGTH_LONG);
            return;
        }
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(node);
        TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.COPY);
    }

    /**
     * Вырезание ветки из родительской ветки.
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
        boolean res = NodesManager.cutNode(this, node);
        onDeleteNodeResult(node, res, pos, false);
    }

    /**
     * Вставка ветки.
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

        if (NodesManager.insertNode(this, node, trueParentNode, isCutted)) {
            if (mListAdapterNodes.addItem(pos, isSubNode)) {
                TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
            } else {
                LogManager.log(this, getString(R.string.log_create_node_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
            }
        } else {
            TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
        }
    }

    /**
     * Зашифровка ветки.
     * @param node
     */
    private void encryptNode(TetroidNode node) {
        if (node == NodesManager.getQuicklyNode()) {
            Message.show(this, getString(R.string.mes_quickly_node_cannot_encrypt));
            return;
        }
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

    /**
     * Сброс шифрования ветки.
     * @param node
     */
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
            AskDialogs.showYesDialog(this, () -> PassManager.clearSavedPass(this),
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
    @SuppressLint("RestrictedApi")
    private void showNodePopupMenu(View v, TetroidNode node, int pos) {
        PopupMenu popupMenu = new PopupMenu(this, v); //, Gravity.CENTER_HORIZONTAL);
        popupMenu.inflate(R.menu.node_context);

        Menu menu = popupMenu.getMenu();
        TetroidNode parentNode = node.getParentNode();
        boolean isNonCrypted = node.isNonCryptedOrDecrypted();
        visibleMenuItem(menu.findItem(R.id.action_expand_node), node.isExpandable() && isNonCrypted);
//        visibleMenuItem(menu.findItem(R.id.action_create_node), isNonCrypted);
        visibleMenuItem(menu.findItem(R.id.action_create_subnode), isNonCrypted);
        visibleMenuItem(menu.findItem(R.id.action_rename), isNonCrypted);
//        visibleMenuItem(menu.findItem(R.id.action_collapse_node), node.isExpandable());
        visibleMenuItem(menu.findItem(R.id.action_move_up), pos > 0);
        int nodesCount = ((parentNode != null) ? parentNode.getSubNodes() : DataManager.getRootNodes()).size();
        visibleMenuItem(menu.findItem(R.id.action_move_down), pos < nodesCount - 1);
        boolean canInsert = TetroidClipboard.hasObject(FoundType.TYPE_NODE);
        visibleMenuItem(menu.findItem(R.id.action_insert), canInsert);
        visibleMenuItem(menu.findItem(R.id.action_insert_subnode), canInsert && isNonCrypted);
        visibleMenuItem(menu.findItem(R.id.action_copy), isNonCrypted);
        boolean canCutDel = node.getLevel() > 0 || DataManager.getRootNodes().size() > 1;
        visibleMenuItem(menu.findItem(R.id.action_cut), canCutDel && isNonCrypted);
        visibleMenuItem(menu.findItem(R.id.action_delete), canCutDel);
        visibleMenuItem(menu.findItem(R.id.action_encrypt_node), !node.isCrypted());
        boolean canNoCrypt = node.isCrypted() && (parentNode == null || !parentNode.isCrypted());
        visibleMenuItem(menu.findItem(R.id.action_no_encrypt_node), canNoCrypt);
        visibleMenuItem(menu.findItem(R.id.action_info), isNonCrypted);

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
                    NodeDialogs.createNodeInfoDialog(this, node);
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

    /**
     * Отображение всплывающего (контексного) меню метки.
     * FIXME: Заменить на использование AlertDialog ? (чтобы посередине экрана)
     *
     * @param v
     * @param tag
     */
    @SuppressLint("RestrictedApi")
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
     * Открытие активности для выбора файла в файловой системе.
     */
    @Override
    public void openFilePicker() {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, getString(R.string.title_select_file_to_upload));
        intent.putExtra(FolderPicker.EXTRA_LOCATION, DataManager.getLastFolderPathOrDefault(this, false));
        intent.putExtra(FolderPicker.EXTRA_PICK_FILES, true);
        startActivityForResult(intent, REQUEST_CODE_FILE_PICKER);
    }

    /**
     * Открытие активности для выбора каталога в файловой системе.
     */
    @Override
    public void openFolderPicker() {
        Intent intent = new Intent(this, FolderPicker.class);
        intent.putExtra(FolderPicker.EXTRA_TITLE, getString(R.string.title_save_file_to));
        intent.putExtra(FolderPicker.EXTRA_LOCATION, DataManager.getLastFolderPathOrDefault(this, false));
        intent.putExtra(FolderPicker.EXTRA_PICK_FILES, false);
        startActivityForResult(intent, REQUEST_CODE_FOLDER_PICKER);
    }

    /**
     * Обработка возвращаемого результата других активностей.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            onSettingsActivityResult(resultCode, data);
        } else if (requestCode == REQUEST_CODE_RECORD_ACTIVITY) {
            onRecordActivityResult(resultCode, data);
        } else if (requestCode == REQUEST_CODE_SEARCH_ACTIVITY && resultCode == RESULT_OK) {
            ScanManager scan = data.getParcelableExtra(SearchActivity.EXTRA_KEY_SCAN_MANAGER);
            startGlobalSearch(scan);
        } else if (requestCode == StorageManager.REQUEST_CODE_SYNC_STORAGE) {
            onSyncStorageFinish(resultCode == RESULT_OK);
        } else if (requestCode == REQUEST_CODE_FILE_PICKER && resultCode == RESULT_OK) {
            String fileFullName = data.getStringExtra(FolderPicker.EXTRA_DATA);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(this, FileUtils.getFileFolder(fileFullName));
            this.mCurTask = new AttachFileTask(mViewPagerAdapter.getMainFragment().getCurRecord()).run(fileFullName);
        } else if (requestCode == REQUEST_CODE_FOLDER_PICKER && resultCode == RESULT_OK) {
            String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(this, folderPath);
            this.mCurTask = new SaveFileTask(mViewPagerAdapter.getMainFragment().getCurFile()).run(folderPath);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Обработка возвращаемого результата активности настроек приложения.
     * @param data
     * @param resCode
     */
    private void onSettingsActivityResult(int resCode, Intent data) {
        // проверяем нужно ли отслеживать структуру хранилища
//            TetroidFileObserver.startOrStopObserver(SettingsManager.isCheckOutsideChanging(), mOutsideChangingHandler);
        /*Bundle bundle = new Bundle();
        bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START_OR_STOP);
        bundle.putBoolean(FileObserverService.EXTRA_IS_START, SettingsManager.isCheckOutsideChanging());
        FileObserverService.sendCommand(this, bundle);*/
        startStorageTreeObserver();

        // скрываем пункт меню Синхронизация, если отключили
//        ViewUtils.setVisibleIfNotNull(mMenuItemStorageSync, SettingsManager.isSyncStorage(this));
        updateOptionsMenu();

        if (data != null) {
            // перезагружаем хранилище, если изменили путь
            if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_REINIT_STORAGE, false)) {
                boolean toCreate = data.getBooleanExtra(SettingsFragment.EXTRA_IS_CREATE_STORAGE, false);
                AskDialogs.showReloadStorageDialog(this, toCreate, true, () -> {
                    if (toCreate) {
                        createStorage(SettingsManager.getStoragePath(this)/*, true*/);
                    } else {
                        reinitStorage();
                    }
                });
            } else if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_LOAD_STORAGE, false)) {
//                StorageManager.setStorageCallback(this);
                StorageManager.startInitStorage(this, this, false);
            } else if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_LOAD_ALL_NODES, false)) {
                StorageManager.loadAllNodes(this);
            } else if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_PASS_CHANGED, false)) {
                // обновляем списки, т.к. хранилище должно было расшифроваться
                updateNodes();
                updateTags();
            }
        }
    }

    /**
     * Обработка возвращаемого результата активности просмотра содержимого записи.
     * @param data
     * @param resCode
     */
    private void onRecordActivityResult(int resCode, Intent data) {
        if (data == null) {
            return;
        }
        // проверяем нужно ли отслеживать структуру хранилища
//        TetroidFileObserver.startOrStopObserver(SettingsManager.isCheckOutsideChanging(), mOutsideChangingHandler);
        /*Bundle bundle = new Bundle();
        bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START_OR_STOP);
        bundle.putBoolean(FileObserverService.EXTRA_IS_START, SettingsManager.isCheckOutsideChanging());
        FileObserverService.sendCommand(this, bundle);*/
        startStorageTreeObserver();

        // обновляем списки, если редактировали свойства записи
        if (data.getBooleanExtra(RecordActivity.EXTRA_IS_FIELDS_EDITED, false)) {
            mViewPagerAdapter.getMainFragment().onRecordFieldsUpdated(null, false);
        }
        switch (resCode) {
            case RecordActivity.RESULT_REINIT_STORAGE:
                if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_CREATE_STORAGE, false)) {
                    createStorage(SettingsManager.getStoragePath(this)/*, true*/);
                } else {
                    reinitStorage();
                    // сбрасываем Intent, чтобы избежать циклической перезагрузки хранилища
                    this.mReceivedIntent = null;
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
                    LogManager.log(this, String.format(getString(R.string.search_tag_not_found_mask), tagName),
                            ILogger.Types.WARNING, Toast.LENGTH_LONG);
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
     * @param scan
     */
    private void startGlobalSearch(ScanManager scan) {
        this.mLastScan = scan;
        this.mCurTask = new GlobalSearchTask(scan).run();
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
//            case StorageManager.REQUEST_CODE_PERMISSION_WRITE_STORAGE: {
//                if (permGranted) {
//                    LogManager.log(this, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO);
//                    StorageManager.startInitStorage(this, this, false);
//                } else {
//                    LogManager.log(this, R.string.log_missing_read_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
//                }
//            }
//            break;
            case StorageManager.REQUEST_CODE_PERMISSION_WRITE_TEMP: {
                if (permGranted) {
                    LogManager.log(this, R.string.log_write_ext_storage_perm_granted, ILogger.Types.INFO);
                    openAttach(mTempFileToOpen);
                } else {
                    LogManager.log(this, R.string.log_missing_write_ext_storage_permissions, ILogger.Types.WARNING, Toast.LENGTH_SHORT);
                }
            }
        }
    }

    /**
     * Обработка входящего Intent.
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
        /*if (action.equals(FileObserverService.ACTION_OBSERVER_EVENT_COME)) {
            // обработка внешнего изменения дерева записей
            mOutsideChangingHandler.run(true);
            
        } else*/ if (action.equals(Intent.ACTION_SEARCH)) {
            // обработка результата голосового поиска
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchViewRecords.setQuery(query, true);

        } else if (action.equals(RecordActivity.ACTION_RECORD)) {
            int resCode = intent.getIntExtra(RecordActivity.EXTRA_RESULT_CODE, 0);
            onRecordActivityResult(resCode, intent);

            /*// открытие ветки только что созданной записи с помощью виджета
            String recordId = intent.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
            if (recordId != null) {
                TetroidRecord record = RecordsManager.getRecord(recordId);
                if (record != null) {
                    showNode(record.getNode());
                } else {
                    LogManager.log(this, getString(R.string.log_not_found_record) + recordId, ILogger.Types.ERROR, Toast.LENGTH_LONG);
                }
            }*/

        } else if (action.equals(Intent.ACTION_SEND)) {
            // прием текста/изображения из другого приложения
            String type = intent.getType();
            if (type == null) {
                return;
            }
            String text = null;
            boolean isText = false;
            ArrayList<Uri> uris = null;
            if (type != null && type.startsWith("text/")) {
                isText = true;
                text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text == null) {
                    LogManager.log(this, R.string.log_not_passed_text, ILogger.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.log(this, getString(R.string.log_receiving_intent_text), ILogger.Types.INFO);
            } else if (type.startsWith("image/")) {
                // изображение
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri == null) {
                    LogManager.log(this, R.string.log_not_passed_image_uri, ILogger.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.log(this, String.format(getString(R.string.log_receiving_intent_image_mask), imageUri), ILogger.Types.INFO);
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
                    LogManager.log(this, R.string.log_not_passed_image_uri, ILogger.Types.WARNING, Toast.LENGTH_LONG);
                    return;
                }
                LogManager.log(this, String.format(getString(R.string.log_receiving_intent_images_mask), uris.size()), ILogger.Types.INFO);
                showIntentDialog(intent, false, null, uris);
            }
        }
    }

    /**
     * Отрытие диалога для вставки объектов из других приложений.
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
                        StorageManager.loadAllNodes(this);
                    });
            return;
        }

        IntentDialog.createDialog(this, isText, (receivedData) -> {
                if (receivedData.isCreate()) {
                    createRecordFromIntent(intent, isText, text, imagesUri, receivedData);
                } else {
                    // TODO: реализовать выбор имеющихся записей
                }
            });

    }

    /**
     * Создание новой записи для вставки объекта из другого приложения.
     * @param intent
     * @param isText
     * @param text
     * @param imagesUri
     * @param receivedData
     */
    private void createRecordFromIntent(Intent intent, boolean isText, String text, ArrayList<Uri> imagesUri,
                                        ReceivedData receivedData) {
        // имя записи
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String url = null;
        if (Build.VERSION.SDK_INT >= 17) {
            url = intent.getStringExtra(Intent.EXTRA_ORIGINATING_URI);
        }
        // создаем запись
        TetroidRecord record = RecordsManager.createTempRecord(this, subject, url, text);
        if (record == null) {
            return;
        }
        if (isText) {
            // запускаем активность просмотра записи
            openRecord(record.getId());
        } else {
            // загружаем изображения в каталоги записи
            if (!receivedData.isAttach()) {
                // запускаем активность просмотра записи с командой вставки изображений после загрузки
                openRecordWithImages(record.getId(), imagesUri);

            } else {
                // прикрепляем изображения как файлы
                boolean hasError = false;
                UriUtils uriUtils = new UriUtils(this);
                for (Uri uri : imagesUri) {
                    if (AttachesManager.attachFile(this, uriUtils.getPath(uri), record) == null) {
                        hasError = true;
                    }
                }
                if (hasError) {
                    LogManager.log(this, R.string.log_files_attach_error, ILogger.Types.WARNING, Toast.LENGTH_LONG);
                }
                // запускаем активность записи, к которой уже будут прикреплены файлы
                openRecordWithAttachedFiles(record.getId());
                // обновляем список файлов
//                mDrawerLayout.closeDrawers();
//                mViewPagerAdapter.getMainFragment().showRecordFiles(record);
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
            LogManager.log(this, R.string.search_records_search_select_node, Toast.LENGTH_LONG);
        }
    }

    private void searchInTagRecords(String query) {
        if (mCurTag != null) {
            searchInRecords(query, mCurTag.getRecords(), MainPageFragment.MAIN_VIEW_TAG_RECORDS);
        } else {
            LogManager.log(this, R.string.search_records_select_tag, Toast.LENGTH_LONG);
        }
    }

    private void searchInRecords(String query, List<TetroidRecord> records, int viewId) {
        String log = (viewId == MainPageFragment.MAIN_VIEW_NODE_RECORDS)
                ? String.format(getString(R.string.filter_records_in_node_by_query), mCurNode.getName(), query)
                : String.format(getString(R.string.filter_records_in_tag_by_query), mCurTag.getName(), query);
        LogManager.log(this, log);
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
            LogManager.log(this, getString(R.string.log_cur_record_is_not_set), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    private void searchInFiles(String query, TetroidRecord record) {
        LogManager.log(this, String.format(getString(R.string.filter_files_by_query), record.getName(), query));
        List<TetroidFile> found = ScanManager.searchInFiles(record.getAttachedFiles(), query);
        mViewPagerAdapter.getMainFragment().showRecordFiles(found);
        if (found.isEmpty()) {
            mViewPagerAdapter.getMainFragment().setFilesEmptyViewText(
                    String.format(getString(R.string.search_files_not_found_mask), query));
        }
    }

    /**
     * Обработчик создания системного меню.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!super.onBeforeCreateOptionsMenu(menu))
            return true;
        getMenuInflater().inflate(R.menu.main, menu);
        this.mOptionsMenu = menu;

        /*this.mMenuItemGlobalSearch = menu.findItem(R.id.action_global_search);
        this.mMenuItemStorageSync = menu.findItem(R.id.action_storage_sync);
        ViewUtils.setVisibleIfNotNull(mMenuItemStorageSync, SettingsManager.isSyncStorage(this));
        this.mMenuItemStorageInfo = menu.findItem(R.id.action_storage_info);
        this.mMenuItemStorageReload = menu.findItem(R.id.action_storage_reload);
        this.mMenuItemSearchViewRecords = menu.findItem(R.id.action_search_records);*/

/*        int curViewId = (mViewPagerAdapter != null)
            ? mViewPagerAdapter.getMainFragment().getCurMainViewId() : 0;
        boolean canSearchRecords = (mViewPager != null
                && mViewPager.getCurrentItem() == MainViewPager.PAGE_MAIN
                && (curViewId == MainPageFragment.MAIN_VIEW_NODE_RECORDS
                    || curViewId == MainPageFragment.MAIN_VIEW_TAG_RECORDS));
        visibleMenuItem(menu.findItem(R.id.action_search_records), canSearchRecords);
        visibleMenuItem(menu.findItem(R.id.action_storage_sync), SettingsManager.isSyncStorage(this));*/
        // инициализиируем SearchView только 1 раз
//        if (!super.mIsGUICreated) {
            initRecordsSearchView(menu.findItem(R.id.action_search_records));
//        }
        mViewPagerAdapter.getMainFragment().onCreateOptionsMenu(menu);

        return super.onAfterCreateOptionsMenu(menu);
    }

    /**
     * Обработчик подготовки пунктов системного меню перед отображением.
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isOnCreateProcessed())
            return true;

        int curViewId = (mViewPagerAdapter != null)
                ? mViewPagerAdapter.getMainFragment().getCurMainViewId() : 0;
        boolean canSearchRecords = (mViewPager != null
                && mViewPager.getCurrentItem() == MainViewPager.PAGE_MAIN
                && (curViewId == MainPageFragment.MAIN_VIEW_NODE_RECORDS
                || curViewId == MainPageFragment.MAIN_VIEW_TAG_RECORDS));
        visibleMenuItem(menu.findItem(R.id.action_search_records), canSearchRecords);
        visibleMenuItem(menu.findItem(R.id.action_storage_sync), SettingsManager.isSyncStorage(this));

        boolean isStorageLoaded = StorageManager.isLoaded();
        enableMenuItem(menu.findItem(R.id.action_search_records), isStorageLoaded);
        enableMenuItem(menu.findItem(R.id.action_global_search), isStorageLoaded);
        enableMenuItem(menu.findItem(R.id.action_storage_sync), isStorageLoaded);
        enableMenuItem(menu.findItem(R.id.action_storage_info), isStorageLoaded);
        enableMenuItem(menu.findItem(R.id.action_storage_reload), isStorageLoaded);

        mViewPagerAdapter.getMainFragment().onPrepareOptionsMenu(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void updateOptionsMenu() {
        onPrepareOptionsMenu(mOptionsMenu);
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
                showRecordNode(mViewPagerAdapter.getMainFragment().getCurRecord());
                return true;
            case R.id.action_global_search:
                showGlobalSearchActivity(null);
                return true;
            case R.id.action_storage_sync:
                StorageManager.startStorageSync(this, DataManager.getStoragePath());
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

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        if (!onBeforeBackPressed()) {
            return;
        }
        boolean needToExit = true;
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            int curPage = mViewPager.getCurrentItem();
            if (curPage == MainViewPager.PAGE_MAIN || curPage == MainViewPager.PAGE_FOUND) {
                if (curPage == MainViewPager.PAGE_MAIN && !mViewPagerAdapter.getMainFragment().onBackPressed()
                        || curPage == MainViewPager.PAGE_FOUND && !mViewPagerAdapter.getFoundFragment().onBackPressed()) {
                    if (SettingsManager.isConfirmAppExit(this)) {
                        askForExit();
                        needToExit = false;
                    }
                }
            }
            if (needToExit) {
                onBeforeExit();
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onPause() {
        // устанавливаем признак необходимости запроса PIN-кода
        StorageManager.setIsPINNeedToEnter();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private void askForExit() {
        AskDialogs.showExitDialog(this, () -> {
            onBeforeExit();
            finish();
        });
    }

    private void onBeforeExit() {
        LogManager.log(this, R.string.log_app_exit, ILogger.Types.INFO);
        // сохраняем выбранную ветку
        saveLastSelectedNode();
        // останавливаем отслеживание изменения структуры хранилища
//        TetroidFileObserver.stopObserver();
        FileObserverService.sendCommand(this, FileObserverService.ACTION_STOP);
        FileObserverService.stop(this);

        // очищаем память
        DataManager.destruct();
    }

    /**
     * Сохранение последней выбранной ветки.
     */
    private void saveLastSelectedNode() {
        if (SettingsManager.isKeepLastNode(this)) {
            TetroidNode curNode =
                    (mViewPagerAdapter.getMainFragment().getCurMainViewId() == MainPageFragment.MAIN_VIEW_FAVORITES)
                            ? FavoritesManager.FAVORITES_NODE : mCurNode;
            String nodeId = (curNode != null) ? curNode.getId() : null;
            SettingsManager.setLastNodeId(this, nodeId);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public boolean taskPreExecute(int sRes) {
        super.taskPreExecute(sRes);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        boolean isDrawerOpened = mDrawerLayout.isDrawerOpen(Gravity.LEFT);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
//        mTextViewProgress.setText(sRes);
//        mLayoutProgress.setVisibility(View.VISIBLE);
        return isDrawerOpened;
    }

    public void taskPostExecute(boolean isDrawerOpened) {
        super.taskPostExecute(isDrawerOpened);
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (isDrawerOpened) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
//        mLayoutProgress.setVisibility(View.INVISIBLE);
    }

    /**
     * Задание, в котором выполняется зашифровка/сброс шифровки веток.
     */
    public class CryptNodeTask extends TetroidTask2<Void,String,Integer> {

        boolean mIsDrawerOpened;
        TetroidNode mNode;
        boolean mIsEncrypt;
        boolean mWasCrypted;
        TetroidLog.Opers mOper;

        CryptNodeTask(TetroidNode node, boolean isEncrypt) {
            super(MainActivity.this, MainActivity.this);
            this.mNode = node;
            this.mIsEncrypt = isEncrypt;
            this.mWasCrypted = node.isCrypted();
            this.mOper = (isEncrypt) ? TetroidLog.Opers.ENCRYPT : TetroidLog.Opers.DROPCRYPT;
        }

        @Override
        protected void onPreExecute() {
            this.mIsDrawerOpened = taskPreExecute(
                    (mIsEncrypt) ? R.string.task_node_encrypting : R.string.task_node_drop_crypting);
            TetroidLog.logOperStart(mContext, TetroidLog.Objs.NODE,
                    (mIsEncrypt) ? TetroidLog.Opers.ENCRYPT : TetroidLog.Opers.DROPCRYPT, mNode);
        }

        @Override
        protected Integer doInBackground(Void... values) {
            // сначала расшифровываем хранилище
            if (DataManager.isCrypted(mContext)) {
                setStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, TaskStage.Stages.START);
                if (DataManager.getInstance().decryptStorage(mContext, false)) {
                    setStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, TaskStage.Stages.SUCCESS);
                } else {
                    setStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT, TaskStage.Stages.FAILED);
                    return -2;
                }
            }
            // только если хранилище расшифровано
            if (DataManager.isDecrypted()) {
                setStage(TetroidLog.Objs.NODE, mOper, TaskStage.Stages.START);
                return (((mIsEncrypt) ? DataManager.getInstance().encryptNode(mContext, mNode)
                        : DataManager.getInstance().dropCryptNode(mContext, mNode))) ? 1 : -1;
            }
            return 0;
        }

        private void setStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage) {
            TaskStage taskStage = new TaskStage(CryptNodeTask.class, obj, oper, stage);
            String mes = TetroidLog.logTaskStage(mContext, taskStage);
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
                TetroidLog.logOperRes(mContext, TetroidLog.Objs.NODE, mOper);
                if (!mIsEncrypt && mWasCrypted) {
                    // проверяем существование зашифрованных веток
                    checkExistenceCryptedNodes();
                }
            } else {
                TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.NODE, mOper);
            }
            afterStorageDecrypted(mNode);
        }
    }

    /**
     * Задание, в котором выполняется прикрепление нового файла в записи.
     */
    public class AttachFileTask extends TetroidTask2<String,Void,TetroidFile> {

        TetroidRecord mRecord;

        AttachFileTask(TetroidRecord record) {
            super(MainActivity.this, MainActivity.this);
            this.mRecord = record;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.task_attach_file);
        }

        @Override
        protected TetroidFile doInBackground(String... values) {
            String fileFullName = values[0];
            return AttachesManager.attachFile(mContext, fileFullName, mRecord);
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
    public class SaveFileTask extends TetroidTask2<String,Void,Boolean> {

        TetroidFile mFile;

        SaveFileTask(TetroidFile file) {
            super(MainActivity.this, MainActivity.this);
            this.mFile = file;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.task_file_saving);
        }

        @Override
        protected Boolean doInBackground(String... values) {
            String path = values[0];
            return AttachesManager.saveFile(mContext, mFile, path);
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
    private class GlobalSearchTask extends TetroidTask2<Void, Void,HashMap<ITetroidObject,FoundType>> {

        private ScanManager mScan;

        GlobalSearchTask(ScanManager scan) {
            super(MainActivity.this, MainActivity.this);
            this.mScan = scan;
        }

        @Override
        protected void onPreExecute() {
            taskPreExecute(R.string.global_searching);
            LogManager.log(mContext, String.format(getString(R.string.global_search_start), mScan.getQuery()));
        }

        @Override
        protected HashMap<ITetroidObject, FoundType> doInBackground(Void... values) {
            return mScan.globalSearch(mContext, mCurNode);
        }

        @Override
        protected void onPostExecute(HashMap<ITetroidObject,FoundType> found) {
            taskPostExecute(false);
            if (found == null) {
                LogManager.log(mContext, getString(R.string.log_global_search_return_null), Toast.LENGTH_SHORT);
                return;
            } else if (mScan.isSearchInNode() && mScan.getNode() != null) {
                LogManager.log(mContext, String.format(getString(R.string.global_search_by_node_result),
                        mScan.getNode().getName()), Toast.LENGTH_SHORT);
            }
            // уведомляем, если не смогли поискать в зашифрованных ветках
            if (mScan.isExistCryptedNodes()) {
                LogManager.log(mContext, R.string.log_found_crypted_nodes, Toast.LENGTH_SHORT);
            }
            LogManager.log(mContext, String.format(getString(R.string.global_search_end), found.size()));
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

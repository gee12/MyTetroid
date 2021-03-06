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
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import com.gee12.mytetroid.SortHelper;
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
import com.gee12.mytetroid.data.TagsManager;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.dialogs.AskDialogs;
import com.gee12.mytetroid.dialogs.NodeDialogs;
import com.gee12.mytetroid.dialogs.TagDialogs;
import com.gee12.mytetroid.fragments.FoundPageFragment;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.fragments.settings.SettingsFragment;
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
import com.gee12.mytetroid.helpers.UriHelper;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.dialogs.IntentDialog;
import com.gee12.mytetroid.views.MainViewPager;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.SearchViewXListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    public static final int REQUEST_CODE_ICON = 9;

    public static final String ACTION_MAIN_ACTIVITY = "ACTION_MAIN_ACTIVITY";
    public static final String EXTRA_CUR_NODE_ID = "EXTRA_CUR_NODE_ID";
    public static final String EXTRA_QUERY = "EXTRA_QUERY";
    public static final String EXTRA_SHOW_STORAGE_INFO = "EXTRA_SHOW_STORAGE_INFO";

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
    private MainPagerAdapter mViewPagerAdapter;
    private MainViewPager mViewPager;
    private PagerTabStrip mTitleStrip;
    private View mFavoritesNode;
    private Button mButtonLoadStorageNodes;
    private Button mButtonLoadStorageTags;
    private FloatingActionButton mFabCreateNode;

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
        mDrawerLayout.openDrawer(Gravity.LEFT);

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
        View nodesFooter =  ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        mListViewNodes.getListView().addFooterView(nodesFooter, null, false);
//        registerForContextMenu(mListViewNodes.getListView());
        this.mTextViewNodesEmpty = findViewById(R.id.nodes_text_view_empty);
        this.mFabCreateNode = findViewById(R.id.button_add_node);
        mFabCreateNode.setOnClickListener(v -> createNode());
        mFabCreateNode.hide();

        NavigationView nodesNavView = mDrawerLayout.findViewById(R.id.nav_view_left);
        View vNodesHeader = nodesNavView.getHeaderView(0);
        this.mSearchViewNodes = vNodesHeader.findViewById(R.id.search_view_nodes);
        mSearchViewNodes.setVisibility(View.GONE);
        initNodesSearchView(mSearchViewNodes, vNodesHeader);

        // метки
        this.mListViewTags = findViewById(R.id.tags_list_view);
        mListViewTags.setOnItemClickListener(onTagClicklistener);
        mListViewTags.setOnItemLongClickListener(onTagLongClicklistener);
        this.mTextViewTagsEmpty = findViewById(R.id.tags_text_view_empty);
        mListViewTags.setEmptyView(mTextViewTagsEmpty);
        View tagsFooter =  ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        mListViewTags.addFooterView(tagsFooter, null, false);

        NavigationView tagsNavView = mDrawerLayout.findViewById(R.id.nav_view_right);
        View vTagsHeader = tagsNavView.getHeaderView(0);
        this.mSearchViewTags = vTagsHeader.findViewById(R.id.search_view_tags);
        mSearchViewTags.setVisibility(View.GONE);
        initTagsSearchView(mSearchViewTags, vTagsHeader);
        vTagsHeader.findViewById(R.id.button_tags_sort).setOnClickListener(v -> showTagsSortPopupMenu(v));

        // избранное
        this.mFavoritesNode = findViewById(R.id.node_favorites);
        this.mButtonLoadStorageNodes = findViewById(R.id.button_load);
        this.mButtonLoadStorageTags = findViewById(R.id.button_load_2);
        mFavoritesNode.setVisibility(View.GONE);
        mButtonLoadStorageNodes.setVisibility(View.GONE);
        mButtonLoadStorageTags.setVisibility(View.GONE);
        if (App.isFullVersion()) {
            mFavoritesNode.setOnClickListener(v -> showFavorites());
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StorageManager.loadAllNodes(MainActivity.this, false);
                }
            };
            mButtonLoadStorageNodes.setOnClickListener(listener);
            mButtonLoadStorageTags.setOnClickListener(listener);
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


    // region UI

    /**
     * Первоначальная инициализация списков веток, записей, файлов, меток
     *
     * @param isLoaded   Результат загрузки хранилища.
     * @param isOnlyFavorites
     * @param isOpenLastNode  Нужно ли загружать ветку, сохраненную в опции getLastNodeId(),
     *                        или ветку с избранными записями
     */
    @Override
    public void initGUI(boolean isLoaded, boolean isOnlyFavorites, boolean isOpenLastNode) {
        // избранные записи
        int loadButtonsVis = (isLoaded && isOnlyFavorites) ? View.VISIBLE : View.GONE;
        mButtonLoadStorageNodes.setVisibility(loadButtonsVis);
        mButtonLoadStorageTags.setVisibility(loadButtonsVis);
        ViewUtils.setFabVisibility(mFabCreateNode, isLoaded && !isOnlyFavorites);
        mListViewNodes.setVisibility((!isOnlyFavorites) ? View.VISIBLE : View.GONE);
        mFavoritesNode.setVisibility((isLoaded && App.isFullVersion()) ? View.VISIBLE : View.GONE);
        mTextViewNodesEmpty.setVisibility(View.GONE);
        if (isLoaded && App.isFullVersion()) {
            updateFavorites();
        }
        // элементы фильтра веток и меток
        ViewUtils.setVisibleIfNotNull(mSearchViewNodes, isLoaded && !isOnlyFavorites);
        ViewUtils.setVisibleIfNotNull(mSearchViewTags, isLoaded && !isOnlyFavorites);
        ViewUtils.setVisibleIfNotNull((View)findViewById(R.id.button_tags_sort), isLoaded && !isOnlyFavorites);

        if (isOnlyFavorites) {
            // обработка только "ветки" избранных записей
            if (isLoaded) {
                // списки записей, файлов
                getMainPage().initListAdapters(this);
                showFavorites();
                // список меток
                mTextViewTagsEmpty.setText(R.string.title_load_all_nodes);
                setListEmptyViewState(mTextViewNodesEmpty, true, R.string.title_load_all_nodes);
            } else {
                setEmptyTextViews(R.string.title_storage_not_loaded);
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
            isLoaded = (isLoaded && rootNodes != null);
            if (isLoaded) {
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
                    getMainPage().clearView();
                }
                if (!nodesAdapterInited) {
                    mListAdapterNodes.setDataItems(rootNodes);
                }

                if (!isEmpty) {
                    // списки записей, файлов
                    getMainPage().initListAdapters(this);
                    if (nodeToSelect != null) {
                        if (nodeToSelect == FavoritesManager.FAVORITES_NODE) {
                            showFavorites();
                        } else {
                            showNode(nodeToSelect);
                        }
                    }

                    // список меток
                    setTagsDataItems(DataManager.getTags());
                    mTextViewTagsEmpty.setText(R.string.log_tags_is_missing);
                }
                setListEmptyViewState(mTextViewNodesEmpty, isEmpty, R.string.title_nodes_is_missing);
            } else {
                setEmptyTextViews(R.string.title_storage_not_loaded);
            }
        }
        updateOptionsMenu();
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

    /**
     * Обработчик события, когда создались все элементы интерфейса.
     * Вызывается из onCreateOptionsMenu(), т.к. пункты меню, судя по всему, создаются в последнюю очередь.
     */
    @Override
    protected void onGUICreated() {
        // инициализация
        App.init(this);
        getMainPage().onSettingsInited();

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

    private void setEmptyTextViews(@StringRes int mesId) {
        getMainPage().setRecordsEmptyViewText(getString(mesId));
        setListEmptyViewState(mTextViewNodesEmpty, true, mesId);
        setListEmptyViewState(mTextViewTagsEmpty, true, mesId);
        mDrawerLayout.closeDrawers();
    }

    private void setListEmptyViewState(TextView tvEmpty, boolean isVisible, @StringRes int stringId) {
        setListEmptyViewState(tvEmpty, isVisible, getString(stringId));
    }

    private void setListEmptyViewState(TextView tvEmpty, boolean isVisible, String string) {
        tvEmpty.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
        tvEmpty.setText(string);
    }

    /**
     * Изменение ToolBar по текущей странице.
     * @param curPage
     */
    private void changeToolBarByPage(int curPage) {
        if (curPage == MainViewPager.PAGE_MAIN) {
            getMainPage().restoreLastMainToolbarState();
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

    private MainPageFragment getMainPage() {
        return mViewPagerAdapter.getMainFragment();
    }

    private FoundPageFragment getFoundPage() {
        return mViewPagerAdapter.getFoundFragment();
    }

    // endregion UI


    // region LoadStorage

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
//                bundle.putString(FileObserverService.EXTRA_FILE_PATH, StorageManager.getStoragePath() + "/" + DataManager.MYTETRA_XML_FILE_NAME);
                bundle.putString(FileObserverService.EXTRA_FILE_PATH, DataManager.getPathToMyTetraXml());
                bundle.putInt(FileObserverService.EXTRA_EVENT_MASK, FileObserver.MODIFY);
                FileObserverService.sendCommand(this, bundle);
                LogManager.log(this, getString(R.string.log_mytetra_xml_observer_mask,
                        getString(R.string.launched)), ILogger.Types.INFO);
            }
        } else {
            FileObserverService.sendCommand(this, FileObserverService.ACTION_STOP);
            FileObserverService.stop(this);
            LogManager.log(this, getString(R.string.log_mytetra_xml_observer_mask,
                    getString(R.string.stopped)), ILogger.Types.INFO);
        }
    }

    /**
     * Перезагрузка хранилища (при изменении пути в настройках).
     */
    private void reinitStorage() {
        closeFoundFragment();
        getMainPage().clearView();
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
            getMainPage().clearView();
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
        updateNodeList();
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
     * Если включен режим только избранных записей, то сначала нужно загрузить все ветки.
     * @param data
     * @return
     */
    private boolean checkIsNeedLoadAllNodes(Intent data) {
        if (StorageManager.isFavoritesMode()) {
            mReceivedIntent = data;
            StorageManager.loadAllNodes(MainActivity.this, true);
            return true;
        }
        return false;
    }

    // endregion LoadStorage


    // region Nodes

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

        // сохраняем выбранную ветку
        saveLastSelectedNode();
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
        getMainPage().setCurNode(node);
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
     * Обновление текущей ветки, если еще не установлена.
     * @param record
     */
    private void checkCurNode(TetroidRecord record) {
        TetroidNode node = record.getNode();
        if (node != mCurNode) {
            setCurNode(node);
        }
    }

    /**
     * Обновление списка веток.
     */
    @Override
    public void updateNodeList() {
        if (mListAdapterNodes != null) {
            mListAdapterNodes.notifyDataSetChanged();
        }
    }

    /**
     * Сохранение последней выбранной ветки.
     */
    private void saveLastSelectedNode() {
        if (SettingsManager.isKeepLastNode(this)) {
            TetroidNode curNode =
                    (getMainPage().getCurMainViewId() == MainPageFragment.MAIN_VIEW_FAVORITES)
                            ? FavoritesManager.FAVORITES_NODE : mCurNode;
            String nodeId = (curNode != null) ? curNode.getId() : null;
            SettingsManager.setLastNodeId(this, nodeId);
        }
    }

    /**
     * Обработчик клика на заголовке ветки с подветками.
     */
    NodesListAdapter.OnNodeHeaderClickListener onNodeHeaderClickListener = new NodesListAdapter.OnNodeHeaderClickListener() {
        @Override
        public void onClick(TetroidNode node, int pos) {
            if (node.isExpandable() && SettingsManager.isExpandEmptyNode(MainActivity.this)) {
                // если у ветки есть подветки и установлена опция
                if (node.getRecordsCount() > 0) {
                    // и в ветке есть записи - открываем список записей
                    showNode(node);
                } else {
                    // иначе - разворачиваем/сворачиваем ветку
                    mListAdapterNodes.toggleNodeExpand(pos);
                }
            } else {
                // сразу открываем список записей (даже если он пуст)
                showNode(node);
            }
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

    // endregion Nodes


    // region Node

    /**
     * Создание ветки.
     * @param parentNode Родительская ветка
     * @param pos        Позиция в списке родительской ветки
     * @param isSubNode  Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    private void createNode(TetroidNode parentNode, int pos, boolean isSubNode) {
        NodeDialogs.createNodeDialog(this, null, false, (name, parNode) -> {
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
     * Создание ветки.
     */
    private void createNode() {
        NodeDialogs.createNodeDialog(this, null, true, (name, parNode) -> {
            TetroidNode node = NodesManager.createNode(this, name, parNode);
            if (node != null) {
                if (mListAdapterNodes.addItem(parNode)) {
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
     * Переименование ветки.
     * @param node
     */
    private void renameNode(TetroidNode node) {
        NodeDialogs.createNodeDialog(this, node, false, (name, parNode) -> {
            if (NodesManager.editNodeFields(this, node, name)) {
                TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
                updateNodeList();
                if (mCurNode == node) {
                    setTitle(name);
                }
            } else {
                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.RENAME);
            }
        });
    }

    private void setNodeIcon(TetroidNode node) {
        IconsActivity.startIconsActivity(this, node, REQUEST_CODE_ICON);
    }

    private void setNodeIcon(String nodeId, String iconPath, boolean isDrop) {
        TetroidNode node = (mCurNode != null && mCurNode.getId() == nodeId) ? mCurNode
                : NodesManager.getNode(nodeId);
        if (NodesManager.setNodeIcon(this, node, iconPath, isDrop)) {
            TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CHANGE);
            updateNodeList();
        } else {
            TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CHANGE);
        }
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

        NodeDialogs.deleteNode(this, node.getName(), () -> {
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
            // обновляем label с количеством избранных записей
            if (App.isFullVersion()) {
                updateFavorites();
            }
            // убираем список записей удаляемой ветки
            if (mCurNode == node || isNodeInNode(mCurNode, node)) {
                getMainPage().clearView();
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
            int res = DataManager.swapTetroidObjects(this, subNodes, posInNode, isUp, true);
            if (res > 0) {
                // меняем местами элементы внутри списка
                int newPosInNode = (isUp) ?
                        (posInNode == 0) ? subNodes.size() - 1 : posInNode - 1
                        : (posInNode == subNodes.size() - 1) ? 0 : posInNode + 1;
//                if (mListAdapterNodes.swapItems(pos, posInNode, (isUp) ? posInNode - 1 : posInNode + 1)) {
                if (mListAdapterNodes.swapItems(pos, posInNode, newPosInNode)) {
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

    // endregion Node


    // region Favorites

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

            // сохраняем выбранную ветку
            saveLastSelectedNode();
        }
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

    // endregion Favorites


    // region Tags

    private void setTagsDataItems(Map<String,TetroidTag> tags) {
        mListAdapterTags.setDataItems(tags, new SortHelper(
                SettingsManager.getTagsSortMode(this, SortHelper.byNameAsc())));
    }

    /**
     * Обновление списка меток.
     */
    @Override
    public void updateTags() {
        setTagsDataItems(DataManager.getTags());
    }

    // endregion Tags


    // region Tag

    /**
     * Отображение записей по метке.
     * @param position Индекс метки в списке
     */
    private void showTagRecords(int position) {
        TetroidTag tag = mListAdapterTags.getItem(position).getValue();
        showTag(tag);
    }

    /**
     * Отображение записей по метке.
     * @param tag
     */
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
     * Переименование метки в записях.
     * @param tag
     */
    private void renameTag(TetroidTag tag) {
        if (tag == null) {
            LogManager.log(this, R.string.log_tag_is_null, ILogger.Types.ERROR, Toast.LENGTH_LONG);
            return;
        }
        TagDialogs.createTagDialog(this, tag, name -> {
            if (tag.getName().equals(name))
                return;
            if (TagsManager.renameTag(MainActivity.this, tag, name)) {
                TetroidLog.logOperRes(MainActivity.this, TetroidLog.Objs.TAG, TetroidLog.Opers.RENAME);
                updateTags();
                getMainPage().updateRecordList();
            } else {
                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.TAG, TetroidLog.Opers.RENAME);
            }
        });
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

    // endregion Tag


    // region Records

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
        getMainPage().showRecords(records, viewId);
    }

    // endregion Records


    // region Record

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

    @Override
    public void openRecordFolder(TetroidRecord record) {
        if (!RecordsManager.openRecordFolder(this, record)) {
            LogManager.log(this, R.string.log_missing_file_manager, Toast.LENGTH_LONG);
        }
    }

    // endregion Record


    // region Attaches

    @Override
    public void openRecordAttaches(TetroidRecord record) {
        checkCurNode(record);
        getMainPage().showRecordAttaches(record, false);
        mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
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

    private void moveBackFromAttaches() {
        if (getMainPage().isFromRecordActivity()) {
            openRecord(getMainPage().getCurRecord());
            getMainPage().dropIsFromRecordActivity();
        } else {
            showRecordNode(getMainPage().getCurRecord());
        }
    }

    public void attachFile(String fullFileName, boolean deleteSrcFile) {
        this.mCurTask = new MainAttachFileTask(getMainPage().getCurRecord(), deleteSrcFile).run(fullFileName);
    }

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

    // endregion Attaches


    // region ContextMenus

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
        int nodesCount = ((parentNode != null) ? parentNode.getSubNodes() : DataManager.getRootNodes()).size();
//        visibleMenuItem(menu.findItem(R.id.action_move_up), pos > 0);
//        visibleMenuItem(menu.findItem(R.id.action_move_down), pos < nodesCount - 1);
        visibleMenuItem(menu.findItem(R.id.action_move_up), nodesCount > 0);
        visibleMenuItem(menu.findItem(R.id.action_move_down), nodesCount > 0);
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
                    setNodeIcon(node);
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
        setForceShowMenuIcons(v, (MenuBuilder) menu);
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

        Menu menu = popupMenu.getMenu();
        visibleMenuItem(menu.findItem(R.id.action_rename), App.isFullVersion());

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_open_tag:
                    showTag(tag);
                    return true;
                case R.id.action_rename:
                    renameTag(tag);
                    return true;
                case R.id.action_copy_link:
                    copyTagLink(tag);
                    return true;
                default:
                    return false;
            }
        });
        setForceShowMenuIcons(v, (MenuBuilder) popupMenu.getMenu());
    }

    private void showTagsSortPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.tags_sort);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_sort_tags_name_asc:
                    mListAdapterTags.sort(true, true);
                    SettingsManager.setTagsSortMode(this, SortHelper.byNameAsc());
                    return true;
                case R.id.action_sort_tags_name_desc:
                    mListAdapterTags.sort(true, false);
                    SettingsManager.setTagsSortMode(this, SortHelper.byNameDesc());
                    return true;
                case R.id.action_sort_tags_count_asc:
                    mListAdapterTags.sort(false, true);
                    SettingsManager.setTagsSortMode(this, SortHelper.byCountAsc());
                    return true;
                case R.id.action_sort_tags_count_desc:
                    mListAdapterTags.sort(false, false);
                    SettingsManager.setTagsSortMode(this, SortHelper.byCountDesc());
                    return true;
                default:
                    return false;
            }
        });
        setForceShowMenuIcons(v, (MenuBuilder) popupMenu.getMenu());
    }

    /**
     * Принудительное отображение иконок у пунктов меню.
     * @param v
     * @param menu
     */
    @SuppressLint("RestrictedApi")
    private void setForceShowMenuIcons(View v, MenuBuilder menu) {
        // для отображения иконок
        MenuPopupHelper menuHelper = new MenuPopupHelper(this, menu, v);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    // endregion ContextMenus


    // region FileFolderPicker

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

    // endregion FileFolderPicker


    // region OnActivityResult

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
            attachFile(fileFullName, false);
        } else if (requestCode == REQUEST_CODE_FOLDER_PICKER && resultCode == RESULT_OK) {
            String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
            // сохраняем путь
            SettingsManager.setLastChoosedFolder(this, folderPath);
            this.mCurTask = new SaveFileTask(getMainPage().getCurFile()).run(folderPath);
        } else if (requestCode == REQUEST_CODE_ICON && resultCode == RESULT_OK) {
            String nodeId = data.getStringExtra(IconsActivity.EXTRA_NODE_ID);
            String iconPath = data.getStringExtra(IconsActivity.EXTRA_NODE_ICON_PATH);
            boolean isDrop = data.getBooleanExtra(IconsActivity.EXTRA_IS_DROP, false);
            setNodeIcon(nodeId, iconPath, isDrop);
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
        // обновляем списки, могли измениться настройки отображения
        getMainPage().updateRecordList();
        updateNodeList();

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
                StorageManager.loadAllNodes(this, false);
            } else if (data.getBooleanExtra(SettingsFragment.EXTRA_IS_PASS_CHANGED, false)) {
                // обновляем списки, т.к. хранилище должно было расшифроваться
                updateNodeList();
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
            // обычное закрытие активности

            // проверяем пора ли запустить диалог оценки приложения
            checkForInAppReviewShowing();

            return;
        }
        // проверяем нужно ли отслеживать структуру хранилища
//        TetroidFileObserver.startOrStopObserver(SettingsManager.isCheckOutsideChanging(), mOutsideChangingHandler);
        /*Bundle bundle = new Bundle();
        bundle.putInt(FileObserverService.EXTRA_ACTION_ID, FileObserverService.ACTION_START_OR_STOP);
        bundle.putBoolean(FileObserverService.EXTRA_IS_START, SettingsManager.isCheckOutsideChanging());
        FileObserverService.sendCommand(this, bundle);*/
        startStorageTreeObserver();

        if (data.getBooleanExtra(RecordActivity.EXTRA_IS_FIELDS_EDITED, false)) {
            // обновляем списки, если редактировали свойства записи
            getMainPage().onRecordFieldsUpdated(null, false);
        } else {
            // обновляем список записей, чтобы обновить дату изменения
            if (App.RecordFieldsInList.checkIsEditedDate()) {
                getMainPage().updateRecordList();
            }
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
                    updateNodeList();
                    updateTags();
                }
                break;
            case RecordActivity.RESULT_OPEN_RECORD:
                if (checkIsNeedLoadAllNodes(data)) return;

                String recordId = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                if (recordId != null) {
                    openRecord(recordId);
                }
                break;
            case RecordActivity.RESULT_OPEN_NODE:
                if (checkIsNeedLoadAllNodes(data)) return;

                String nodeId = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                if (nodeId != null) {
                    showNode(nodeId);
                }
                break;
            case RecordActivity.RESULT_SHOW_ATTACHES:
                String recordId2 = data.getStringExtra(RecordActivity.EXTRA_OBJECT_ID);
                TetroidRecord record = RecordsManager.getRecord(recordId2);
                if (record != null) {
                    getMainPage().showRecordAttaches(record, true);
                }
                break;
            case RecordActivity.RESULT_SHOW_TAG:
                if (checkIsNeedLoadAllNodes(data)) return;

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
                    getMainPage().deleteRecordExactly(record2);
                }
                break;
        }
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

    // endregion OnActivityResult


    // region GlobalSearch

    public void setFoundPageVisibility(boolean isVisible) {
        if (!isVisible) {
            mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
        }
        mViewPager.setPagingEnabled(isVisible);
        mTitleStrip.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
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
                openRecordAttaches(((TetroidFile) found).getRecord());
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

    // endregion GlobalSearch


    // region OnNewIntent

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

        } else if (action.equals(ACTION_MAIN_ACTIVITY)) {
            if (intent.hasExtra(EXTRA_SHOW_STORAGE_INFO)) {
                showStorageInfoActivity();
            }

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
                        StorageManager.loadAllNodes(this, false);
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
                UriHelper uriHelper = new UriHelper(this);
                for (Uri uri : imagesUri) {
                    if (AttachesManager.attachFile(this, uriHelper.getPath(uri), record) == null) {
                        hasError = true;
                    }
                }
                if (hasError) {
                    LogManager.log(this, R.string.log_files_attach_error, ILogger.Types.WARNING, Toast.LENGTH_LONG);
                    showSnackMoreInLogs();
                }
                // запускаем активность записи, к которой уже будут прикреплены файлы
                openRecordWithAttachedFiles(record.getId());
                // обновляем список файлов
//                mDrawerLayout.closeDrawers();
//                mViewPagerAdapter.getMainFragment().showRecordFiles(record);
            }
        }
    }

    // endregion OnNewIntent


    // region Search

    /**
     * Фильтр меток по названию.
     * @param query
     * @param isSearch Если false, то происходит сброс фильтра.
     */
    private void searchInTags(String query, boolean isSearch) {
        if (!DataManager.isLoaded()) {
            onGUICreated();
            return;
        }
        Map<String, TetroidTag> tags;
        if (isSearch) {
            tags = ScanManager.searchInTags(DataManager.getTags(), query);
        } else {
            tags = DataManager.getTags();
        }
        setTagsDataItems(tags);
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
                searchInMainPage(query, true);
            }

            @Override
            public void onQueryChange(String query) {
                searchInMainPage(query, false);
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
                    switch (getMainPage().getCurMainViewId()) {
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
     * Настройка элемента для фильтра веток.
     * @param nodesHeader
     */
    private void initNodesSearchView(final SearchView searchView, View nodesHeader) {
        final TextView tvHeader = nodesHeader.findViewById(R.id.text_view_nodes_header);
        final ImageView ivIcon = nodesHeader.findViewById(R.id.image_view_app_icon);

        new SearchViewXListener(searchView) {
            @Override
            public void onClose() {
                // ничего не делать, если хранилище не было загружено
                if (mListAdapterNodes == null)
                    return;
                searchInNodesNames(null);
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
            public void onQueryChange(String query) {
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
            public void onQueryChange(String query) {
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
        if (TextUtils.isEmpty(query)) {
            if (mListAdapterNodes != null) {
                // просто выводим все ветки
                mListAdapterNodes.setDataItems(DataManager.getRootNodes());
            }
            setListEmptyViewState(mTextViewNodesEmpty, false, "");
            return;
        }
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

    /**
     * Поиск по записям, меткам или файлам (смотря какой список активен в данный момент).
     * @param query
     */
    private void searchInMainPage(String query, boolean isSaveQuery) {
        if (isSaveQuery) {
            TetroidSuggestionProvider.saveRecentQuery(this, query);
        }
        searchInMainPage(query, getMainPage().getCurMainViewId());
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
            getMainPage().setRecordsEmptyViewText(emptyText);
            this.mLastSearchQuery = query;
            getMainPage().showGlobalSearchButton(true);
        } else {
            getMainPage().showGlobalSearchButton(false);
        }
    }

    private void searchInRecordFiles(String query) {
        TetroidRecord curRecord = getMainPage().getCurRecord();
        if (curRecord != null) {
            searchInFiles(query, curRecord);
        } else {
            LogManager.log(this, getString(R.string.log_cur_record_is_not_set), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    private void searchInFiles(String query, TetroidRecord record) {
        LogManager.log(this, String.format(getString(R.string.filter_files_by_query), record.getName(), query));
        List<TetroidFile> found = ScanManager.searchInFiles(record.getAttachedFiles(), query);
        getMainPage().showRecordAttaches(found);
        if (found.isEmpty()) {
            getMainPage().setFilesEmptyViewText(
                    String.format(getString(R.string.search_files_not_found_mask), query));
        }
    }

    // endregion Search


    // region InAppReview

    /**
     * Запуск механизма оценки и отзыва о приложении In-App Review.
     *
     * TODO: пока отключено.
     *
     */
    private int recordOpeningCount = 0;
    private void checkForInAppReviewShowing() {
        if (++recordOpeningCount > 2) {
            recordOpeningCount = 0;
//            TetroidReview.showInAppReview(this);
        }
    }

    // endregion InAppReview


    // region OptionsMenu

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

        initRecordsSearchView(menu.findItem(R.id.action_search_records));
        getMainPage().onCreateOptionsMenu(menu);

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
                ? getMainPage().getCurMainViewId() : 0;
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
        enableMenuItem(menu.findItem(R.id.action_storage_reload), !TextUtils.isEmpty(SettingsManager.getStoragePath(this)));

        getMainPage().onPrepareOptionsMenu(menu);

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
            case R.id.action_move_back:
                moveBackFromAttaches();
                return true;
            case R.id.action_global_search:
                showGlobalSearchActivity(null);
                return true;
            case R.id.action_storage_sync:
                StorageManager.startStorageSync(this, DataManager.getStoragePath(), null);
                return true;
            case R.id.action_storage_info:
                showStorageInfoActivity();
                return true;
            case R.id.action_storage_reload:
                reloadStorageAsk();
                return true;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, REQUEST_CODE_SETTINGS_ACTIVITY);
                return true;
            default:
                if (getMainPage().onOptionsItemSelected(id)) {
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    // endregion OptionsMenu


    // region Exit

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        if (!onBeforeBackPressed()) {
            return;
        }
        if (SettingsManager.isShowNodesInsteadExit(this)) {
            // показывать левую шторку вместо выхода
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                if (SettingsManager.isConfirmAppExit(this)) {
                    askForExit();
                } else {
                    onBeforeExit();
                    onExit();
                    super.onBackPressed();
                }
            } else if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else {
                boolean needToOpenDrawer = true;
                int curPage = mViewPager.getCurrentItem();
                if (curPage == MainViewPager.PAGE_MAIN || curPage == MainViewPager.PAGE_FOUND) {
                    if (curPage == MainViewPager.PAGE_MAIN && getMainPage().onBackPressed()
                            || curPage == MainViewPager.PAGE_FOUND && getFoundPage().onBackPressed()) {
                        needToOpenDrawer = false;
                    }
                }
                // открываем левую шторку, если все проверили
                if (needToOpenDrawer) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            }
        } else {
            boolean needToExit = true;
            // выходить, если не отображаются боковые панели
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else {
                int curPage = mViewPager.getCurrentItem();
                if (curPage == MainViewPager.PAGE_MAIN || curPage == MainViewPager.PAGE_FOUND) {
                    if (curPage == MainViewPager.PAGE_MAIN && !getMainPage().onBackPressed()
                            || curPage == MainViewPager.PAGE_FOUND && !getFoundPage().onBackPressed()) {
                        if (SettingsManager.isConfirmAppExit(this)) {
                            askForExit();
                            needToExit = false;
                        }
                    } else {
                        needToExit = false;
                    }
                }
                // выходим, если все проверили
                if (needToExit) {
                    onBeforeExit();
                    onExit();
                    super.onBackPressed();
                }
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
        });
    }

    private void onBeforeExit() {
        // синхронизация перед выходом из приложения
        StorageManager.startStorageSyncAndExit(this, () -> {
            onExit();
            finish();
        });
    }

    private void onExit() {
        LogManager.log(this, R.string.log_app_exit, ILogger.Types.INFO);

        // останавливаем отслеживание изменения структуры хранилища
        FileObserverService.sendCommand(this, FileObserverService.ACTION_STOP);
        FileObserverService.stop(this);

        // очищаем память
        DataManager.destruct();
    }

    // endregion Exit


    // region StartActivity

    private void showStorageInfoActivity() {
        if (StorageManager.isFavoritesMode()) {
            AskDialogs.showLoadAllNodesDialog(this,
                    () -> {
                        // помещаем Intent для обработки
                        Intent intent = new Intent(ACTION_MAIN_ACTIVITY);
                        intent.putExtra(EXTRA_SHOW_STORAGE_INFO, true);
                        this.mReceivedIntent = intent;
                        // загружаем все ветки
                        StorageManager.loadAllNodes(MainActivity.this, true);
                    });
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
//            intent.putExtra(EXTRA_CUR_NODE_IS_NOT_NULL, (mCurNode != null));
            intent.putExtra(EXTRA_CUR_NODE_ID, (mCurNode != null) ? mCurNode.getId() : null);
            startActivityForResult(intent, REQUEST_CODE_SEARCH_ACTIVITY);
        }
    }

    @Override
    public void showGlobalSearchWithQuery() {
        showGlobalSearchActivity(mLastSearchQuery);
    }

    // endregion StartActivity


    // region AsyncTasks

    public int taskPreExecute(int sRes) {
        super.taskPreExecute(sRes);
        int openedDrawer = (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) ? Gravity.LEFT
                : (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) ? Gravity.RIGHT : Gravity.NO_GRAVITY;
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        return openedDrawer;
    }

    public void taskPostExecute(int openedDrawer) {
        super.taskPostExecute(openedDrawer);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (openedDrawer != Gravity.NO_GRAVITY) {
            mDrawerLayout.openDrawer(openedDrawer);
        }
    }

    /**
     * Задание, в котором выполняется зашифровка/сброс шифровки веток.
     */
    public class CryptNodeTask extends TetroidTask2<Void,String,Integer> {

        int mOpenedDrawer;
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
            this.mOpenedDrawer = taskPreExecute(
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
            setProgressText(mes);
        }

        @Override
        protected void onPostExecute(Integer res) {
            taskPostExecute(mOpenedDrawer);
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
    public class MainAttachFileTask extends AttachFileTask {

        public MainAttachFileTask(TetroidRecord record, boolean deleteSrcFile) {
            super(record, deleteSrcFile);
        }

        @Override
        protected void onPostExecute(TetroidFile res) {
            taskPostExecute(Gravity.NO_GRAVITY);
            getMainPage().attachFile(res);
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
            taskPostExecute(Gravity.NO_GRAVITY);
            getMainPage().onSaveFileResult(res);
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
            return mScan.globalSearch(mContext);
        }

        @Override
        protected void onPostExecute(HashMap<ITetroidObject,FoundType> found) {
            taskPostExecute(Gravity.NO_GRAVITY);
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
            getFoundPage().setFounds(found, mScan);
            mViewPagerAdapter.notifyDataSetChanged(); // для обновления title у страницы
            setFoundPageVisibility(true);
            mViewPager.setCurrent(MainViewPager.PAGE_FOUND);
        }
    }

    // endregion AsyncTasks


    // region Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

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

    // endregion Parcelable
}

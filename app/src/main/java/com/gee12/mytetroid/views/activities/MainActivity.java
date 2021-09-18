package com.gee12.mytetroid.views.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.PermissionManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SortHelper;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.data.PINManager;
import com.gee12.mytetroid.viewmodels.AskPasswordParams;
import com.gee12.mytetroid.viewmodels.ClipboardParams;
import com.gee12.mytetroid.viewmodels.FilteredObjectsInView;
import com.gee12.mytetroid.viewmodels.GlobalSearchParams;
import com.gee12.mytetroid.viewmodels.MainViewModel;
import com.gee12.mytetroid.viewmodels.ObjectsInView;
import com.gee12.mytetroid.viewmodels.ReadDecryptStorageState;
import com.gee12.mytetroid.viewmodels.factory.StorageViewModelFactory;
import com.gee12.mytetroid.viewmodels.ToolbarParams;
import com.gee12.mytetroid.views.adapters.MainPagerAdapter;
import com.gee12.mytetroid.views.adapters.NodesListAdapter;
import com.gee12.mytetroid.views.adapters.TagsListAdapter;
import com.gee12.mytetroid.data.FavoritesManager;
import com.gee12.mytetroid.data.PassManager;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.data.SettingsManager;
//import com.gee12.mytetroid.data.StorageManager;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.views.dialogs.AskDialogs;
import com.gee12.mytetroid.views.dialogs.FileDialogs;
import com.gee12.mytetroid.views.dialogs.NodeDialogs;
import com.gee12.mytetroid.views.dialogs.PassDialogs;
import com.gee12.mytetroid.views.dialogs.RecordDialogs;
import com.gee12.mytetroid.views.dialogs.TagDialogs;
import com.gee12.mytetroid.views.fragments.FoundPageFragment;
import com.gee12.mytetroid.views.fragments.MainPageFragment;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;
import com.gee12.mytetroid.services.FileObserverService;
import com.gee12.mytetroid.utils.FileUtils;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.dialogs.IntentDialog;
import com.gee12.mytetroid.views.MainViewPager;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.SearchViewXListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

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
public class MainActivity extends TetroidActivity {

    private DrawerLayout drawerLayout;
    private MultiLevelListView lvNodes;
    private NodesListAdapter listAdapterNodes;
    private ListView lvTags;
    private TagsListAdapter listAdapterTags;
    private TextView tvNodesEmpty;
    private TextView tvTagsEmpty;
    private SearchView searchViewNodes;
    private SearchView searchViewTags;
    private SearchView searchViewRecords;
    private MainPagerAdapter viewPagerAdapter;
    private MainViewPager viewPager;
    private PagerTabStrip titleStrip;
    private View favoritesNode;
    private Button btnLoadStorageNodes;
    private Button btnLoadStorageTags;
    private FloatingActionButton fabCreateNode;

    private boolean isActivityCreated;
    private int openedDrawerState;

    private MainViewModel viewModel;

    private BroadcastReceiver broadcastReceiver;
    private LocalBroadcastManager broadcastManager;


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

        this.viewModel = new ViewModelProvider(this, new StorageViewModelFactory(getApplication()))
                .get(MainViewModel.class);

        // выдвигающиеся панели
        this.drawerLayout = findViewById(R.id.drawer_layout);
        // задаем кнопку (стрелку) управления шторкой
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        drawerLayout.openDrawer(Gravity.LEFT);

        // обработчик нажатия на экране, когда ветка не выбрана
        drawerLayout.setOnTouchListener(this);

        // страницы (главная и найдено)
        this.viewPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), gestureDetector);
        this.viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(viewPagerAdapter);
//        mViewPager.setGestureDetector(mGestureDetector);

        this.titleStrip = viewPager.findViewById(R.id.pager_title_strip);
        setFoundPageVisibility(false);
        viewPager.addOnPageChangeListener(new ViewPagerListener());

//        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
//        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
//        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        // ветки
        lvNodes = findViewById(R.id.list_view_nodes);
        lvNodes.setOnItemClickListener(onNodeClickListener);
        lvNodes.setOnItemLongClickListener(onNodeLongClickListener);
        View nodesFooter = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        lvNodes.getListView().addFooterView(nodesFooter, null, false);
//        registerForContextMenu(mListViewNodes.getListView());
        this.tvNodesEmpty = findViewById(R.id.nodes_text_view_empty);
        this.fabCreateNode = findViewById(R.id.button_add_node);
        fabCreateNode.setOnClickListener(v -> createNode());
        fabCreateNode.hide();

        NavigationView nodesNavView = drawerLayout.findViewById(R.id.nav_view_left);
        View vNodesHeader = nodesNavView.getHeaderView(0);
        this.searchViewNodes = vNodesHeader.findViewById(R.id.search_view_nodes);
        searchViewNodes.setVisibility(View.GONE);
        initNodesSearchView(searchViewNodes, vNodesHeader);

        // метки
        this.lvTags = findViewById(R.id.tags_list_view);
        lvTags.setOnItemClickListener(onTagClicklistener);
        lvTags.setOnItemLongClickListener(onTagLongClicklistener);
        this.tvTagsEmpty = findViewById(R.id.tags_text_view_empty);
        lvTags.setEmptyView(tvTagsEmpty);
        View tagsFooter = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        lvTags.addFooterView(tagsFooter, null, false);

        NavigationView tagsNavView = drawerLayout.findViewById(R.id.nav_view_right);
        View vTagsHeader = tagsNavView.getHeaderView(0);
        this.searchViewTags = vTagsHeader.findViewById(R.id.search_view_tags);
        searchViewTags.setVisibility(View.GONE);
        initTagsSearchView(searchViewTags, vTagsHeader);
        vTagsHeader.findViewById(R.id.button_tags_sort).setOnClickListener(v -> showTagsSortPopupMenu(v));

        // избранное
        this.favoritesNode = findViewById(R.id.node_favorites);
        this.btnLoadStorageNodes = findViewById(R.id.button_load);
        this.btnLoadStorageTags = findViewById(R.id.button_load_2);
        favoritesNode.setVisibility(View.GONE);
        btnLoadStorageNodes.setVisibility(View.GONE);
        btnLoadStorageTags.setVisibility(View.GONE);
        if (App.isFullVersion()) {
            favoritesNode.setOnClickListener(v -> viewModel.showFavorites());
            View.OnClickListener listener = v -> viewModel.loadAllNodes(false);
            btnLoadStorageNodes.setOnClickListener(listener);
            btnLoadStorageTags.setOnClickListener(listener);
        }

        initBroadcastReceiver();

        viewModel.getViewEvent().observe(this, it -> onActivityEvent(it.getState(), it.getData()));
        viewModel.getStorageEvent().observe(this, it -> onStorageStateChanged(it.getState(), it.getData()));
        viewModel.getObjectAction().observe(this, it -> onObjectActions(it.getState(), it.getData()));
    }

    /**
     *
     * @param event
     * @param data
     */
    private void onActivityEvent(Constants.ViewEvents event, Object data) {
        switch (event) {
            // activity
            case InitGUI:
                ReadDecryptStorageState storageParams = (ReadDecryptStorageState) data;
                initGUI(storageParams.getResult(), storageParams.isFavoritesOnly(), storageParams.isOpenLastNode());
                break;
            case UpdateToolbar:
                ToolbarParams toolbarParams = (ToolbarParams) data;
                updateMainToolbar(toolbarParams.getViewId(), toolbarParams.getTitle());
                break;
            case HandleReceivedIntent:
                checkReceivedIntent(receivedIntent);
                break;

            // pages
            case OpenPage:
                viewPager.setCurrent((int) data);
                break;
            case MainPageCreated:
                onMainPageCreated();
                break;
            case ShowMainView:
                getMainPage().showView((int) data);
                break;
            case ClearMainView:
                getMainPage().clearView();
                break;
            case CloseFoundView:
                closeFoundFragment();
                break;

            // long-term tasks
            case TaskStarted:
                openedDrawerState = taskMainPreExecute((int)data);
                break;
            case TaskFinished:
                if (data instanceof Integer) {
                    openedDrawerState = (int)data;
                }
                taskMainPostExecute(openedDrawerState);
                break;
        }
    }

    /**
     * Обработчик изменения состояния хранилища.
     * @param state
     * @param data
     */
    private void onStorageStateChanged(Constants.StorageEvents state, Object data) {
        switch (state) {
            case PermissionCheck:
                if (PermissionManager.checkWriteExtStoragePermission(MainActivity.this, Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE)) {
                    viewModel.onPermissionChecked();
                }
                break;
            case PermissionChecked:
                viewModel.initOrSyncStorage(this);
                break;
            case AskBeforeSyncOnInit:
                AskDialogs.showSyncRequestDialog(MainActivity.this, new Dialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        viewModel.syncStorage(MainActivity.this, true);
                    }

                    @Override
                    public void onCancel() {
                        viewModel.initStorageAndLoad();
                    }
                });
                break;
            case AskAfterSyncOnInit:
                boolean isSyncSuccess = (boolean) data;
                AskDialogs.showSyncDoneDialog(this, isSyncSuccess, () -> {
                    viewModel.initStorageAndLoad();
                });
                break;
            case AskBeforeSyncOnExit:
                AskDialogs.showSyncRequestDialog(MainActivity.this, new Dialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        viewModel.syncStorage(MainActivity.this, false);
                    }

                    @Override
                    public void onCancel() {
                        viewModel.initStorageAndLoad();
                    }
                });
                break;
            case InitFailed:
                boolean isFavorMode = (boolean) data;
                initGUI(false, isFavorMode, false);
                break;
            case EmptyPassCheck: {
                AskPasswordParams params = (AskPasswordParams) data;
                PassDialogs.showEmptyPassCheckingFieldDialog(MainActivity.this, params.getFieldName(),
                        viewModel.getEmptyPassCheckingFieldCallback(params));
            } break;
            case AskPassword: {
                AskPasswordParams params = (AskPasswordParams) data;
                // выводим окно с запросом пароля
                PassDialogs.showPassEnterDialog(MainActivity.this, params.getNode(), false,
                        viewModel.getPassInputHandler(params));
            } break;
            case AskPinCode: {
                AskPasswordParams params = (AskPasswordParams) data;
                // выводим окно с запросом пароля
                PINManager.askPINCode(MainActivity.this, params.isNodeOpening(),
                        viewModel.getPinCodeInputHandler(params));
            } break;
            case AskForClearStoragePass:
                showNoCryptedNodesLeftDialog();
                break;
            case ChangedOutside:
                // выводим уведомление
                AskDialogs.showYesNoDialog(MainActivity.this, new Dialogs.IApplyCancelDismissResult() {
                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onApply() {
                        viewModel.startReinitStorage();
                    }

                    @Override
                    public void onDismiss() {
                        viewModel.dropIsStorageChangingHandled();
                    }
                }, R.string.ask_storage_changed_outside);
                break;
        }
    }

    /**
     * Обработчик действий в приложении.
     * @param action
     * @param data
     */
    private void onObjectActions(Constants.ObjectEvents action, Object data) {
        switch (action) {
            // nodes
            case ShowNode:
                setCurNode((TetroidNode) data);
                break;
            case NodeCreated:
                onNodeCreated((TetroidNode) data);
                break;
            case NodeInserted:
                onNodeInserted((TetroidNode) data);
                break;
            case NodeRenamed:
                onNodeRenamed((TetroidNode) data);
                break;
            case AskForDeleteNode:
                showDeleteNodeDialog((TetroidNode) data);
                break;
            case NodeCutted:
                onDeleteNodeResult((TetroidNode) data, true);
                break;
            case NodeDeleted:
                onDeleteNodeResult((TetroidNode) data, false);
                break;

            // records
            case OpenRecord:
                openRecord((Bundle) data);
                break;
            case ShowRecords:
                ObjectsInView param = (ObjectsInView) data;
                showRecords(param.getObjects(), param.getViewId());
                break;
            case RecordsFiltered:
                FilteredObjectsInView param2 = (FilteredObjectsInView) data;
                getMainPage().onRecordsFiltered(param2.getQuery(), param2.getObjects(), param2.getViewId());
                break;
            case RecordDeleted:
            case RecordCutted:
                getMainPage().onDeleteRecordResult((TetroidRecord) data);
                break;
            case UpdateRecords:
                getMainPage().updateRecordList();
                break;

            // tags
            case UpdateTags:
                updateTags();
                break;

            // attaches
            case CheckPermissionAndOpenAttach:
                checkPermissionAndOpenAttach((TetroidFile) data);
                break;
            case ShowAttaches:
                ObjectsInView attaches = (ObjectsInView) data;
                getMainPage().showAttaches(attaches.getObjects());
//            case ShowRecordAttaches:
//                openRecordAttaches((TetroidRecord) data);
//                break;
            case AttachesFiltered:
                FilteredObjectsInView param3 = (FilteredObjectsInView) data;
                getMainPage().onAttachesFiltered(param3.getQuery(), param3.getObjects());
                break;
            case AttachDeleted:
                getMainPage().onDeleteFileResult((TetroidFile) data);
                break;

            // favorites
            case ShowFavorites:
                showFavorites();
                break;
            case UpdateFavorites:
                updateFavorites();
                break;

            // storage tree observer
            case StartFileObserver:
                Bundle bundle = (Bundle) data;
                startStopStorageTreeObserver(true, bundle);
                break;
            case StopFileObserver:
                startStopStorageTreeObserver(false, null);
                break;

            // global search
            case GlobalSearchStart:
                showGlobalSearchActivity((String) data);
                break;
            case GlobalResearch:
                research();
                break;
            case GlobalSearchFinished:
                GlobalSearchParams searchParams = (GlobalSearchParams)data;
                onGlobalSearchFinished(searchParams.getFound(), searchParams.getScan());
                break;

            case AskForOperationWithoutDir:
                ClipboardParams clipboardParams = (ClipboardParams)data;
                if (clipboardParams.getObj() instanceof TetroidRecord) {
                    RecordDialogs.operWithoutDir(this, clipboardParams.getOperation(), () -> {
                        viewModel.doOperationWithoutDir(clipboardParams);
                    });
                } else if (clipboardParams.getObj() instanceof TetroidFile) {
                    FileDialogs.renameAttachWithoutDir(this, () -> {
                        viewModel.doOperationWithoutDir(clipboardParams);
                    });
                }
                break;
            case AskForOperationWithoutFile:
                ClipboardParams clipboardParams2 = (ClipboardParams) data;
                FileDialogs.operWithoutFile(this, clipboardParams2.getOperation(), () -> {
                    viewModel.doOperationWithoutFile(clipboardParams2);
                });
                break;
            case ShowMoreInLogs:
                showSnackMoreInLogs();
                break;
            case OpenFilePicker:
                openFilePicker();
                break;
            case OpenFolderPicker:
                openFolderPicker();
                break;
        }
    }

    /**
     * Приемник сигнала о внешнем изменении дерева записей.
     */
    private void initBroadcastReceiver() {
        this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(FileObserverService.ACTION_OBSERVER_EVENT_COME)) {
                    // обработка внешнего изменения дерева записей
//                    mOutsideChangingHandler.run(true);
                    viewModel.onStorageOutsideChanged();
                }
            }
        };
        this.broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FileObserverService.ACTION_OBSERVER_EVENT_COME);
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);
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
    public void initGUI(boolean isLoaded, boolean isOnlyFavorites, boolean isOpenLastNode) {
        // избранные записи
        int loadButtonsVis = (isLoaded && isOnlyFavorites) ? View.VISIBLE : View.GONE;
        btnLoadStorageNodes.setVisibility(loadButtonsVis);
        btnLoadStorageTags.setVisibility(loadButtonsVis);
        ViewUtils.setFabVisibility(fabCreateNode, isLoaded && !isOnlyFavorites);
        lvNodes.setVisibility((!isOnlyFavorites) ? View.VISIBLE : View.GONE);
        favoritesNode.setVisibility((isLoaded && App.isFullVersion()) ? View.VISIBLE : View.GONE);
        tvNodesEmpty.setVisibility(View.GONE);
        if (isLoaded && App.isFullVersion()) {
            updateFavorites();
        }
        // элементы фильтра веток и меток
        ViewUtils.setVisibleIfNotNull(searchViewNodes, isLoaded && !isOnlyFavorites);
        ViewUtils.setVisibleIfNotNull(searchViewTags, isLoaded && !isOnlyFavorites);
        ViewUtils.setVisibleIfNotNull((View)findViewById(R.id.button_tags_sort), isLoaded && !isOnlyFavorites);

        if (isOnlyFavorites) {
            // обработка только "ветки" избранных записей
            if (isLoaded) {
                // списки записей, файлов
                getMainPage().initListAdapters(this);
                viewModel.showFavorites();
                // список меток
                tvTagsEmpty.setText(R.string.title_load_all_nodes);
                setListEmptyViewState(tvNodesEmpty, true, R.string.title_load_all_nodes);
            } else {
                setEmptyTextViews(R.string.title_storage_not_loaded);
            }
        } else {
            // список веток
            this.listAdapterNodes = new NodesListAdapter(this, onNodeHeaderClickListener);
            lvNodes.setAdapter(listAdapterNodes);
            // список меток
            this.listAdapterTags = new TagsListAdapter(this);
            lvTags.setAdapter(listAdapterTags);

            // добавляем к результату загрузки проверку на пустоту списка веток
            List<TetroidNode> rootNodes = viewModel.getRootNodes();
            isLoaded = (isLoaded && rootNodes != null);
            if (isLoaded) {
                boolean isEmpty = rootNodes.isEmpty();
                // выбираем ветку, выбранную в прошлый раз
                boolean nodesAdapterInited = false;
                TetroidNode nodeToSelect = null;
                if (SettingsManager.isKeepLastNodeDef(this) && !isEmpty && isOpenLastNode) {
                    String nodeId = SettingsManager.getLastNodeId(this);
                    if (nodeId != null) {
                        if (nodeId.equals(FavoritesManager.FAVORITES_NODE.getId())) {
                            nodeToSelect = FavoritesManager.FAVORITES_NODE;
                        } else {
                            nodeToSelect = viewModel.getNode(nodeId);
                            if (nodeToSelect != null) {
                                Stack<TetroidNode> expandNodes = viewModel.createNodesHierarchy(nodeToSelect);
                                listAdapterNodes.setDataItems(rootNodes, expandNodes);
                                nodesAdapterInited = true;
                            }
                        }
                    }
                } else {
                    // очищаем заголовок, список
                    getMainPage().clearView();
                }
                if (!nodesAdapterInited) {
                    listAdapterNodes.setDataItems(rootNodes);
                }

                if (!isEmpty) {
                    // списки записей, файлов
                    getMainPage().initListAdapters(this);
                    if (nodeToSelect != null) {
                        if (nodeToSelect == FavoritesManager.FAVORITES_NODE) {
                            viewModel.showFavorites();
                        } else {
                            viewModel.showNode(nodeToSelect);
                        }
                    }

                    // список меток
                    setTagsDataItems(viewModel.getTags());
                    tvTagsEmpty.setText(R.string.log_tags_is_missing);
                }
                setListEmptyViewState(tvNodesEmpty, isEmpty, R.string.title_nodes_is_missing);
            } else {
                setEmptyTextViews(R.string.title_storage_not_loaded);
            }
        }
        updateOptionsMenu();
    }

    /**
     * Обработчик события, когда создался главный фрагмент активности.
     */
    public void onMainPageCreated() {
        // принудительно запускаем создание пунтов меню уже после отработки onCreate
        afterOnCreate();
        //
        this.isActivityCreated = true;
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

        if (viewModel.isLoaded()) {
            // тут ничего не пишем.
            // код отображения загруженного хранилище находится в onGUICreated(),
            //  который вызывается после создания пунктов меню активности

            // инициализация контролов
//            initGUI(true, StorageManager.isFavoritesMode(), SettingsManager.isKeepLastNodeDef(this));
            initGUI(true, viewModel.isLoadedFavoritesOnly(), viewModel.isKeepLastNode());
            // действия после загрузки хранилища
            afterStorageLoaded(true);
        } else {
            // загружаем хранилище, если еще не загружано
            viewModel.startInitStorage();
        }
    }

    private void setEmptyTextViews(@StringRes int mesId) {
        getMainPage().setRecordsEmptyViewText(getString(mesId));
        setListEmptyViewState(tvNodesEmpty, true, mesId);
        setListEmptyViewState(tvTagsEmpty, true, mesId);
        drawerLayout.closeDrawers();
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
        if (curPage == Constants.PAGE_MAIN) {
            viewModel.restoreLastMainToolbarState();
        } else {
            updateMainToolbar(Constants.MAIN_VIEW_GLOBAL_FOUND, null);
        }
    }

    /**
     * Установка заголовка и подзаголовка ToolBar.
     * @param viewId
     */
    public void updateMainToolbar(int viewId, String title) {
        switch (viewId) {
            case Constants.MAIN_VIEW_GLOBAL_FOUND:
                title = getString(R.string.title_global_search);
                break;
            case Constants.MAIN_VIEW_NONE:
                title = null;
                break;
            case Constants.MAIN_VIEW_NODE_RECORDS:
                title = viewModel.getCurNodeName();
                break;
            case Constants.MAIN_VIEW_TAG_RECORDS:
                title = viewModel.getCurTagName();
                break;
//            case MainPageFragment.VIEW_FOUND_RECORDS:
//                break;
            case Constants.MAIN_VIEW_FAVORITES:
                title = getString(R.string.title_favorites);
                break;
            case Constants.MAIN_VIEW_RECORD_FILES:
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
        if (viewId != Constants.MAIN_VIEW_GLOBAL_FOUND) {
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
        } else if (viewModel.getLastScan() != null) {
            setSubtitle("\"" + viewModel.getLastScan().getQuery() + "\"");
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
            if (isActivityCreated) {
                changeToolBarByPage(i);
            }
        }
        @Override
        public void onPageScrollStateChanged(int i) {
        }
    }

    private MainPageFragment getMainPage() {
        return viewPagerAdapter.getMainFragment();
    }

    private FoundPageFragment getFoundPage() {
        return viewPagerAdapter.getFoundFragment();
    }

    // endregion UI

    // region LoadStorage

    /**
     * Обработчик события после загрузки хранилища.
     */
    @Override
    public void afterStorageLoaded(boolean res) {
        if (res) {
            // проверяем входящий Intent после загрузки
            checkReceivedIntent(receivedIntent);
            // запускаем отслеживание изменения структуры хранилища
            viewModel.startStorageTreeObserver();
        }
    }

    private void startStopStorageTreeObserver(boolean isStart, Bundle bundle) {
        if (isStart) {
            FileObserverService.sendCommand(this, bundle);
            LogManager.log(this, getString(R.string.log_mytetra_xml_observer_mask, getString(R.string.launched)), ILogger.Types.INFO);
        } else {
            FileObserverService.sendCommand(this, FileObserverService.ACTION_STOP);
            FileObserverService.stop(this);
            LogManager.log(this, getString(R.string.log_mytetra_xml_observer_mask, getString(R.string.stopped)), ILogger.Types.INFO);
        }
    }

    /**
     * Перезагрузка хранилища (при изменении пути в настройках).
     */
    private void reinitStorage() {
        closeFoundFragment();
        getMainPage().clearView();
//        StorageManager.startInitStorage(this, this, true);
        viewModel.startReinitStorage();
    }

    /**
     * Перезагрузка хранилища.
     */
    private void reloadStorageAsk() {
        AskDialogs.showReloadStorageDialog(this, false, false, () -> {
            reinitStorage();
        });
    }

//    /**
//     * Создание нового хранилища в указанном расположении.
//     *
//     * @param storagePath
////     * @param checkDirIsEmpty
//     */
//    @Override
//    protected void createStorage(String storagePath/*, boolean checkDirIsEmpty*/) {
//        if (StorageManager.createStorage(this, storagePath)) {
//            closeFoundFragment();
//            getMainPage().clearView();
//            mDrawerLayout.openDrawer(Gravity.LEFT);
//            // сохраняем путь к хранилищу
////            if (SettingsManager.isLoadLastStoragePath()) {
//            /*SettingsManager.setStoragePath(storagePath);*/
////            }
//            initGUI(DataManager.createDefault(this), false, false);
////            LogManager.log(getString(R.string.log_storage_created) + mStoragePath, LogManager.Types.INFO, Toast.LENGTH_SHORT);
//        } else {
//            mDrawerLayout.openDrawer(Gravity.LEFT);
//            initGUI(false, false, false);
////            LogManager.log(getString(R.string.log_failed_storage_create) + mStoragePath, LogManager.Types.ERROR, Toast.LENGTH_LONG);
//        }
//    }


    // TODO: почему нигде не используется ??
//    private void initStorage(String storagePath) {
//        // читаем установленную опцию isLoadFavoritesOnly только при первой загрузке
//        boolean isFavorites = !DataManager.isLoaded() && SettingsManager.isLoadFavoritesOnlyDef(this)
//                || (DataManager.isLoaded() && DataManager.isFavoritesMode());
//
//        if (StorageManager.initStorage(this, storagePath)) {
//            mDrawerLayout.openDrawer(Gravity.LEFT);
//        } else {
//            mDrawerLayout.openDrawer(Gravity.LEFT);
//            initGUI(false, isFavorites, false);
//        }
//    }

    @Override
    public void afterStorageDecrypted(TetroidNode node) {
        updateNodes();
        updateTags();

        checkReceivedIntent(receivedIntent);
    }

    /**
     * Если включен режим только избранных записей, то сначала нужно загрузить все ветки.
     * @param data
     * @return
     */
    private boolean checkIsNeedLoadAllNodes(Intent data) {
        if (viewModel.isLoadedFavoritesOnly()) {
            receivedIntent = data;
            viewModel.loadAllNodes(true);
            return true;
        }
        return false;
    }

    // endregion LoadStorage

    // region Nodes

    /**
     * Открытие записей ветки по Id.
     *
     * @param nodeId
     */
    public void showNode(String nodeId) {
        TetroidNode node = viewModel.getNode(nodeId);
        if (node != null) {
            viewModel.showNode(node);
        } else {
            LogManager.log(this, getString(R.string.log_not_found_node_id) + nodeId, ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Установка и выделение текущей ветки.
     * @param node
     */
    private void setCurNode(TetroidNode node) {
        viewModel.setCurNode(node);
        listAdapterNodes.setCurNode(node);
        listAdapterNodes.notifyDataSetChanged();
        if (node != null && App.isFullVersion()) {
            setFavorIsCurNode(false);
        }
    }

    /**
     * Управление подсветкой ветки Избранное.
     * @param isCurNode Текущая ветка?
     */
    private void setFavorIsCurNode(boolean isCurNode) {
        favoritesNode.setBackgroundColor(ContextCompat.getColor(this,
                (isCurNode) ? R.color.colorCurNode : R.color.transparent));
    }

    /**
     * Обновление списка веток.
     */
    public void updateNodes() {
        if (listAdapterNodes != null) {
            listAdapterNodes.notifyDataSetChanged();
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
                    viewModel.showNode(node);
                } else {
                    // иначе - разворачиваем/сворачиваем ветку
                    listAdapterNodes.toggleNodeExpand(pos);
                }
            } else {
                // сразу открываем список записей (даже если он пуст)
                viewModel.showNode(node);
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
    private final OnItemClickListener onNodeClickListener = new OnItemClickListener() {

        /**
         * Клик на конечной ветке.
         */
        @Override
        public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
            viewModel.showNode((TetroidNode) item);
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
    private final OnItemLongClickListener onNodeLongClickListener = (parent, view, item, itemInfo, pos) -> {
        if (parent != lvNodes)
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
            viewModel.createNode(name, trueParentNode);
//            TetroidNode node = viewModel.createNode(name, trueParentNode);
//            if (node != null) {
//                if (listAdapterNodes.addItem(pos, isSubNode)) {
//                    TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE, node, false);
//                } else {
//                    LogManager.log(this, getString(R.string.log_create_node_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
//                }
//            } else {
//                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
//            }
        });
    }

    /**
     * Создание ветки.
     */
    private void createNode() {
        NodeDialogs.createNodeDialog(this, null, true, (name, parNode) -> {
            viewModel.createNode(name, parNode);
//            TetroidNode node = viewModel.createNode(name, parNode);
//            if (node != null) {
//                if (listAdapterNodes.addItem(parNode)) {
//                    TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE, node, false);
//                } else {
//                    LogManager.log(this, getString(R.string.log_create_node_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
//                }
//            } else {
//                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE);
//            }
        });
    }

    private void onNodeCreated(TetroidNode node) {
        if (listAdapterNodes.addItem(node.getParentNode())) {
            TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CREATE, node, false);
        } else {
            LogManager.log(this, getString(R.string.log_create_node_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
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
            viewModel.renameNode(node, name);
        });
    }

    /**
     * Переименование ветки.
     * @param node
     */
    private void onNodeRenamed(TetroidNode node) {
        if (viewModel.getCurNode() == node) {
            setTitle(node.getName());
        }
    }

    private void setNodeIcon(TetroidNode node) {
        IconsActivity.startIconsActivity(this, node, Constants.REQUEST_CODE_NODE_ICON);
    }

//    // VM
//    private void setNodeIcon(String nodeId, String iconPath, boolean isDrop) {
//        TetroidNode node = (mCurNode != null && mCurNode.getId() == nodeId) ? mCurNode
//                : NodesManager.getNode(nodeId);
//        if (NodesManager.setNodeIcon(this, node, iconPath, isDrop)) {
//            TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CHANGE);
//            updateNodeList();
//        } else {
//            TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.CHANGE);
//        }
//    }

    /**
     * Удаление ветки.
     * @param node
     */
    private void showDeleteNodeDialog(TetroidNode node) {
        NodeDialogs.deleteNode(this, node.getName(), () -> {
//            boolean res = NodesManager.deleteNode(this, node);
//            onDeleteNodeResult(node, res, false);
            viewModel.deleteNode(node);
        });
    }

    private void onDeleteNodeResult(TetroidNode node, boolean isCutted) {
        // удаляем элемент внутри списка
        if (listAdapterNodes.deleteItem(node)) {
            TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, (isCutted) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);
        } else {
            LogManager.log(this, getString(R.string.log_node_delete_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
        // VM
//        // обновляем label с количеством избранных записей
//        if (App.isFullVersion()) {
//            updateFavorites();
//        }
//        // убираем список записей удаляемой ветки
//        if (mCurNode == node || isNodeInNode(mCurNode, node)) {
//            getMainPage().clearView();
//            this.mCurNode = null;
//        }
//        if (node.isCrypted()) {
//            // проверяем существование зашифрованных веток
//            checkExistenceCryptedNodes();
//        }
    }

    /**
     * Перемещение ветки вверх/вниз по списку.
     * @param node
     * @param pos  Позиция элемента в списке
     * @param isUp
     */
    private void reorderNode(TetroidNode node, int pos, boolean isUp) {
        if (node == null)
            return;
        TetroidNode parentNode = node.getParentNode();
        List<TetroidNode> subNodes = (parentNode != null) ? parentNode.getSubNodes() : viewModel.getRootNodes();
        if (subNodes.size() > 0) {
            int posInNode = subNodes.indexOf(node);
            int res = viewModel.swapTetroidObjects(subNodes, posInNode, isUp, true);
            if (res > 0) {
                // меняем местами элементы внутри списка
                int newPosInNode = (isUp) ?
                        (posInNode == 0) ? subNodes.size() - 1 : posInNode - 1
                        : (posInNode == subNodes.size() - 1) ? 0 : posInNode + 1;
//                if (mListAdapterNodes.swapItems(pos, posInNode, (isUp) ? posInNode - 1 : posInNode + 1)) {
                if (listAdapterNodes.swapItems(pos, posInNode, newPosInNode)) {
                    TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.REORDER);
                } else {
                    LogManager.log(this, getString(R.string.log_node_move_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
                }
            } else if (res < 0) {
                TetroidLog.logOperErrorMore(this, TetroidLog.Objs.NODE, TetroidLog.Opers.REORDER);
            }
        }
    }

    /**
     * Развернуть все подветки у ветки.
     * @param pos
     */
    private void expandSubNodes(int pos) {
        listAdapterNodes.extendNodeSubnodes(pos, NestType.MULTIPLE);
    }

    /**
     * Копирование ветки.
     * @param node
     */
    private void copyNode(TetroidNode node) {
        if (viewModel.getNodesInteractor().hasNonDecryptedNodes(node)) {
            Message.show(this, getString(R.string.log_enter_pass_first), Toast.LENGTH_LONG);
            return;
        }
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(node);
        TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.COPY);
    }

//    /**
//     * Вырезание ветки из родительской ветки.
//     * @param node
//     */
//    private void cutNode(TetroidNode node, int pos) {
//        if (viewModel.getNodesInteractor().hasNonDecryptedNodes(node)) {
//            Message.show(this, getString(R.string.log_enter_pass_first), Toast.LENGTH_LONG);
//            return;
//        }
//        // добавляем в "буфер обмена"
//        TetroidClipboard.cut(node);
//        // удаляем ветку из родительской ветки вместе с записями
//        boolean res = viewModel.cutNode(this, node);
//        onDeleteNodeResult(node, res, pos, false);
//    }

    /**
     * Вставка ветки.
     * @param parentNode Родительская ветка
     * @param pos        Позиция в списке родительской ветки
     * @param isSubNode  Если true, значит как подветка, иначе рядом с выделенной веткой
     */
    private void insertNode(TetroidNode parentNode, int pos, boolean isSubNode) {
        viewModel.insertNode(parentNode, isSubNode);
    }

    private void onNodeInserted(TetroidNode node) {
//        if (listAdapterNodes.addItem(pos, isSubNode)) {
        if (listAdapterNodes.addItem(node)) {
            TetroidLog.logOperRes(this, TetroidLog.Objs.NODE, TetroidLog.Opers.INSERT);
        } else {
            LogManager.log(this, getString(R.string.log_create_node_list_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Зашифровка ветки.
     * @param node
     */
    private void encryptNode(TetroidNode node) {
        if (node == viewModel.getQuicklyNode()) {
            Message.show(this, getString(R.string.mes_quickly_node_cannot_encrypt));
            return;
        }
        PassManager.checkStoragePass(this, node, new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
//                mCurTask = new CryptNodeTask(node, true).run();
                viewModel.encryptNode(node);
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
                viewModel.decryptNode(node);
            }

            @Override
            public void onCancel() {
            }
        });
    }

    /**
     * Диалог с вопросом о сбросе пароля в связи с отсутствием зашифрованных веток.
     */
    private void showNoCryptedNodesLeftDialog() {
        AskDialogs.showYesDialog(this, () -> viewModel.clearSavedPass(), R.string.ask_clear_pass_database_ini);
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
//        {
            // выделяем ветку Избранное, только если загружено не одно Избранное
//            if (!App.IsLoadedFavoritesOnly) {
            if (!viewModel.isLoadedFavoritesOnly()) {
                setCurNode(null);
                setFavorIsCurNode(true);
            }
            viewModel.showRecords(FavoritesManager.getFavoritesRecords(), Constants.MAIN_VIEW_FAVORITES, true);

            // VM
            // сохраняем выбранную ветку
//            saveLastSelectedNode();
//        }
    }

    /**
     * Обновление ветки Избранное.
     */
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
        listAdapterTags.setDataItems(tags, new SortHelper(
                SettingsManager.getTagsSortMode(this, SortHelper.byNameAsc())));
    }

    /**
     * Обновление списка меток.
     */
    public void updateTags() {
        setTagsDataItems(viewModel.getTags());
    }

    // endregion Tags


    // region Tag

    /**
     * Отображение записей по метке.
     * @param position Индекс метки в списке
     */
    private void showTagRecords(int position) {
        TetroidTag tag = listAdapterTags.getItem(position).getValue();
        viewModel.showTag(tag);
    }

    /**
     * Переименование метки в записях.
     * @param tag
     */
    private void renameTag(TetroidTag tag) {
        TagDialogs.createTagDialog(this, tag, name -> {
            viewModel.renameTag(tag, name);
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
        if (dropSearch && searchViewRecords != null && !searchViewRecords.isIconified()) {
            // сбрасываем SearchView;
            // но т.к. при этом срабатывает событие onClose, нужно избежать повторной загрузки
            // полного списка записей в его обработчике с помощью проверки mIsDropRecordsFiltering
            viewModel.setDropRecordsFiltering(false);
//          mSearchViewRecords.onActionViewCollapsed();
            searchViewRecords.setQuery("", false);
            searchViewRecords.setIconified(true);
            viewModel.setDropRecordsFiltering(true);
        }

        drawerLayout.closeDrawers();
        viewPager.setCurrent(Constants.PAGE_MAIN);
        getMainPage().showRecords(records, viewId);
    }

    // endregion Records


    // region Record

    /**
     * Открытие активности RecordActivity.
     * @param bundle
     */
    public void openRecord(Bundle bundle) {
        ViewUtils.startActivity(this, RecordActivity.class, bundle, Intent.ACTION_MAIN, 0,
                Constants.REQUEST_CODE_RECORD_ACTIVITY);
    }

    // endregion Record

    // region Attaches

//    @Override
//    public void openRecordAttaches(TetroidRecord record) {
//        viewModel.checkCurNode(record);
//        getMainPage().showRecordAttaches(record, false);
//        mViewPager.setCurrent(MainViewPager.PAGE_MAIN);
//    }

    /**
     * Отрытие прикрепленного файла.
     * Если файл нужно расшифровать во временные каталог, спрашиваем разрешение
     * на запись во внешнее хранилище.
     * <p>
     * FIXME: Разрешение WRITE_EXTERNAL_STORAGE просить не нужно,
     *  т.к. оно и так запрашивается при загрузке хранилища.
     *
     * @param file
     */
    @SuppressLint("MissingPermission")
    public void checkPermissionAndOpenAttach(TetroidFile file) {
        if (file == null) {
            return;
        }
//        if (Build.VERSION.SDK_INT >= 23) {
        // если файл нужно расшифровать во временный каталог, нужно разрешение на запись
        if (file.getRecord().isCrypted() && SettingsManager.isDecryptFilesInTempDef(this)
                && !PermissionManager.writeExtStoragePermGranted(this)) {
            viewModel.setTempFileToOpen(file);
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            LogManager.log(this, getString(R.string.log_request_perm) + permission, ILogger.Types.INFO);
            ActivityCompat.requestPermissions(this,
                    new String[]{permission}, Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP);
        } else {
//        }
            // расшифровываем без запроса разрешения во время выполнения, т.к. нужные разрешения
            // уже были выданы при установке приложения
            viewModel.openAttach(file);
        }
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
        Map.Entry<String, TetroidTag> tagEntry = listAdapterTags.getItem(position);
        if (tagEntry != null) {
            showTagPopupMenu(view, tagEntry.getValue());
        }
        return true;
    };

    // endregion Attaches

    // region ContextMenus

    /**
     * Отображение всплывающего (контексного) меню ветки.
     *
     * FIXME: Заменить на использование AlertDialog ? (чтобы посередине экрана)
     *
     * @param v
     * @param node
     */
    @SuppressLint({"RestrictedApi", "NonConstantResourceId"})
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
        int nodesCount = ((parentNode != null) ? parentNode.getSubNodes() : viewModel.getRootNodes()).size();
//        visibleMenuItem(menu.findItem(R.id.action_move_up), pos > 0);
//        visibleMenuItem(menu.findItem(R.id.action_move_down), pos < nodesCount - 1);
        visibleMenuItem(menu.findItem(R.id.action_move_up), nodesCount > 0);
        visibleMenuItem(menu.findItem(R.id.action_move_down), nodesCount > 0);
        boolean canInsert = TetroidClipboard.hasObject(FoundType.TYPE_NODE);
        visibleMenuItem(menu.findItem(R.id.action_insert), canInsert);
        visibleMenuItem(menu.findItem(R.id.action_insert_subnode), canInsert && isNonCrypted);
        visibleMenuItem(menu.findItem(R.id.action_copy), isNonCrypted);
        boolean canCutDel = node.getLevel() > 0 || viewModel.getRootNodes().size() > 1;
        visibleMenuItem(menu.findItem(R.id.action_cut), canCutDel && isNonCrypted);
        visibleMenuItem(menu.findItem(R.id.action_delete), canCutDel);
        visibleMenuItem(menu.findItem(R.id.action_encrypt_node), !node.isCrypted());
        boolean canNoCrypt = node.isCrypted() && (parentNode == null || !parentNode.isCrypted());
        visibleMenuItem(menu.findItem(R.id.action_no_encrypt_node), canNoCrypt);
        visibleMenuItem(menu.findItem(R.id.action_info), isNonCrypted);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_open_node:
                    viewModel.showNode(node);
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
                    reorderNode(node, pos, true);
                    return true;
                case R.id.action_move_down:
                    reorderNode(node, pos, false);
                    return true;
                case R.id.action_copy:
                    copyNode(node);
                    return true;
                case R.id.action_cut:
                    viewModel.cutNode(node, pos);
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
                    viewModel.startDeleteNode(node);
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
    @SuppressLint({"RestrictedApi", "NonConstantResourceId"})
    private void showTagPopupMenu(View v, TetroidTag tag) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.tag_context);

        Menu menu = popupMenu.getMenu();
        visibleMenuItem(menu.findItem(R.id.action_rename), App.isFullVersion());

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_open_tag:
                    viewModel.showTag(tag);
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

    @SuppressLint("NonConstantResourceId")
    private void showTagsSortPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.tags_sort);

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_sort_tags_name_asc:
                    listAdapterTags.sort(true, true);
                    SettingsManager.setTagsSortMode(this, SortHelper.byNameAsc());
                    return true;
                case R.id.action_sort_tags_name_desc:
                    listAdapterTags.sort(true, false);
                    SettingsManager.setTagsSortMode(this, SortHelper.byNameDesc());
                    return true;
                case R.id.action_sort_tags_count_asc:
                    listAdapterTags.sort(false, true);
                    SettingsManager.setTagsSortMode(this, SortHelper.byCountAsc());
                    return true;
                case R.id.action_sort_tags_count_desc:
                    listAdapterTags.sort(false, false);
                    SettingsManager.setTagsSortMode(this, SortHelper.byCountDesc());
                    return true;
                default:
                    return false;
            }
        });
        setForceShowMenuIcons(v, (MenuBuilder) popupMenu.getMenu());
    }

    // endregion ContextMenus

    // region OnActivityResult

    /**
     * Обработка возвращаемого результата других активностей.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_CODE_STORAGES_ACTIVITY: {
                onStoragesActivityResult(data);
            } break;
            case Constants.REQUEST_CODE_SETTINGS_ACTIVITY: {
                onSettingsActivityResult(data);
            } break;
            case Constants.REQUEST_CODE_RECORD_ACTIVITY: {
                onRecordActivityResult(resultCode, data);
            } break;
            case Constants.REQUEST_CODE_SEARCH_ACTIVITY: {
                if (resultCode == RESULT_OK) {
                    ScanManager scan = data.getParcelableExtra(Constants.EXTRA_SCAN_MANAGER);
                    viewModel.startGlobalSearch(scan);
                }
            } break;
            case Constants.REQUEST_CODE_SYNC_STORAGE: {
                viewModel.onSyncStorageFinished(resultCode == RESULT_OK);
            } break;
            case Constants.REQUEST_CODE_FILE_PICKER: {
                if (resultCode == RESULT_OK) {
                    String fileFullName = data.getStringExtra(FolderPicker.EXTRA_DATA);
                    // сохраняем путь
                    SettingsManager.setLastChoosedFolder(this, FileUtils.getFileFolder(fileFullName));
                    viewModel.attachFileToCurRecord(fileFullName, false);
                }
            } break;
            case Constants.REQUEST_CODE_FOLDER_PICKER: {
                if (resultCode == RESULT_OK) {
                    String folderPath = data.getStringExtra(FolderPicker.EXTRA_DATA);
                    // сохраняем путь
                    SettingsManager.setLastChoosedFolder(this, folderPath);
                    viewModel.saveCurAttachOnDevice(folderPath);
                }
            } break;
            case Constants.REQUEST_CODE_NODE_ICON: {
                if (resultCode == RESULT_OK) {
                    String nodeId = data.getStringExtra(Constants.EXTRA_NODE_ID);
                    String iconPath = data.getStringExtra(Constants.EXTRA_NODE_ICON_PATH);
                    boolean isDrop = data.getBooleanExtra(Constants.EXTRA_IS_DROP, false);
                    viewModel.setNodeIcon(nodeId, iconPath, isDrop);
                }
            } break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Обработка возвращаемого результата активности со списком хранилищ.
     * @param data
     */
    private void onStoragesActivityResult(Intent data) {
        // проверяем нужно ли отслеживать структуру хранилища
        viewModel.startStorageTreeObserver();

        // скрываем пункт меню Синхронизация, если отключили
        updateOptionsMenu();
        // обновляем списки, могли измениться настройки отображения
        getMainPage().updateRecordList();
        updateNodes();

        onStorageChangedIntent(data);
    }

    /**
     * Обработка возвращаемого результата активности настроек приложения.
     * @param data
     */
    private void onSettingsActivityResult(Intent data) {
        // проверяем нужно ли отслеживать структуру хранилища
        viewModel.startStorageTreeObserver();

        // скрываем пункт меню Синхронизация, если отключили
        updateOptionsMenu();
        // обновляем списки, могли измениться настройки отображения
        getMainPage().updateRecordList();
        updateNodes();

        onStorageChangedIntent(data);
    }

    private void onStorageChangedIntent(Intent data) {
        if (data != null) {
            // перезагружаем хранилище, если изменили путь
//            if (data.getBooleanExtra(Constants.EXTRA_IS_REINIT_STORAGE, false)) {
//                boolean toCreate = data.getBooleanExtra(Constants.EXTRA_IS_CREATE_STORAGE, false);
//                AskDialogs.showReloadStorageDialog(this, toCreate, true, () -> {
//                    if (toCreate) {
//                        createStorage(SettingsManager.getStoragePath(this)/*, true*/);
//                    } else {
//                        reinitStorage();
//                    }
//                });
//            } else
            if (data.getBooleanExtra(Constants.EXTRA_IS_LOAD_STORAGE, false)) {
//                StorageManager.setStorageCallback(this);
//                StorageManager.startInitStorage(this, this, false);

                // TODO!!
                int storageId = data.getIntExtra(Constants.EXTRA_STORAGE_ID, 0);
                viewModel.startInitStorage(storageId);

            } else if (data.getBooleanExtra(Constants.EXTRA_IS_LOAD_ALL_NODES, false)) {
                viewModel.loadAllNodes(false);
            } else if (data.getBooleanExtra(Constants.EXTRA_IS_PASS_CHANGED, false)) {
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
            // обычное закрытие активности

            // проверяем пора ли запустить диалог оценки приложения
            checkForInAppReviewShowing();

            return;
        }
        // проверяем нужно ли отслеживать структуру хранилища
        viewModel.startStorageTreeObserver();

        if (data.getBooleanExtra(Constants.EXTRA_IS_FIELDS_EDITED, false)) {
            // обновляем списки, если редактировали свойства записи
            viewModel.onRecordFieldsUpdated(null, false);
        } else {
            // обновляем список записей, чтобы обновить дату изменения
            if (App.RecordFieldsInList.checkIsEditedDate()) {
                getMainPage().updateRecordList();
            }
        }
        switch (resCode) {
            case RecordActivity.RESULT_REINIT_STORAGE:
                /*if (data.getBooleanExtra(Constants.EXTRA_IS_CREATE_STORAGE, false)) {
                    createStorage(SettingsManager.getStoragePath(this)*//*, true*//*);
                } else*/ {
                    reinitStorage();
                    // сбрасываем Intent, чтобы избежать циклической перезагрузки хранилища
                    this.receivedIntent = null;
                }
                break;
            case RecordActivity.RESULT_PASS_CHANGED:
                if (data.getBooleanExtra(Constants.EXTRA_IS_PASS_CHANGED, false)) {
                    // обновляем списки, т.к. хранилище должно было расшифроваться
                    updateNodes();
                    updateTags();
                }
                break;
            case RecordActivity.RESULT_OPEN_RECORD:
                if (checkIsNeedLoadAllNodes(data)) return;

                String recordId = data.getStringExtra(Constants.EXTRA_OBJECT_ID);
                if (recordId != null) {
                    viewModel.openRecord(recordId);
                }
                break;
            case RecordActivity.RESULT_OPEN_NODE:
                if (checkIsNeedLoadAllNodes(data)) return;

                String nodeId = data.getStringExtra(Constants.EXTRA_OBJECT_ID);
                if (nodeId != null) {
                    showNode(nodeId);
                }
                break;
            case RecordActivity.RESULT_SHOW_ATTACHES:
                String recordId2 = data.getStringExtra(Constants.EXTRA_OBJECT_ID);
                TetroidRecord record = viewModel.getRecord(recordId2);
                if (record != null) {
                    viewModel.showRecordAttaches(record, true);
                }
                break;
            case RecordActivity.RESULT_SHOW_TAG:
                if (checkIsNeedLoadAllNodes(data)) return;

                String tagName = data.getStringExtra(Constants.EXTRA_TAG_NAME);
                viewModel.showTag(tagName);
                break;
            case RecordActivity.RESULT_DELETE_RECORD:
                String recordId3 = data.getStringExtra(Constants.EXTRA_OBJECT_ID);
                if (recordId3 != null) {
                    TetroidRecord record2 = viewModel.getRecord(recordId3);
                    viewModel.deleteRecord(record2);
                }
                break;
        }
    }

    @Override
    protected void onPermissionGranted(int permission) {
        switch (permission) {
            case Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE:
                viewModel.onPermissionChecked();
                break;
            case Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP:
                checkPermissionAndOpenAttach(viewModel.getTempFileToOpen());
                break;
            case Constants.REQUEST_CODE_PERMISSION_TERMUX:
                viewModel.startStorageSyncAndInit(this);
                break;
        }
    }

    // endregion OnActivityResult

    // region GlobalSearch

    public void setFoundPageVisibility(boolean isVisible) {
        if (!isVisible) {
            viewPager.setCurrent(Constants.PAGE_MAIN);
        }
        viewPager.setPagingEnabled(isVisible);
        titleStrip.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
    }

    void onGlobalSearchFinished(HashMap<ITetroidObject, FoundType> found, ScanManager scan) {
        getFoundPage().setFounds(found, scan);
        viewPagerAdapter.notifyDataSetChanged(); // для обновления title у страницы
        setFoundPageVisibility(true);
        viewPager.setCurrent(Constants.PAGE_FOUND);
    }

    public void closeFoundFragment() {
        setFoundPageVisibility(false);
    }

    public void research() {
        showGlobalSearchActivity(null);
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

        } else*/
        switch (action) {
            case Intent.ACTION_SEARCH:
                // обработка результата голосового поиска
                String query = intent.getStringExtra(SearchManager.QUERY);
                searchViewRecords.setQuery(query, true);

                break;
            case Constants.ACTION_RECORD:
                int resCode = intent.getIntExtra(Constants.EXTRA_RESULT_CODE, 0);
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

                break;
            case Constants.ACTION_MAIN_ACTIVITY:
                if (intent.hasExtra(Constants.EXTRA_SHOW_STORAGE_INFO)) {
                    showStorageInfoActivity();
                }

                break;
            case Intent.ACTION_SEND: {
                // прием текста/изображения из другого приложения
                String type = intent.getType();
                if (type == null) {
                    return;
                }
                String text = null;
                boolean isText = false;
                ArrayList<Uri> uris = null;
                if (type.startsWith("text/")) {
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

                break;
            }
            case Intent.ACTION_SEND_MULTIPLE: {
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
                break;
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
        if (viewModel.isLoadedFavoritesOnly()) {
            // если загружено только избранное, то нужно сначала загрузить все ветки,
            // чтобы добавить текст/картинку в одну из записей или в новую запись одной из веток
            Spanned mes = Utils.fromHtml(String.format(getString(R.string.text_load_nodes_before_receive_mask),
                    getString((isText) ? R.string.word_received_text :
                            (imagesUri != null && imagesUri.size() > 1) ? R.string.word_received_images
                                    : R.string.word_received_image)));
            Dialogs.showAlertDialog(this, mes,
                    () -> {
                        // сохраняем Intent и загружаем хранилище
                        this.receivedIntent = intent;
                        viewModel.loadAllNodes(false);
                    });
            return;
        }

        IntentDialog.createDialog(this, isText, (receivedData) -> {
                if (receivedData.isCreate()) {
                    viewModel.createRecordFromIntent(intent, isText, text, imagesUri, receivedData);
                } else {
                    // TODO: реализовать выбор имеющихся записей
                }
            });
    }

    // endregion OnNewIntent

    // region Search

    /**
     * Фильтр меток по названию.
     * @param query
     * @param isSearch Если false, то происходит сброс фильтра.
     */
    private void searchInTags(String query, boolean isSearch) {
        if (!viewModel.isLoaded()) {
            onGUICreated();
            return;
        }
        Map<String, TetroidTag> tags;
        if (isSearch) {
            tags = ScanManager.searchInTags(viewModel.getTags(), query);
        } else {
            tags = viewModel.getTags();
        }
        setTagsDataItems(tags);
        if (tags.isEmpty()) {
            tvTagsEmpty.setText((isSearch)
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
        this.searchViewRecords = (SearchView) menuItem.getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchViewRecords.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchViewRecords.setIconifiedByDefault(true);
        searchViewRecords.setQueryRefinementEnabled(true);

        new SearchViewXListener(searchViewRecords) {
            @Override
            public void onSearchClick() {
            }

            @Override
            public void onQuerySubmit(String query) {
                viewModel.filterListInMainPage(query, true);
            }

            @Override
            public void onQueryChange(String query) {
                viewModel.filterListInMainPage(query, false);
            }

            @Override
            public void onSuggestionSelectOrClick(String query) {
                searchViewRecords.setQuery(query, true);
            }

            @Override
            public void onClose() {
                viewModel.onRecordsSearchClose();
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
                if (listAdapterNodes == null)
                    return;
                searchInNodesNames(null);
                setListEmptyViewState(tvNodesEmpty, viewModel.getRootNodes().isEmpty(), R.string.title_nodes_is_missing);
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
        if (listAdapterNodes != null) {
            if (TextUtils.isEmpty(query)) {
                // просто выводим все ветки
                listAdapterNodes.setDataItems(viewModel.getRootNodes());
                setListEmptyViewState(tvNodesEmpty, false, "");
            } else {
                List<TetroidNode> found = ScanManager.searchInNodesNames(viewModel.getRootNodes(), query);
                listAdapterNodes.setDataItems(found);
                setListEmptyViewState(tvNodesEmpty, found.isEmpty(), String.format(getString(R.string.search_nodes_not_found_mask), query));
            }
        }
    }

    private void closeSearchView(SearchView search) {
        search.setIconified(true);
        search.setQuery("", false);
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
        this.optionsMenu = menu;

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

        int curViewId = (viewPagerAdapter != null)
                ? viewModel.getCurMainViewId() : 0;
        boolean canSearchRecords = (viewPager != null
                && viewPager.getCurrentItem() == Constants.PAGE_MAIN
                && (curViewId == Constants.MAIN_VIEW_NODE_RECORDS
                || curViewId == Constants.MAIN_VIEW_TAG_RECORDS));
        visibleMenuItem(menu.findItem(R.id.action_search_records), canSearchRecords);
        visibleMenuItem(menu.findItem(R.id.action_storage_sync), SettingsManager.isSyncStorageDef(this));

//        boolean isStorageLoaded = StorageManager.isLoaded();
        boolean isStorageLoaded = viewModel.isLoaded();
        enableMenuItem(menu.findItem(R.id.action_search_records), isStorageLoaded);
        enableMenuItem(menu.findItem(R.id.action_global_search), isStorageLoaded);
        enableMenuItem(menu.findItem(R.id.action_storage_sync), isStorageLoaded);
        enableMenuItem(menu.findItem(R.id.action_storage_info), isStorageLoaded);
//        enableMenuItem(menu.findItem(R.id.action_storage_reload), !TextUtils.isEmpty(SettingsManager.getStoragePath(this)));

        // TODO: ??
        enableMenuItem(menu.findItem(R.id.action_storage_reload), SettingsManager.getLastStorageId(this) != 0);

        getMainPage().onPrepareOptionsMenu(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Обработчик выбора пунктов системного меню.
     * @param item
     * @return
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_move_back:
                viewModel.moveBackFromAttaches();
                return true;
            case R.id.action_global_search:
                showGlobalSearchActivity(null);
                return true;
            case R.id.action_storage_sync:
//                StorageManager.startStorageSync(this, DataManager.getStoragePath(), null);
                viewModel.startStorageSync(this, null);
                return true;
            case R.id.action_storage_info:
                showStorageInfoActivity();
                return true;
            case R.id.action_storage_reload:
                reloadStorageAsk();
                return true;
            case R.id.action_storages:
                showStoragesActivity();
                return true;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, Constants.REQUEST_CODE_SETTINGS_ACTIVITY);
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
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                if (SettingsManager.isConfirmAppExit(this)) {
                    askForExit();
                } else {
                    onBeforeExit();
                    onExit();
                    super.onBackPressed();
                }
            } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                boolean needToOpenDrawer = true;
                int curPage = viewPager.getCurrentItem();
                if (curPage == Constants.PAGE_MAIN || curPage == Constants.PAGE_FOUND) {
                    if (curPage == Constants.PAGE_MAIN && getMainPage().onBackPressed()
                            || curPage == Constants.PAGE_FOUND && getFoundPage().onBackPressed()) {
                        needToOpenDrawer = false;
                    }
                }
                // открываем левую шторку, если все проверили
                if (needToOpenDrawer) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        } else {
            boolean needToExit = true;
            // выходить, если не отображаются боковые панели
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                int curPage = viewPager.getCurrentItem();
                if (curPage == Constants.PAGE_MAIN || curPage == Constants.PAGE_FOUND) {
                    if (curPage == Constants.PAGE_MAIN && !getMainPage().onBackPressed()
                            || curPage == Constants.PAGE_FOUND && !getFoundPage().onBackPressed()) {
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
//        StorageManager.setIsPINNeedToEnter();
        viewModel.setIsPINNeedToEnter();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        broadcastManager.unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    private void askForExit() {
        AskDialogs.showExitDialog(this, () -> {
            onBeforeExit();
        });
    }

    private void onBeforeExit() {
        // синхронизация перед выходом из приложения
//        StorageManager.startStorageSyncAndExit(this, () -> {
        viewModel.startStorageSyncAndExit(this, () -> {
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
//        DataManager.destruct();
    }

    // endregion Exit


    // region StartActivity

    private void showStoragesActivity() {
        ViewUtils.startActivity(this, StoragesActivity.class, null, Constants.REQUEST_CODE_STORAGES_ACTIVITY);
    }

    private void showStorageInfoActivity() {
        if (viewModel.isLoadedFavoritesOnly()) {
            AskDialogs.showLoadAllNodesDialog(this,
                    () -> {
                        // помещаем Intent для обработки
                        Intent intent = new Intent(Constants.ACTION_MAIN_ACTIVITY);
                        intent.putExtra(Constants.EXTRA_SHOW_STORAGE_INFO, true);
                        this.receivedIntent = intent;
                        // загружаем все ветки
                        viewModel.loadAllNodes(true);
                    });
        } else {
            ViewUtils.startActivity(this, InfoActivity.class, null);
        }
    }

    private void showGlobalSearchActivity(String query) {
        if (viewModel.isLoadedFavoritesOnly()) {
            Message.show(this, getString(R.string.title_need_load_nodes), Toast.LENGTH_LONG);
        } else {
            Intent intent = new Intent(this, SearchActivity.class);
            if (query != null) {
                intent.putExtra(Constants.EXTRA_QUERY, query);
            }
//            intent.putExtra(EXTRA_CUR_NODE_IS_NOT_NULL, (mCurNode != null));
            intent.putExtra(Constants.EXTRA_CUR_NODE_ID, (viewModel.getCurNode() != null) ? viewModel.getCurNode().getId() : null);
            startActivityForResult(intent, Constants.REQUEST_CODE_SEARCH_ACTIVITY);
        }
    }

    // endregion StartActivity

    // region Tasks

    public int taskMainPreExecute(int progressTextResId) {
        super.taskPreExecute(progressTextResId);
        int openedDrawer = (drawerLayout.isDrawerOpen(Gravity.LEFT)) ? Gravity.LEFT
                : (drawerLayout.isDrawerOpen(Gravity.RIGHT)) ? Gravity.RIGHT : Gravity.NO_GRAVITY;
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        return openedDrawer;
    }

    public void taskMainPostExecute(int openedDrawer) {
        super.taskPostExecute();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        if (openedDrawer != Gravity.NO_GRAVITY) {
            drawerLayout.openDrawer(openedDrawer);
        }
    }

    //endregion Tasks

}

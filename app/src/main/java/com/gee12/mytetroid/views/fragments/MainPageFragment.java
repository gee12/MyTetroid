package com.gee12.mytetroid.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.viewmodels.MainViewModel;
import com.gee12.mytetroid.views.activities.MainActivity;
import com.gee12.mytetroid.views.adapters.FilesListAdapter;
import com.gee12.mytetroid.views.adapters.RecordsListAdapter;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.views.dialogs.attach.AttachFieldsDialog;
import com.gee12.mytetroid.views.dialogs.attach.AttachFileByURLDialog;
import com.gee12.mytetroid.views.dialogs.attach.AttachInfoDialog;
import com.gee12.mytetroid.views.dialogs.attach.AttachAskDialogs;
import com.gee12.mytetroid.views.dialogs.record.RecordDialogs;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.dialogs.record.RecordFieldsDialog;
import com.gee12.mytetroid.views.dialogs.record.RecordInfoDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MainPageFragment extends TetroidFragment<MainViewModel> {

    private ViewFlipper viewFlipperMain;
    private ListView lvRecords;
    private ListView lvAttaches;
    private TextView tvRecordsEmpty;
    private TextView tvFilesEmpty;
    private FloatingActionButton fabAddRecord;
    private com.github.clans.fab.FloatingActionMenu fabAddAttach;
    private Button btnUseGlobalSearch;
    private RecordsListAdapter listAdapterRecords;
    private FilesListAdapter listAdapterAttaches;


    @Override
    protected Class<MainViewModel> getViewModelClazz() {
        return MainViewModel.class;
    }

    public MainPageFragment(GestureDetectorCompat gestureDetector) {
        super(gestureDetector);
    }

    public MainPageFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, R.layout.fragment_main);

        this.viewFlipperMain = view.findViewById(R.id.view_flipper_main);
        // обработка нажатия на пустом месте экрана, когда записей в ветке нет
        viewFlipperMain.setOnTouchListener(this);

        // список записей
        this.lvRecords = view.findViewById(R.id.list_view_records);

        // обработка нажатия на пустом месте списка записей
        lvRecords.setOnTouchListener(this);
        //
        lvRecords.setOnItemClickListener(onRecordClicklistener);
        this.tvRecordsEmpty = view.findViewById(R.id.text_view_empty_records);
        this.btnUseGlobalSearch = view.findViewById(R.id.button_global_search);
        btnUseGlobalSearch.setOnClickListener(v -> viewModel.startGlobalSearchFromFilterQuery());
        lvRecords.setEmptyView(tvRecordsEmpty);

        // пустое пространство под списками
        View recordsFooter =  ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        lvRecords.addFooterView(recordsFooter, null, false);
        registerForContextMenu(lvRecords);
        /*mListViewRecords.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });*/

        // список файлов
        this.lvAttaches = view.findViewById(R.id.list_view_files);
        // обработка нажатия на пустом месте списка файлов
        lvAttaches.setOnTouchListener(this);
        lvAttaches.setOnItemClickListener(onFileClicklistener);
        this.tvFilesEmpty = view.findViewById(R.id.text_view_empty_files);
        lvAttaches.setEmptyView(tvFilesEmpty);

        // пустое пространство под списками
        View filesFooter =  ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        lvAttaches.addFooterView(filesFooter, null, false);
        registerForContextMenu(lvAttaches);

        this.fabAddRecord = view.findViewById(R.id.button_add_record);
        fabAddRecord.setOnClickListener(v -> createRecord());

        this.fabAddAttach = view.findViewById(R.id.fab_add_attach);
        fabAddAttach.setClosedOnTouchOutside(true);

        com.github.clans.fab.FloatingActionButton fabAddLocalFile = view.findViewById(R.id.fab_attach_local_file);
        fabAddLocalFile.setOnClickListener(v -> {
            fabAddAttach.close(true);
            viewModel.pickAndAttachFile();
        });
        com.github.clans.fab.FloatingActionButton fabAttachFileByLink = view.findViewById(R.id.fab_attach_file_by_link);
        fabAttachFileByLink.setOnClickListener(v -> {
            fabAddAttach.close(true);
            new AttachFileByURLDialog(
                    url -> viewModel.downloadAndAttachFile(url)
            ).showIfPossible(getParentFragmentManager());
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)getActivity()).onMainPageCreated();
    }

    public void initListAdapters(Context context) {
        // список записей
        if (lvRecords != null) {
            this.listAdapterRecords = new RecordsListAdapter(
                    context,
                    viewModel.getRecordsInteractor(),
                    viewModel.getCommonSettingsInteractor(),
                    onRecordAttachmentClickListener
            );
            lvRecords.setAdapter(listAdapterRecords);
        }
        // список файлов
        if (lvAttaches != null) {
            this.listAdapterAttaches = new FilesListAdapter(context, viewModel.getAttachesInteractor());
            lvAttaches.setAdapter(listAdapterAttaches);
        }
    }

    /**
     *
     * @param viewId
     */
    public void showView(int viewId) {
        fabAddRecord.hide();
        fabAddAttach.hideMenu(true);

        int whichChild = viewId;
        String title = null;
        switch (viewId) {
            // для "очистки" активности выводим пустой список записей
            case Constants.MAIN_VIEW_NONE:
            // один контрол на записи ветки и метки
            case Constants.MAIN_VIEW_TAG_RECORDS:
                whichChild = Constants.MAIN_VIEW_NODE_RECORDS;
                break;
            case Constants.MAIN_VIEW_NODE_RECORDS:
                fabAddRecord.show();
                break;
            case Constants.MAIN_VIEW_RECORD_FILES:
                if (!viewModel.isLoadedFavoritesOnly()) {
                    fabAddAttach.showMenu(true);
                }
                title = ((viewModel.getCurRecord() != null) ? viewModel.getCurRecord().getName() : "");
                break;
        }
        viewModel.updateToolbar(viewId, title);

        if (viewFlipperMain != null) {
            viewFlipperMain.setDisplayedChild(whichChild - 1);
        }
    }

    /**
     * Возврат фрагмента в первоначальное состояние.
     */
    public void clearView() {
        viewModel.showMainView(Constants.MAIN_VIEW_NONE);
        lvRecords.setAdapter(null);
        lvAttaches.setAdapter(null);
        tvRecordsEmpty.setText(R.string.title_select_the_node);
        showGlobalSearchButton(false);
    }

    // region Records

    /**
     * Отображение списка записей.
     * @param records
     * @param viewId
     */
    public void showRecords(List<TetroidRecord> records, int viewId) {
        viewModel.showMainView(viewId);
        tvRecordsEmpty.setText((viewId == Constants.MAIN_VIEW_FAVORITES)
                ? R.string.title_favors_is_missing : R.string.title_records_is_missing);
        showGlobalSearchButton(false);
        listAdapterRecords.setDataItems(records, viewId);
        lvRecords.setAdapter(listAdapterRecords);
    }

    /**
     * Обновление списка записей.
     */
    public void updateRecordList() {
        if (listAdapterRecords == null) return;

        if ((viewModel.getCurMainViewId() == Constants.MAIN_VIEW_FAVORITES)) {
            listAdapterRecords.setDataItems(viewModel.getFavoriteRecords(), viewModel.getCurMainViewId());
        } else {
            listAdapterRecords.notifyDataSetInvalidated();
            // FIXME?
//            listAdapterRecords.notifyDataSetChanged();
        }
    }

    /**
     * Обработчик клика на записи
     */
    private AdapterView.OnItemClickListener onRecordClicklistener = (parent, view, position, id) -> showRecord(position);

    public void onRecordsFiltered(String query, List<TetroidRecord> found, int viewId) {
        if (found.isEmpty()) {
            String emptyText = (viewId == Constants.MAIN_VIEW_NODE_RECORDS)
                    ? String.format(getString(R.string.search_records_in_node_not_found_mask), query, viewModel.getCurNodeName())
                    : String.format(getString(R.string.search_records_in_tag_not_found_mask), query, viewModel.getCurTagName());
            setRecordsEmptyViewText(emptyText);
            showGlobalSearchButton(true);
        } else {
            showGlobalSearchButton(false);
        }
    }

    // endregion Records

    // region Record

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей
     */
    private void showRecord(int position) {
        TetroidRecord record = (TetroidRecord) listAdapterRecords.getItem(position);
        showRecord(record);
    }

    public void showRecord(TetroidRecord record) {
        viewModel.openRecord(record);
    }

    /**
     * Создание новой записи.
     */
    public void createRecord() {
        new RecordFieldsDialog(
                null,
                true,
                viewModel.getCurNode(),
                (name, tags, author, url, node, isFavor) -> {
                    viewModel.createRecord(name, tags, author, url, node, isFavor);
                }
        ).showIfPossible(getParentFragmentManager());
    }

    /**
     * Удаление записи.
     * @param record
     */
    private void deleteRecord(@NotNull TetroidRecord record) {
        RecordDialogs.deleteRecord(getContext(), record.getName(), () -> {
            viewModel.deleteRecord(record);
        });
    }

    /**
     * Обработка результата удаления записи.
     * @param record
     */
    public void onDeleteRecordResult(TetroidRecord record) {
        listAdapterRecords.getDataSet().remove(record);
        listAdapterRecords.notifyDataSetChanged();
        lvAttaches.setAdapter(null);
    }

    /**
     * Перемещение записи вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    private void reorderRecord(int pos, boolean isUp) {
        viewModel.reorderRecords(listAdapterRecords.getDataSet(), pos, isUp);
    }

    /**
     * Редактирование свойств записи.
     * @param record
     */
    private void editRecordFields(TetroidRecord record) {
        new RecordFieldsDialog(
                record,
                true,
                viewModel.getCurNode(),
                (name, tags, author, url, node, isFavor) -> {
                    viewModel.editRecordFields(record, name, tags, author, url, node, isFavor);
                }
        ).showIfPossible(getParentFragmentManager());
    }

    void showRecordInfoDialog(TetroidRecord record) {
        new RecordInfoDialog(
                record,
                viewModel.getStorageId()
        ).showIfPossible(getParentFragmentManager());
    }

    // endregion Record

    // region Attaches

    public void showAttaches(List<TetroidFile> files) {
        viewModel.showMainView(Constants.MAIN_VIEW_RECORD_FILES);
        this.listAdapterAttaches.reset(files);
        lvAttaches.setAdapter(listAdapterAttaches);
    }

    /**
     * Обработчик клика на иконке прикрепленных файлов записи
     */
    RecordsListAdapter.OnRecordAttachmentClickListener onRecordAttachmentClickListener = record -> viewModel.showRecordAttaches(record, false);

    /**
     * Обработчик клика на прикрепленном файле
     */
    private AdapterView.OnItemClickListener onFileClicklistener = (parent, view, position, id) -> openAttach(position);

    // endregion Attaches

    // region Attach

    /**
     * Открытие прикрепленного файла.
     * @param position Индекс файла в списке прикрепленных файлов записи
     */
    private void openAttach(int position) {
        TetroidFile attach = viewModel.getCurRecord().getAttachedFiles().get(position);
        viewModel.checkPermissionAndOpenAttach(attach);
    }

    /**
     * Перемещение файла вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    private void reorderAttach(int pos, boolean isUp) {
        viewModel.reorderAttaches(listAdapterAttaches.getDataSet(), pos, isUp);
    }

    /**
     * Удаление прикрепленного файла.
     * @param attach
     */
    private void deleteAttach(@NotNull TetroidFile attach) {
        AttachAskDialogs.deleteFile(getContext(), attach.getName(), () -> {
            viewModel.deleteAttach(attach);
        });
    }

    /**
     * Обработка результата удаления файла.
     * @param attach
     */
    public void onDeleteAttachResult(TetroidFile  attach) {
        listAdapterAttaches.getDataSet().remove(attach);
        updateAttachesList();
    }

    /**
     * Переименование прикрепленного файла.
     * @param attach
     */
    private void renameAttach(TetroidFile attach) {
        new AttachFieldsDialog(
                attach,
                (name) -> viewModel.renameAttach(attach, name)
        ).showIfPossible(getParentFragmentManager());
    }

    void showAttachInfoDialog(TetroidFile attach) {
        new AttachInfoDialog(
                attach
        ).showIfPossible(getParentFragmentManager());
    }

    public void updateAttachesList() {
        listAdapterAttaches.notifyDataSetChanged();
    }

    // endregion Attach

    // region OptionsMenu

    /**
     *
     * @param menu
     */
    public void onCreateOptionsMenu(Menu menu) {

    }

    private void visibleMenuItem(MenuItem menuItem, boolean isVisible) {
        ViewUtils.setVisibleIfNotNull(menuItem, isVisible);
    }

    /**
     *
     * @param menu
     */
    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        boolean isRecordFilesView = (viewModel.getCurMainViewId() == Constants.MAIN_VIEW_RECORD_FILES);
        boolean mIsFavoritesView = (viewModel.getCurMainViewId() == Constants.MAIN_VIEW_FAVORITES);
        activateMenuItem(menu.findItem(R.id.action_move_back), isRecordFilesView);
        activateMenuItem(menu.findItem(R.id.action_cur_record), isRecordFilesView);
        activateMenuItem(menu.findItem(R.id.action_cur_record_folder), isRecordFilesView);
        activateMenuItem(menu.findItem(R.id.action_insert),
                !mIsFavoritesView && TetroidClipboard.hasObject(FoundType.TYPE_RECORD));
    }

    /**
     * Обработчик пунктов меню фрагмента. Вызывается из аналогичного метода в классе активности.
     * @param menuId
     * @return
     */
    public boolean onOptionsItemSelected(int menuId) {
        switch (menuId) {
            case R.id.action_insert:
                viewModel.insertRecord();
                return true;
            case R.id.action_cur_record:
                showRecord(viewModel.getCurRecord());
                return true;
            case R.id.action_cur_record_folder:
                viewModel.openRecordFolder(viewModel.getCurRecord());
                return true;
        }
        return false;
    }

    private void activateMenuItem(MenuItem menuItem, boolean isActivate) {
        ViewUtils.setVisibleIfNotNull(menuItem, isActivate);
    }

    // endregion OptionsMenu

    // region ContextMenu

    /**
     * Обработчик создания контекстного меню при долгом тапе на записи.
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        int viewId = v.getId();
        MenuInflater inflater = getActivity().getMenuInflater();
        AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (viewId == R.id.list_view_records) {
            inflater.inflate(R.menu.record_context, menu);
            prepareRecordsContextMenu(menu, adapterMenuInfo);
        } else if (viewId == R.id.list_view_files) {
            inflater.inflate(R.menu.attach_context, menu);
            prepareFilesContextMenu(menu, adapterMenuInfo);
        }
    }

    /**
     * @param menu
     * @param menuInfo
     */
    private void prepareRecordsContextMenu(@NonNull Menu menu, AdapterView.AdapterContextMenuInfo menuInfo) {
        if (menuInfo == null)
            return;
        boolean isPro = App.INSTANCE.isFullVersion();
        boolean isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly();
        boolean isFavoritesView = (viewModel.getCurMainViewId() == Constants.MAIN_VIEW_FAVORITES);
        boolean isNonCrypted = false;
        TetroidRecord record = (TetroidRecord) listAdapterRecords.getItem(menuInfo.position);
        if (record != null) {
            isNonCrypted = record.isNonCryptedOrDecrypted();
            if (!isNonCrypted) {
                activateMenuItem(menu.findItem(R.id.action_copy), false);
                activateMenuItem(menu.findItem(R.id.action_cut), false);
                activateMenuItem(menu.findItem(R.id.action_attached_files), false);
                activateMenuItem(menu.findItem(R.id.action_open_record_folder), false);
                activateMenuItem(menu.findItem(R.id.action_copy_link), false);
                activateMenuItem(menu.findItem(R.id.action_info), false);
            }
            boolean isFavorite = record.isFavorite();
            activateMenuItem(menu.findItem(R.id.action_add_favorite), isPro && !isFavoritesView && !isFavorite);
            activateMenuItem(menu.findItem(R.id.action_remove_favorite), isPro && isFavoritesView || isFavorite);
        }
        activateMenuItem(menu.findItem(R.id.action_record_edit_fields), !isLoadedFavoritesOnly && isNonCrypted);
        activateMenuItem(menu.findItem(R.id.action_delete), !isLoadedFavoritesOnly);
        activateMenuItem(menu.findItem(R.id.action_record_node),
                !isLoadedFavoritesOnly && isFavoritesView && isNonCrypted);
        activateMenuItem(menu.findItem(R.id.action_insert),
                !isFavoritesView && TetroidClipboard.hasObject(FoundType.TYPE_RECORD));
        int recordsCount = listAdapterRecords.getCount();
        activateMenuItem(menu.findItem(R.id.action_move_up), recordsCount > 0);
        activateMenuItem(menu.findItem(R.id.action_move_down), recordsCount > 0);
    }

    /**
     * @param menu
     * @param menuInfo
     */
    private void prepareFilesContextMenu(@NonNull Menu menu, AdapterView.AdapterContextMenuInfo menuInfo) {
        if (menuInfo == null)
            return;
        boolean isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly();
        activateMenuItem(menu.findItem(R.id.action_rename), !isLoadedFavoritesOnly);
        int filesCount = listAdapterAttaches.getCount();
        activateMenuItem(menu.findItem(R.id.action_move_up), !isLoadedFavoritesOnly && filesCount > 0);
        activateMenuItem(menu.findItem(R.id.action_move_down), !isLoadedFavoritesOnly && filesCount > 0);
        TetroidFile file = (TetroidFile) listAdapterAttaches.getItem(menuInfo.position);
        activateMenuItem(menu.findItem(R.id.action_save_as), file != null
                && !TextUtils.isEmpty(viewModel.getAttachSize(file)));
        activateMenuItem(menu.findItem(R.id.action_delete), !isLoadedFavoritesOnly);
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи.
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int id = item.getItemId();
        int pos = info.position;
        if (info.targetView.getParent() == lvRecords) {
            return onContextRecordItemSelected(id, pos);
        } else if (info.targetView.getParent() == lvAttaches) {
            return onContextFileItemSelected(id, pos);
        }
        return super.onContextItemSelected(item);
    }

    private boolean onContextRecordItemSelected(int id, int pos) {
        TetroidRecord record = (TetroidRecord) listAdapterRecords.getItem(pos);
        if (record == null) {
            viewModel.logError(getString(R.string.log_get_item_is_null), true);
            return true;
        }
        switch (id) {
            case R.id.action_open_record:
                showRecord(record);
                return true;
            case R.id.action_record_edit_fields:
                editRecordFields(record);
                return true;
            case R.id.action_record_node:
                viewModel.showNode(record.getNode());
                return true;
            case R.id.action_copy:
                viewModel.copyRecord(record);
                return true;
            case R.id.action_cut:
                viewModel.cutRecord(record);
                return true;
            case R.id.action_insert:
                viewModel.insertRecord();
                return true;
            case R.id.action_attached_files:
                viewModel.showRecordAttaches(record, false);
                return true;
            case R.id.action_open_record_folder:
                viewModel.openRecordFolder(record);
                return true;
            case R.id.action_copy_link:
                viewModel.copyRecordLink(record);
                return true;
            case R.id.action_move_up:
                reorderRecord(pos, true);
                return true;
            case R.id.action_move_down:
                reorderRecord(pos, false);
                return true;
            case R.id.action_add_favorite:
                viewModel.addToFavorite(record);
                return true;
            case R.id.action_remove_favorite:
                viewModel.removeFromFavorite(record);
                return true;
            case R.id.action_info:
                showRecordInfoDialog(record);
                return true;
            case R.id.action_delete:
                deleteRecord(record);
                return true;
            default:
                return false;
        }
    }

    private boolean onContextFileItemSelected(int id, int pos) {
        TetroidFile attach = (TetroidFile) listAdapterAttaches.getItem(pos);
        if (attach == null) {
            viewModel.logError(getString(R.string.log_get_item_is_null), true);
            return true;
        }
        switch (id) {
            case R.id.action_open_file:
                openAttach(pos);
                return true;
            case R.id.action_rename:
                renameAttach(attach);
                return true;
            case R.id.action_copy_link:

                // TODO: реализовать ссылки на файлы
//                viewModel.copyAttachLink();
                return true;
            case R.id.action_save_as:
                viewModel.saveAttachOnDevice(attach);
                return true;
            case R.id.action_move_up:
                reorderAttach(pos, true);
                return true;
            case R.id.action_move_down:
                reorderAttach(pos, false);
                return true;
            case R.id.action_info:
                showAttachInfoDialog(attach);
                return true;
            case R.id.action_delete:
                deleteAttach(attach);
                return true;
            default:
                return false;
        }
    }

    // endregion ContextMenu


    /**
     * Обработчик нажатия кнопки Back
     */
    public boolean onBackPressed() {
        if (viewFlipperMain == null) {
            return false;
        }
        int curView = viewFlipperMain.getDisplayedChild() + 1;
        return viewModel.onMainViewBackPressed(curView);
    }

    public void setRecordsEmptyViewText(String s) {
        tvRecordsEmpty.setText(s);
    }

    public void showGlobalSearchButton(boolean vis) {
        btnUseGlobalSearch.setVisibility((vis) ? View.VISIBLE : View.GONE);
    }

    public void onAttachesFiltered(String query, List<TetroidFile> found) {
        if (found.isEmpty()) {
            tvFilesEmpty.setText(String.format(getString(R.string.search_files_not_found_mask), query));
        }
    }

    @NonNull
    @Override
    public String getTitle() {
        return "Главная";
    }
}

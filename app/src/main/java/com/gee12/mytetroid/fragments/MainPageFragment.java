package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.activities.TetroidActivity;
import com.gee12.mytetroid.adapters.FilesListAdapter;
import com.gee12.mytetroid.adapters.RecordsListAdapter;
import com.gee12.mytetroid.data.AttachesManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.FavoritesManager;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.dialogs.AttachDialogs;
import com.gee12.mytetroid.dialogs.FileDialogs;
import com.gee12.mytetroid.dialogs.RecordDialogs;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.helpers.UriHelper;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.Message;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MainPageFragment extends TetroidFragment {

    public static final int MAIN_VIEW_GLOBAL_FOUND = -1;
    public static final int MAIN_VIEW_NONE = 0;
    public static final int MAIN_VIEW_NODE_RECORDS = 1;
    public static final int MAIN_VIEW_RECORD_FILES = 2;
    public static final int MAIN_VIEW_TAG_RECORDS = 3;
    public static final int MAIN_VIEW_FAVORITES = 4;

    private ViewFlipper mViewFlipperfMain;
    private ListView mListViewRecords;
    private ListView mListViewFiles;
    private TextView mTextViewRecordsEmpty;
    private TextView mTextViewFilesEmpty;
    private FloatingActionButton mFabAddRecord;
    private com.github.clans.fab.FloatingActionMenu mFabAddAttach;
    private Button mUseGlobalSearchButton;

    private int mCurMainViewId;
    private int mLastViewId;
    private RecordsListAdapter mListAdapterRecords;
    private FilesListAdapter mListAdapterFiles;
    private TetroidRecord mCurRecord;
    private TetroidNode mCurNode;
    private TetroidFile mCurFile;
    private boolean mIsFromRecordActivity;


    public MainPageFragment(GestureDetectorCompat gestureDetector) {
        super(gestureDetector);
    }

    public MainPageFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        this.mViewFlipperfMain = view.findViewById(R.id.view_flipper_main);
        // обработка нажатия на пустом месте экрана, когда записей в ветке нет
        mViewFlipperfMain.setOnTouchListener(this);

        // список записей
        this.mListViewRecords = view.findViewById(R.id.list_view_records);
        // обработка нажатия на пустом месте списка записей
        mListViewRecords.setOnTouchListener(this);
        //
        mListViewRecords.setOnItemClickListener(onRecordClicklistener);
        this.mTextViewRecordsEmpty = view.findViewById(R.id.text_view_empty_records);
        this.mUseGlobalSearchButton = view.findViewById(R.id.button_global_search);
        mUseGlobalSearchButton.setOnClickListener(v -> mMainView.showGlobalSearchWithQuery());
        mListViewRecords.setEmptyView(mTextViewRecordsEmpty);
        // пустое пространство под списками
        View recordsFooter =  ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        mListViewRecords.addFooterView(recordsFooter, null, false);
        registerForContextMenu(mListViewRecords);
        /*mListViewRecords.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });*/
        // список файлов
        this.mListViewFiles = view.findViewById(R.id.list_view_files);
        // обработка нажатия на пустом месте списка файлов
        mListViewFiles.setOnTouchListener(this);
        mListViewFiles.setOnItemClickListener(onFileClicklistener);
        this.mTextViewFilesEmpty = view.findViewById(R.id.text_view_empty_files);
        mListViewFiles.setEmptyView(mTextViewFilesEmpty);
        // пустое пространство под списками
        View filesFooter =  ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);
        mListViewFiles.addFooterView(filesFooter, null, false);
        registerForContextMenu(mListViewFiles);

        this.mFabAddRecord = view.findViewById(R.id.button_add_record);
        mFabAddRecord.setOnClickListener(v -> createRecord());

        this.mFabAddAttach = view.findViewById(R.id.fab_add_attach);
        mFabAddAttach.setClosedOnTouchOutside(true);

        com.github.clans.fab.FloatingActionButton fabAddLocalFile = view.findViewById(R.id.fab_attach_local_file);
        fabAddLocalFile.setOnClickListener(v -> {
            mFabAddAttach.close(true);
            attachFile();
        });
        com.github.clans.fab.FloatingActionButton fabAttachFileByLink = view.findViewById(R.id.fab_attach_file_by_link);
        fabAttachFileByLink.setOnClickListener(v -> {
            mFabAddAttach.close(true);
            downloadAndAttachFile();
        });

        this.mCurMainViewId = MainPageFragment.MAIN_VIEW_NONE;
        setMainView(getArguments());
        // mMainView уже должен быть установлен
//        initRecordViews();
        // можно загружать настройки и хранилище
        mMainView.onMainPageCreated();

        return view;
    }

    public void initListAdapters(Context context) {
        // список записей
        if (mListViewRecords != null) {
            this.mListAdapterRecords = new RecordsListAdapter(context, onRecordAttachmentClickListener);
            mListViewRecords.setAdapter(mListAdapterRecords);
        }
        // список файлов
        if (mListViewFiles != null) {
            this.mListAdapterFiles = new FilesListAdapter(context);
            mListViewFiles.setAdapter(mListAdapterFiles);
        }
    }

    /**
     * Вызывается после инициализации настроек.
     */
    public void onSettingsInited() {
//        this.curRecordViewId = getDefaultRecordViewId();
//        updateFab(curRecordViewId);
    }

    /**
     *
     * @param viewId
     */
    public void showView(int viewId) {
        mFabAddRecord.hide();
        mFabAddAttach.hideMenu(true);
        // сохраняем значение для возврата на старое View
        // (только, если осуществляется переключение на действительно другую вьюшку)
        if (viewId != mCurMainViewId)
            this.mLastViewId = mCurMainViewId;
        int whichChild = viewId;
        String title = null;
        switch (viewId) {
            // для "очистки" активности выводим пустой список записей
            case MainPageFragment.MAIN_VIEW_NONE:
            // один контрол на записи ветки и метки
            case MainPageFragment.MAIN_VIEW_TAG_RECORDS:
                whichChild = MAIN_VIEW_NODE_RECORDS;
                break;
            case MAIN_VIEW_NODE_RECORDS:
                mFabAddRecord.show();
                break;
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
                if (!App.IsLoadedFavoritesOnly) {
                    mFabAddAttach.showMenu(true);
                }
                title = ((mCurRecord != null) ? mCurRecord.getName() : "");
                break;
        }
//        mMainView.updateMenuItems(viewId);
//        mMainView.checkKeepScreenOn(viewId);
        this.mCurMainViewId = viewId;
        mMainView.updateMainToolbar(viewId, title);

        if (mViewFlipperfMain != null) {
            mViewFlipperfMain.setDisplayedChild(whichChild - 1);
        }
    }

    /**
     * Восстанавливаем состояние Toolbar при переключении обратно к фрагменту MainPageFragment.
     */
    public void restoreLastMainToolbarState() {
        String title = null;
        int restoredViewId = mCurMainViewId;
        switch (restoredViewId) {
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
                title = ((mCurRecord != null) ? mCurRecord.getName() : "");
            break;
        }
        mMainView.updateMainToolbar(restoredViewId, title);
    }

    /**
     * Возврат фрагмента в первоначальное состояние.
     */
    public void clearView() {
        showView(MAIN_VIEW_NONE);
        this.mCurRecord = null;
        this.mCurNode = null;
        mListViewRecords.setAdapter(null);
        mListViewFiles.setAdapter(null);
        mTextViewRecordsEmpty.setText(R.string.title_select_the_node);
        showGlobalSearchButton(false);
    }

    // region Records

    /**
     * Отображение списка записей.
     * @param records
     * @param viewId
     */
    public void showRecords(List<TetroidRecord> records, int viewId) {
        showView(viewId);
        mTextViewRecordsEmpty.setText((viewId == MAIN_VIEW_FAVORITES)
                ? R.string.title_favors_is_missing : R.string.title_records_is_missing);
        showGlobalSearchButton(false);
        this.mListAdapterRecords.setDataItems(records, viewId);
        mListViewRecords.setAdapter(mListAdapterRecords);
    }

    /**
     * Обновление списка записей.
     */
    public void updateRecordList() {
        if (mListAdapterRecords != null) {
            mListAdapterRecords.notifyDataSetInvalidated();
        }
    }

    /**
     * Обработчик клика на записи
     */
    private AdapterView.OnItemClickListener onRecordClicklistener = (parent, view, position, id) -> showRecord(position);

    // endregion Records


    // region Record

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей
     */
    private void showRecord(int position) {
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(position);
        showRecord(record);
    }

    public void showCurRecord() {
        showRecord(mCurRecord);
    }

    public void showRecord(TetroidRecord record) {
        mMainView.openRecord(record);
    }

    /**
     * Создание новой записи.
     */
    public void createRecord() {
        RecordDialogs.createRecordFieldsDialog(mContext, null, true, mCurNode,
                (name, tags, author, url, node, isFavor) -> {
            TetroidRecord record = RecordsManager.createRecord(mContext, name, tags, author, url, mCurNode, isFavor);
            addNewRecord(record,true);
        });
    }

    public void addNewRecord(TetroidRecord record, boolean isShow) {
        if (record != null) {
            TetroidLog.logOperRes(getContext(), TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE, record, false);

            mListAdapterRecords.notifyDataSetInvalidated();
            mMainView.updateTags();
            mMainView.updateNodeList();
            updateFavorites(record);
            if (isShow) {
                showRecord(record);
            }
        } else {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);
        }
    }

    /**
     * Удаление записи.
     * @param record
     */
    private void deleteRecord(@NotNull TetroidRecord record) {
        RecordDialogs.deleteRecord(mContext, record.getName(), () -> {
            deleteRecordExactly(record);
        });
    }

    public void deleteRecordExactly(TetroidRecord record) {
        int res = RecordsManager.deleteRecord(mContext, record, false);
        if (res == -1) {
            RecordDialogs.operWithoutDir(mContext, TetroidLog.Opers.DELETE, () -> {
                int res1 = RecordsManager.deleteRecord(mContext, record, true);
                onDeleteRecordResult(record, res1, false);
            });
        } else {
            onDeleteRecordResult(record, res, false);
        }
    }

    /**
     * Обработка результата удаления записи.
     * @param record
     * @param res
     */
    private void onDeleteRecordResult(TetroidRecord record, int res, boolean isCutted) {
        if (res > 0) {
            mListAdapterRecords.getDataSet().remove(record);
            mListAdapterRecords.notifyDataSetChanged();
            mMainView.updateTags();
            mMainView.updateNodeList();
            // обновляем избранное
            updateFavorites(record);
            this.mCurRecord = null;
            mListViewFiles.setAdapter(null);
            TetroidLog.logOperRes(mContext, TetroidLog.Objs.RECORD, (isCutted) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);
            // переходим в список записей ветки после удаления
            // (запись может быть удалена при попытке просмотра/изменения файла, например)
            if (mCurMainViewId != MAIN_VIEW_NODE_RECORDS && mCurMainViewId != MAIN_VIEW_FAVORITES) {
                showView(MAIN_VIEW_NODE_RECORDS);
            }
        } else if (res == -2 && !isCutted) {
            LogManager.log(mContext, R.string.log_erorr_move_record_dir_when_del, ILogger.Types.WARNING, Toast.LENGTH_LONG);
            showSnackMoreInLogs();
        } else {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.RECORD, (isCutted) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);
        }
    }

    /**
     * Перемещение записи вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    private void moveRecord(Context context, int pos, boolean isUp) {
        int res = (mCurMainViewId == MAIN_VIEW_FAVORITES)
                ? FavoritesManager.swapRecords(context, pos, isUp, true)
                : DataManager.swapTetroidObjects(mContext, mListAdapterRecords.getDataSet(), pos, isUp, true);
        if (res > 0) {
            mListAdapterRecords.notifyDataSetChanged();
            TetroidLog.logOperRes(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.MOVE);
        } else if (res < 0) {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.MOVE);
        }
    }

    /**
     * Открытие каталога записи.
     * @param record
     */
    private void openRecordFolder(TetroidRecord record) {
        mMainView.openRecordFolder(record);
    }

    public void openRecordFolder() {
        if (mCurRecord != null) {
            mMainView.openRecordFolder(mCurRecord);
        }
    }

    /**
     * Редактирование свойств записи.
     * @param record
     */
    private void editRecordFields(TetroidRecord record) {
        TetroidNode oldNode = record.getNode();
        RecordDialogs.createRecordFieldsDialog(mContext, record, true, mCurNode,
                (name, tags, author, url, node, isFavor) -> {
            if (RecordsManager.editRecordFields(mContext, record, name, tags, author, url, node, isFavor)) {
                onRecordFieldsUpdated(record, oldNode != record.getNode());
//                TetroidLog.logOperRes(TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE);
                LogManager.log(mContext, R.string.log_record_fields_changed, ILogger.Types.INFO, Toast.LENGTH_SHORT);
            } else {
                TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.RECORD_FIELDS, TetroidLog.Opers.CHANGE);
            }
        });
    }

    /**
     * Обновление списка записей и меток после изменения свойств записи.
     */
    public void onRecordFieldsUpdated(TetroidRecord record, boolean nodeChanged) {
        if (nodeChanged) {
            mMainView.openNode(record.getNode());
        } else {
            mListAdapterRecords.notifyDataSetInvalidated();
        }
        mMainView.updateTags();
        updateFavorites(record);
    }

    /**
     * Копирование записи.
     * @param record
     */
    private void copyRecord(TetroidRecord record) {
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(record);
        TetroidLog.logOperRes(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.COPY);
    }

    /**
     * Вырезание записи из ветки.
     * @param record
     */
    private void cutRecord(TetroidRecord record) {
        // добавляем в "буфер обмена"
        TetroidClipboard.cut(record);
        // удаляем запись из текущей ветки и каталог перемещаем в корзину
        int res = RecordsManager.cutRecord(mContext, record, false);
        if (res == -1) {
            RecordDialogs.operWithoutDir(mContext, TetroidLog.Opers.CUT, () -> {
                // вставляем без каталога записи
                int res1 = RecordsManager.cutRecord(mContext, record, true);
                onDeleteRecordResult(record, res1, true);
            });
        } else {
            onDeleteRecordResult(record, res, true);
        }
    }

    /**
     * Вставка записи в ветку.
     */
    private void insertRecord() {
        // на всякий случай проверяем тип
        if (!TetroidClipboard.hasObject(FoundType.TYPE_RECORD))
            return;
        // достаем объект из "буфера обмена"
        TetroidClipboard clipboard = TetroidClipboard.getInstance();
        // вставляем с попыткой восстановить каталог записи
        TetroidRecord record = (TetroidRecord) clipboard.getObject();
        boolean isCutted = clipboard.isCutted();
        int res = RecordsManager.insertRecord(mContext, record, isCutted, mCurNode, false);
        if (res == -1) {
            RecordDialogs.operWithoutDir(mContext, TetroidLog.Opers.INSERT, () -> {
                // вставляем без каталога записи
                int res1 = RecordsManager.insertRecord(mContext, record, isCutted, mCurNode, true);
                onInsertRecordResult(record, res1, isCutted);
            });
        } else if (res == -2) {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.INSERT);
        } else {
            onInsertRecordResult(record, res, isCutted);
        }
    }

    private void onInsertRecordResult(TetroidRecord record, int res, boolean isCutted) {
        if (res > 0) {
            mListAdapterRecords.notifyDataSetInvalidated();
            mMainView.updateTags();
            mMainView.updateNodeList();
            TetroidLog.logOperRes(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);
            if (isCutted) {
                // очищаем "буфер обмена"
                TetroidClipboard.clear();
                // обновляем избранное
                updateFavorites(record);
            }
        } else {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);
        }
    }

    /**
     * Копирование ссылки на запись в буфер обмена.
     * @param record
     */
    private void copyRecordLink(TetroidRecord record) {
        if (record != null) {
            String url = record.createUrl();
            Utils.writeToClipboard(mContext, getString(R.string.link_to_record), url);
            LogManager.log(mContext, getString(R.string.title_link_was_copied) + url, ILogger.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.log(mContext, getString(R.string.log_get_item_is_null), ILogger.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    // endregion Record


    // region Attaches

    /**
     * Отображение списка прикрепленных файлов.
     * @param record Запись
     */
    public void showRecordAttaches(TetroidRecord record, boolean fromRecordActivity) {
        if (record == null)
            return;
        this.mCurRecord = record;
        this.mIsFromRecordActivity = fromRecordActivity;
        showRecordAttaches(record.getAttachedFiles());
    }

    public void showRecordAttaches(List<TetroidFile> files) {
        showView(MAIN_VIEW_RECORD_FILES);
        this.mListAdapterFiles.reset(files);
        mListViewFiles.setAdapter(mListAdapterFiles);
    }


    /**
     * Обработчик клика на иконке прикрепленных файлов записи
     */
    RecordsListAdapter.OnRecordAttachmentClickListener onRecordAttachmentClickListener = record -> showRecordAttaches(record, false);

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
        if (mCurRecord.isCrypted() && !SettingsManager.isDecryptFilesInTemp(mContext)) {
            LogManager.log(mContext, R.string.log_viewing_decrypted_not_possible, Toast.LENGTH_LONG);
            return;
        }
        TetroidFile file = mCurRecord.getAttachedFiles().get(position);
        mMainView.openAttach(file);
    }

    /**
     * Выбор файла на устройстве для прикрепления к записи.
     */
    private void attachFile() {
        mMainView.openFilePicker();
    }

    private void attachFile(Uri uri) {
        UriHelper uriHelper = new UriHelper(getContext());
        mMainView.attachFile(uriHelper.getPath(uri), true);
    }

    /**
     * Ввод URL для загрузки и прикрепления файла к записи.
     */
    private void downloadAndAttachFile() {
        AttachDialogs.createAttachFileByURLDialog(getContext(), url ->
                mMainView.downloadFileToCache(url, new TetroidActivity.IDownloadFileResult() {
                        @Override
                        public void onSuccess(Uri uri) {
                            attachFile(uri);
                        }
                        @Override
                        public void onError(Exception ex) {
                        }
                    }));
    }

    public void attachFile(TetroidFile file) {
        if (file != null) {
            mListAdapterFiles.notifyDataSetInvalidated();
            // обновляем список записей для обновления иконки о наличии прикрепляемых файлов у записи,
            // если был прикреплен первый файл
            if (mCurRecord.getAttachedFilesCount() == 1) {
                mListAdapterRecords.notifyDataSetInvalidated();
            }
            LogManager.log(mContext, getString(R.string.log_file_was_attached), ILogger.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.log(mContext, getString(R.string.log_file_attaching_error), ILogger.Types.ERROR, Toast.LENGTH_LONG);
            showSnackMoreInLogs();
        }
    }

    /**
     * Перемещение файла вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    private void moveFile(int pos, boolean isUp) {
        int res = DataManager.swapTetroidObjects(mContext, mListAdapterFiles.getDataSet(), pos, isUp, true);
        if (res > 0) {
            mListAdapterFiles.notifyDataSetChanged();
            TetroidLog.logOperRes(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE);
        } else if (res < 0) {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.MOVE);
        }
    }

    /**
     * Удаление прикрепленного файла.
     * @param file
     */
    private void deleteFile(@NotNull TetroidFile file) {
        FileDialogs.deleteFile(mContext, file.getName(), () -> {
            int res = AttachesManager.deleteAttachedFile(mContext, file, false);
            if (res == -2) {
                FileDialogs.deleteAttachWithoutFile(mContext, () -> {
                    int res1 = AttachesManager.deleteAttachedFile(mContext, file, true);
                    onDeleteFileResult(file, res1);
                });
            } else if (res == -1) {
                RecordDialogs.operWithoutDir(mContext, TetroidLog.Opers.DELETE, () -> {
                    int res1 = AttachesManager.deleteAttachedFile(mContext, file, true);
                    onDeleteFileResult(file, res1);
                });
            } else {
                onDeleteFileResult(file, res);
            }
        });
    }

    /**
     * Обработка результата удаления файла.
     * @param file
     * @param res
     */
    private void onDeleteFileResult(TetroidFile  file, int res) {
        if (res > 0) {
            mListAdapterFiles.getDataSet().remove(file);
            mListAdapterFiles.notifyDataSetChanged();
            // обновляем список записей для удаления иконки о наличии прикрепляемых файлов у записи,
            // если был удален единственный файл
            if (mCurRecord.getAttachedFilesCount() <= 0) {
                mListAdapterRecords.notifyDataSetInvalidated();
            }
            TetroidLog.logOperRes(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE);
        } else {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE);
        }
    }

    /**
     * Переименование прикрепленного файла.
     * @param file
     */
    private void renameFile(TetroidFile file) {
        FileDialogs.createFileDialog(mContext, file, (name) -> {
            int res = AttachesManager.editAttachedFileFields(mContext, file, name);
            if (res == -2) {
                // если файл отсутствует на диске, предлагаем его удалить из хранилища
                FileDialogs.renameAttachWithoutFile(mContext, () -> {
                    int res1 = AttachesManager.deleteAttachedFile(mContext, file, true);
                    onDeleteFileResult(file, res1);
                });
            } else if (res == -1) {
                // если каталог записи отсутствует на диске, предлагаем удалить запись из хранилища

                // TODO: добавить вариант Создать каталог записи

                FileDialogs.renameAttachWithoutDir(mContext, () -> {
                    int res1 = RecordsManager.deleteRecord(mContext, file.getRecord(), true);
                    onDeleteRecordResult(file.getRecord(), res1, false);
                });
            } else {
                onRenameFileResult(res);
            }
        });
    }

    /**
     * Обработка результата переименования файла.
     * @param res
     */
    private void onRenameFileResult(int res) {
        if (res > 0) {
            TetroidLog.logOperRes(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME);
            mListAdapterFiles.notifyDataSetChanged();
        } else {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME);
        }
    }

    private void saveFileAs(TetroidFile file) {
        this.mCurFile = file;
        mMainView.openFolderPicker();
    }

    public void onSaveFileResult(boolean res) {
        if (res) {
            TetroidLog.logOperRes(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.SAVE, "", Toast.LENGTH_SHORT);
        } else {
            TetroidLog.logOperErrorMore(mContext, TetroidLog.Objs.FILE, TetroidLog.Opers.SAVE);
        }
    }

    // endregion Attach


    // region Favorites

    public void addFavorite(Context context, TetroidRecord record) {
        if (FavoritesManager.add(context, record)) {
            String mes = "Добавлено в избранное";
            Message.show(mContext, mes, Toast.LENGTH_SHORT);
            LogManager.log(mContext, mes + ": " + TetroidLog.getIdString(mContext, record), ILogger.Types.INFO, -1);
            updateFavorites(record);
        } else {
            TetroidLog.logOperError(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.ADD,
                    String.format(" с id=%s в избранное", record.getId()), true, Toast.LENGTH_LONG);
        }
    }

    public void removeFavorite(Context context, TetroidRecord record) {
        if (FavoritesManager.remove(context, record, true)) {
            String mes = "Удалено из избранного";
            Message.show(mContext, mes, Toast.LENGTH_SHORT);
            LogManager.log(mContext, mes + ": " + TetroidLog.getIdString(mContext, record), ILogger.Types.INFO, -1);
            mMainView.updateFavorites();
            mListAdapterRecords.notifyDataSetInvalidated();
        } else {
            TetroidLog.logOperError(mContext, TetroidLog.Objs.RECORD, TetroidLog.Opers.DELETE,
                    String.format(" с id=%s из избранного", record.getId()), true, Toast.LENGTH_LONG);
        }
    }

    private void updateFavorites(TetroidRecord record) {
        if (record == null || !record.isFavorite())
            return;
        mMainView.updateFavorites();
        mListAdapterRecords.notifyDataSetInvalidated();
    }

    // endregion Favorites


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
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        boolean isRecordFilesView = (mCurMainViewId == MAIN_VIEW_RECORD_FILES);
        boolean mIsFavoritesView = (mCurMainViewId == MAIN_VIEW_FAVORITES);
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
                insertRecord();
                return true;
            case R.id.action_cur_record:
                showCurRecord();
                return true;
            case R.id.action_cur_record_folder:
                openRecordFolder();
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
        boolean isPro = App.isFullVersion();
        boolean isLoadedFavoritesOnly = App.IsLoadedFavoritesOnly;
        boolean isFavoritesView = (mCurMainViewId == MAIN_VIEW_FAVORITES);
        boolean isNonCrypted = false;
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(menuInfo.position);
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
        int recordsCount = mListAdapterRecords.getCount();
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
        boolean isLoadedFavoritesOnly = App.IsLoadedFavoritesOnly;
        activateMenuItem(menu.findItem(R.id.action_rename), !isLoadedFavoritesOnly);
        int filesCount = mListAdapterFiles.getCount();
        activateMenuItem(menu.findItem(R.id.action_move_up), !isLoadedFavoritesOnly && filesCount > 0);
        activateMenuItem(menu.findItem(R.id.action_move_down), !isLoadedFavoritesOnly && filesCount > 0);
        TetroidFile file = (TetroidFile) mListAdapterFiles.getItem(menuInfo.position);
        activateMenuItem(menu.findItem(R.id.action_save_as), file != null
                && AttachesManager.getAttachedFileSize(mContext, file) != null);
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
        if (info.targetView.getParent() == mListViewRecords) {
            return onContextRecordItemSelected(id, pos);
        } else if (info.targetView.getParent() == mListViewFiles) {
            return onContextFileItemSelected(id, pos);
        }
        return super.onContextItemSelected(item);
    }

    private boolean onContextRecordItemSelected(int id, int pos) {
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(pos);
        if (record == null) {
            LogManager.log(mContext, getString(R.string.log_get_item_is_null), ILogger.Types.ERROR, Toast.LENGTH_LONG);
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
                mMainView.openNode(record.getNode());
                return true;
            case R.id.action_copy:
                copyRecord(record);
                return true;
            case R.id.action_cut:
                cutRecord(record);
                return true;
            case R.id.action_insert:
                insertRecord();
                return true;
            case R.id.action_attached_files:
                showRecordAttaches(record, false);
                return true;
            case R.id.action_open_record_folder:
                openRecordFolder(record);
                return true;
            case R.id.action_copy_link:
                copyRecordLink(record);
                return true;
            case R.id.action_move_up:
                moveRecord(mContext, pos, true);
                return true;
            case R.id.action_move_down:
                moveRecord(mContext, pos, false);
                return true;
            case R.id.action_add_favorite:
                addFavorite(mContext, record);
                return true;
            case R.id.action_remove_favorite:
                removeFavorite(mContext, record);
                return true;
            case R.id.action_info:
                RecordDialogs.createRecordInfoDialog(mContext, record);
                return true;
            case R.id.action_delete:
                deleteRecord(record);
                return true;
            default:
                return false;
        }
    }

    private boolean onContextFileItemSelected(int id, int pos) {
        TetroidFile attach = (TetroidFile) mListAdapterFiles.getItem(pos);
        if (attach == null) {
            LogManager.log(mContext, getString(R.string.log_get_item_is_null), ILogger.Types.ERROR, Toast.LENGTH_LONG);
            return true;
        }
        switch (id) {
            case R.id.action_open_file:
                openAttach(pos);
                return true;
            case R.id.action_rename:
                renameFile(attach);
                return true;
            case R.id.action_copy_link:

                // TODO: реализовать ссылки на файлы

                return true;
            case R.id.action_save_as:
                saveFileAs(attach);
                return true;
            case R.id.action_move_up:
                moveFile(pos, true);
                return true;
            case R.id.action_move_down:
                moveFile(pos, false);
                return true;
            case R.id.action_info:
                AttachDialogs.createAttachInfoDialog(mContext, attach);
                return true;
            case R.id.action_delete:
                deleteFile(attach);
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
        if (mViewFlipperfMain == null) {
            return false;
        }
        boolean res = false;
        int curView = mViewFlipperfMain.getDisplayedChild() + 1;
        if (curView == MAIN_VIEW_RECORD_FILES) {
            res = true;
            switch (mLastViewId) {
                case MAIN_VIEW_NODE_RECORDS:
                case MAIN_VIEW_TAG_RECORDS:
                case MAIN_VIEW_FAVORITES:
//                case VIEW_FOUND_RECORDS:
                    showView(mLastViewId);
                    break;
                default:
                    showView(MAIN_VIEW_NONE);
            }
        }
        return res;
    }

    public void setRecordsEmptyViewText(String s) {
        mTextViewRecordsEmpty.setText(s);
    }

    public void showGlobalSearchButton(boolean vis) {
        mUseGlobalSearchButton.setVisibility((vis) ? View.VISIBLE : View.GONE);
    }

    public void setFilesEmptyViewText(String s) {
        mTextViewFilesEmpty.setText(s);
    }

    public void setCurNode(TetroidNode curNode) {
        this.mCurNode = curNode;
    }

    public TetroidRecord getCurRecord() {
        return mCurRecord;
    }

    public TetroidFile getCurFile() {
        return mCurFile;
    }

    public int getCurMainViewId() {
        return mCurMainViewId;
    }

    public int getLastViewId() {
        return mLastViewId;
    }

    public boolean isFromRecordActivity() {
        return mIsFromRecordActivity;
    }

    public void dropIsFromRecordActivity() {
        mIsFromRecordActivity = false;
    }

    @Override
    public String getTitle() {
        return "Главная";
    }
}

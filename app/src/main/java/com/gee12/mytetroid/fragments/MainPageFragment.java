package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.adapters.FilesListAdapter;
import com.gee12.mytetroid.adapters.RecordsListAdapter;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidClipboard;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.views.FileAskDialogs;
import com.gee12.mytetroid.views.RecordAskDialogs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainPageFragment extends TetroidFragment {

    public static final int MAIN_VIEW_GLOBAL_FOUND = -1;
    public static final int MAIN_VIEW_NONE = 0;
    public static final int MAIN_VIEW_NODE_RECORDS = 1;
    public static final int MAIN_VIEW_RECORD_FILES = 2;
    public static final int MAIN_VIEW_TAG_RECORDS = 3;

    private ViewFlipper mViewFlipperfMain;
    private ListView mListViewRecords;
    private ListView mListViewFiles;
    private TextView mTextViewRecordsEmpty;
    private TextView mTextViewFilesEmpty;
    private MenuItem mMenuItemCurNode;
    private MenuItem mMenuItemCurRecord;
    private MenuItem mMenuItemCurRecordFolder;
    private FloatingActionButton mButtonAddRecord;
    private FloatingActionButton mButtonAddFile;

    private int mCurMainViewId;
    private int mLastViewId;
    private RecordsListAdapter mListAdapterRecords;
    private FilesListAdapter mListAdapterFiles;
    private TetroidRecord mCurRecord;
    private TetroidNode mCurNode;


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
        mListViewRecords.setEmptyView(mTextViewRecordsEmpty);
        registerForContextMenu(mListViewRecords);
        mListViewRecords.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
        // список файлов
        this.mListViewFiles = view.findViewById(R.id.list_view_files);
        // обработка нажатия на пустом месте списка файлов
        mListViewFiles.setOnTouchListener(this);
        mListViewFiles.setOnItemClickListener(onFileClicklistener);
        this.mTextViewFilesEmpty = view.findViewById(R.id.text_view_empty_files);
        mListViewFiles.setEmptyView(mTextViewFilesEmpty);
        registerForContextMenu(mListViewFiles);

        this.mButtonAddRecord = view.findViewById(R.id.button_add_record);
//        mButtonAddRecord.setAlpha(0.1f);
        mButtonAddRecord.setOnClickListener(v -> createRecord());
        this.mButtonAddFile = view.findViewById(R.id.button_add_file);
        mButtonAddFile.setOnClickListener(v -> attachFile());

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
        mMenuItemCurNode.setVisible(false);
        mMenuItemCurRecord.setVisible(false);
        mMenuItemCurRecordFolder.setVisible(false);
        mButtonAddRecord.hide();
        mButtonAddFile.hide();
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
                mButtonAddRecord.show();
                break;
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
                mMenuItemCurNode.setVisible(true);
                mMenuItemCurRecordFolder.setVisible(true);
                mMenuItemCurRecord.setVisible(true);
                mButtonAddFile.show();
                title = ((mCurRecord != null) ? mCurRecord.getName() : "");
                break;
        }
        mMainView.updateMainToolbar(viewId, title);
//        mMainView.updateMenuItems(viewId);
//        mMainView.checkKeepScreenOn(viewId);
        this.mCurMainViewId = viewId;
        mViewFlipperfMain.setDisplayedChild(whichChild-1);
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
        mTextViewRecordsEmpty.setText(R.string.select_the_node);
    }

    /**
     * Отображение списка записей.
     * @param records
     * @param viewId
     */
    public void showRecords(List<TetroidRecord> records, int viewId) {
        String dateTimeFormat = checkDateFormatString();
        showView(viewId);
        mTextViewRecordsEmpty.setText(R.string.records_is_missing);
        this.mListAdapterRecords.setDataItems(records, viewId, dateTimeFormat);
        mListViewRecords.setAdapter(mListAdapterRecords);
    }

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
        RecordAskDialogs.createRecordFieldsDialog(getContext(), null, (name, tags, author, url) -> {
            TetroidRecord record = DataManager.createRecord(name, tags, author, url, mCurNode);
            if (record != null) {
                mListAdapterRecords.notifyDataSetInvalidated();
                mMainView.updateTags();
                mMainView.updateNodes();
                showRecord(record);
            } else {
//                LogManager.addLog(getString(R.string.log_record_create_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                TetroidLog.addOperErrorLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.CREATE);
            }
        });
    }

    /**
     * Удаление записи.
     * @param record
     */
    private void deleteRecord(TetroidRecord record) {
        RecordAskDialogs.deleteRecord(getContext(), () -> {
            int res = DataManager.deleteRecord(record, false);
            if (res == -1) {
                RecordAskDialogs.operWithoutDir(getContext(), TetroidLog.Opers.DELETE, () -> {
                    int res1 = DataManager.deleteRecord(record, true);
                    onDeleteRecordResult(record, res1, false);
                });
            } else {
                onDeleteRecordResult(record, res, false);
            }
        });
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
            mMainView.updateNodes();
            this.mCurRecord = null;
            mListViewFiles.setAdapter(null);
//            String s = getString(R.string.oper_res_record) + getString((isCutted) ? R.string.cutted : R.string.deleted);
//            LogManager.addLog(s, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            TetroidLog.addOperResLog(TetroidLog.Objs.RECORD, (isCutted) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);
            // переходим в список записей ветки после удаления
            // (запись может быть удалена при поппытке просмотра/изменения файла, например)
            if (mCurMainViewId != MAIN_VIEW_NODE_RECORDS) {
                showView(MAIN_VIEW_NODE_RECORDS);
            }
        } else if (res == -2 && !isCutted) {
            LogManager.addLog(R.string.log_erorr_move_record_dir_when_del, LogManager.Types.WARNING, Toast.LENGTH_LONG);
        } else {
//            String s = getString(R.string.oper_res_record) + getString((isCutted) ? R.string.cut : R.string.delete);
//            LogManager.addLog(getString(R.string.log_oper_error_mask), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.addOperErrorLog(TetroidLog.Objs.RECORD, (isCutted) ? TetroidLog.Opers.CUT : TetroidLog.Opers.DELETE);
        }
    }

    /**
     * Перемещение записи вверх/вниз по списку.
     * @param pos
     * @param isUp
     */
    private void moveRecord(int pos, boolean isUp) {
        int res = DataManager.swapTetroidObjects(mListAdapterRecords.getDataSet(), pos, isUp);
        if (res > 0) {
            mListAdapterRecords.notifyDataSetChanged();
//            String s = getString(R.string.oper_res_record) + getString(R.string.moved);
//            LogManager.addLog(s, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            TetroidLog.addOperResLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.MOVE);
        } else if (res < 0) {
//            LogManager.addLog(getString(R.string.log_record_move_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.addOperErrorLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.MOVE);
        }
    }

    /**
     * Отображение списка прикрепленных файлов.
     * @param record Запись
     */
    public void showRecordFiles(TetroidRecord record) {
        if (record == null)
            return;
        this.mCurRecord = record;
        showRecordFiles(record.getAttachedFiles());
    }

    public void showRecordFiles(List<TetroidFile> files) {
        showView(MAIN_VIEW_RECORD_FILES);
        this.mListAdapterFiles.reset(files);
        mListViewFiles.setAdapter(mListAdapterFiles);
    }

    /**
     * Открытие прикрепленного файла.
     * @param position Индекс файла в списке прикрепленных файлов записи
     */
    private void openFile(int position) {
        if (mCurRecord.isCrypted() && !SettingsManager.isDecryptFilesInTemp()) {
            LogManager.addLog(R.string.log_viewing_decrypted_not_possible, Toast.LENGTH_LONG);
            return;
        }
        TetroidFile file = mCurRecord.getAttachedFiles().get(position);
        mMainView.openAttach(file);
    }

    /**
     * Выбор файла для прикрепления к записи.
     */
    private void attachFile() {
        mMainView.openFilePicker();
    }

    /**
     * Прикрепление выбранного файла к записи.
     * @param fullName
     */
    public void attachFile(String fullName) {
        TetroidFile file = DataManager.attachFile(fullName, mCurRecord);
        if (file != null) {
            mListAdapterFiles.notifyDataSetInvalidated();
            // обновляем список записей для обновления иконки о наличии прикрепляемых файлов у записи,
            // если был прикреплен первый файл
            if (mCurRecord.getAttachedFilesCount() == 1) {
                mListAdapterRecords.notifyDataSetInvalidated();
            }
            LogManager.addLog(getString(R.string.log_file_was_attached), LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.addLog(getString(R.string.log_file_attaching_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
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
        RecordAskDialogs.createRecordFieldsDialog(getContext(), record, (name, tags, author, url) -> {
            if (DataManager.editRecordFields(record, name, tags, author, url)) {
                onRecordFieldsUpdated();
//                LogManager.addLog(getString(R.string.log_record_fields_edited), LogManager.Types.INFO, Toast.LENGTH_SHORT);
                TetroidLog.addOperResLog(TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE);
            } else {
//                LogManager.addLog(getString(R.string.log_record_edit_fields_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                TetroidLog.addOperErrorLog(TetroidLog.Objs.FILE_FIELDS, TetroidLog.Opers.CHANGE);
            }
        });
    }

    /**
     * Обновление списка записей и меток после изменения свойств записи.
     */
    public void onRecordFieldsUpdated() {
        mListAdapterRecords.notifyDataSetInvalidated();
        mMainView.updateTags();
    }

    /**
     * Копирование записи.
     * @param record
     */
    private void copyRecord(TetroidRecord record) {
        // добавляем в "буфер обмена"
        TetroidClipboard.copy(record);
    }

    /**
     * Вырезание записи из ветки.
     * @param record
     */
    private void cutRecord(TetroidRecord record) {
        // добавляем в "буфер обмена"
        TetroidClipboard.cut(record);
        // удаляем запись из текущей ветки и каталог перемещаем в корзину
        int res = DataManager.cutRecord(record, false);
        if (res == -1) {
            RecordAskDialogs.operWithoutDir(getContext(), TetroidLog.Opers.CUT, () -> {
                // вставляем без каталога записи
                int res1 = DataManager.cutRecord(record, true);
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
        // достаем запись из "буфера обмена"
        TetroidClipboard clipboard = TetroidClipboard.getInstance();
        // вставляем с попыткой восстановить каталог записи
//        int res = DataManager.insertRecord(clipboard, mCurNode, false);
        TetroidRecord record = (TetroidRecord) clipboard.getObject();
        boolean isCutted = clipboard.isCutted();
        int res = DataManager.insertRecord(record, isCutted, mCurNode, false);
        if (res == -1) {
            RecordAskDialogs.operWithoutDir(getContext(), TetroidLog.Opers.INSERT, () -> {
                // вставляем без каталога записи
//                int res1 = DataManager.insertRecord(clipboard, mCurNode, true);
                int res1 = DataManager.insertRecord(record, isCutted, mCurNode, true);
                onInsertRecordResult(res1, isCutted);
            });
        } else if (res == -2) {
//            LogManager.addLog(R.string.log_error_move_record_catalog, LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.addOperErrorLog(TetroidLog.Objs.RECORD_DIR, TetroidLog.Opers.MOVE);
        } else {
            onInsertRecordResult(res, isCutted);
        }
    }

    private void onInsertRecordResult(/*TetroidRecord record, */int res, boolean isCutted) {
//        TetroidRecord record = (TetroidRecord) TetroidClipboard.getObjectForInsert();
        if (res > 0/* && record != null*/) {
            mListAdapterRecords.notifyDataSetInvalidated();
            mMainView.updateTags();
            mMainView.updateNodes();
            TetroidLog.addOperResLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);
            if (isCutted) {
                // очищаем "буфер обмена"
                TetroidClipboard.clear();
            }
        } else {
            TetroidLog.addOperErrorLog(TetroidLog.Objs.RECORD, TetroidLog.Opers.INSERT);
        }
    }

    /**
     * Копирование ссылки на запись в буфер обмена.
     * @param record
     */
    private void copyRecordLink(TetroidRecord record) {
        if (record != null) {
            String url = record.createUrl();
            Utils.writeToClipboard(getContext(), getString(R.string.link_to_record), url);
            LogManager.addLog(getString(R.string.link_was_copied) + url, LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Удаление прикрепленного файла.
     * @param file
     */
    private void deleteFile(TetroidFile file) {
        FileAskDialogs.deleteFile(getContext(), () -> {
            int res = DataManager.deleteFile(file, false);
            if (res == -2) {
                FileAskDialogs.deleteAttachWithoutFile(getContext(), () -> {
                    int res1 = DataManager.deleteFile(file, true);
                    onDeleteFileResult(file, res1);
                });
            } else if (res == -1) {
                RecordAskDialogs.operWithoutDir(getContext(), TetroidLog.Opers.DELETE, () -> {
                    int res1 = DataManager.deleteFile(file, true);
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
//            String s = getString(R.string.oper_res_file) + getString(R.string.deleted);
//            LogManager.addLog(s, LogManager.Types.INFO, Toast.LENGTH_SHORT);
            TetroidLog.addOperResLog(TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE);
        } else {
//            LogManager.addLog(getString(R.string.log_file_delete_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.addOperErrorLog(TetroidLog.Objs.FILE, TetroidLog.Opers.DELETE);
        }
    }

    /**
     * Переименование прикрепленного файла.
     * @param file
     */
    private void renameFile(TetroidFile file) {
        FileAskDialogs.createFileDialog(getContext(), file, (name) -> {
            int res = DataManager.editFileFields(file, name);
            if (res == -2) {
                // если файл отсутствует на диске, предлагаем его удалить из хранилища
                FileAskDialogs.renameAttachWithoutFile(getContext(), () -> {
                    int res1 = DataManager.deleteFile(file, true);
                    onDeleteFileResult(file, res1);
                });
            } else if (res == -1) {
                // если каталог записи отсутствует на диске, предлагаем удалить запись из хранилища

                // TODO: добавить вариант Создать каталог записи

                FileAskDialogs.renameAttachWithoutDir(getContext(), () -> {
                    int res1 = DataManager.deleteRecord(file.getRecord(), true);
                    onDeleteRecordResult(file.getRecord(), res1, false);
                });
            } else {
                onRenameFileResult(file, res);
            }
        });
    }

    /**
     * Обработка результата переименования файла.
     * @param file
     * @param res
     */
    private void onRenameFileResult(TetroidFile  file, int res) {
        if (res > 0) {
//            LogManager.addLog(getString(R.string.file_was_renamed), LogManager.Types.INFO, Toast.LENGTH_SHORT);
            TetroidLog.addOperResLog(TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME);
            mListAdapterFiles.notifyDataSetChanged();
        } else {
//            LogManager.addLog(getString(R.string.log_file_edit_fields_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            TetroidLog.addOperErrorLog(TetroidLog.Objs.FILE, TetroidLog.Opers.RENAME);
        }
    }

    /**
     * Проверяем строку формата даты/времени, т.к. в версия приложения <= 11
     * введенная строка в настройках не проверялась, что могло привести к падению приложения
     * при отображении списка.
     * @return
     */
    private String checkDateFormatString() {
        String dateFormatString = SettingsManager.getDateFormatString();
        if (Utils.checkDateFormatString(dateFormatString)) {
            return dateFormatString;
        } else {
            LogManager.addLog(getString(R.string.log_incorrect_dateformat_in_settings), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            return getContext().getString(R.string.def_date_format_string);
        }
    }

    /**
     * Обработчик клика на записи
     */
    private AdapterView.OnItemClickListener onRecordClicklistener = (parent, view, position, id) -> showRecord(position);

    /**
     * Обработчик клика на иконке прикрепленных файлов записи
     */
    RecordsListAdapter.OnRecordAttachmentClickListener onRecordAttachmentClickListener = record -> showRecordFiles(record);

    /**
     * Обработчик клика на прикрепленном файле
     */
    private AdapterView.OnItemClickListener onFileClicklistener = (parent, view, position, id) -> openFile(position);

    public void onCreateOptionsMenu(@NonNull Menu menu) {
        this.mMenuItemCurNode = menu.findItem(R.id.action_cur_node);
        this.mMenuItemCurRecord = menu.findItem(R.id.action_cur_record);
        this.mMenuItemCurRecordFolder = menu.findItem(R.id.action_cur_record_folder);
    }

    public boolean onOptionsItemSelected(int menuId) {
        switch (menuId) {
            case R.id.action_cur_record:
                showCurRecord();
                return true;
            case R.id.action_cur_record_folder:
                openRecordFolder();
                return true;
        }
        return false;
    }

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
            inflater.inflate(R.menu.file_context, menu);
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
        activateMenuItem(menu.findItem(R.id.action_insert), TetroidClipboard.checkType(FoundType.TYPE_RECORD));
        activateMenuItem(menu.findItem(R.id.action_move_up), menuInfo.position > 0);
        activateMenuItem(menu.findItem(R.id.action_move_down), menuInfo.position < mListAdapterRecords.getCount() - 1);
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(menuInfo.position);
        if (record != null) {
            activateMenuItem(menu.findItem(R.id.action_attached_files), record.getAttachedFilesCount() > 0);
        }
    }

    /**
     * @param menu
     * @param menuInfo
     */
    private void prepareFilesContextMenu(@NonNull Menu menu, AdapterView.AdapterContextMenuInfo menuInfo) {
        if (menuInfo == null)
            return;
        activateMenuItem(menu.findItem(R.id.action_move_up), menuInfo.position > 0);
        activateMenuItem(menu.findItem(R.id.action_move_down), menuInfo.position < mListAdapterFiles.getCount() - 1);
    }

    private void activateMenuItem(MenuItem menuItem, boolean isActivate) {
//        menuItem.setEnabled(isActivate);
//        menuItem.getIcon().setAlpha((isActivate) ? 255 : 130);
        menuItem.setVisible(isActivate);
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
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return true;
        }
        switch (id) {
            case R.id.action_open_record:
                showRecord(record);
                return true;
            case R.id.action_record_edit_fields:
                editRecordFields(record);
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
                showRecordFiles(record);
                return true;
            case R.id.action_open_record_folder:
                openRecordFolder(record);
                return true;
            case R.id.action_copy_link:
                copyRecordLink(record);
                return true;
            case R.id.action_move_up:
                moveRecord(pos, true);
                return true;
            case R.id.action_move_down:
                moveRecord(pos, false);
                return true;
            case R.id.action_delete:
                deleteRecord(record);
                return true;
            default:
                return false;
        }
    }

    private boolean onContextFileItemSelected(int id, int pos) {
        TetroidFile file = (TetroidFile) mListAdapterFiles.getItem(pos);
        if (file == null) {
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return true;
        }
        switch (id) {
            case R.id.action_open_file:
                openFile(pos);
                return true;
            case R.id.action_rename:
                renameFile(file);
                return true;
            case R.id.action_copy_link:

                // TODO:

                return true;
            case R.id.action_move_up:

                // TODO:

                return true;
            case R.id.action_move_down:

                // TODO:

                return true;
            case R.id.action_delete:
                deleteFile(file);
                return true;
            default:
                return false;
        }
    }

    /**
     * Обработчик нажатия кнопки Back
     */
    public boolean onBackPressed() {
        boolean res = false;
        int curView = mViewFlipperfMain.getDisplayedChild() + 1;
        if (curView == MAIN_VIEW_RECORD_FILES) {
            res = true;
            switch (mLastViewId) {
                case MAIN_VIEW_NODE_RECORDS:
                case MAIN_VIEW_TAG_RECORDS:
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

    public void setFilesEmptyViewText(String s) {
        mTextViewFilesEmpty.setText(s);
    }

    public void setCurNode(TetroidNode curNode) {
        this.mCurNode = curNode;
    }

    public TetroidRecord getCurRecord() {
        return mCurRecord;
    }

    public int getCurMainViewId() {
        return mCurMainViewId;
    }

    public int getLastViewId() {
        return mLastViewId;
    }

    @Override
    public String getTitle() {
        return "Главная";
    }
}

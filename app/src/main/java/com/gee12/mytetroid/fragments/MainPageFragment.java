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

import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.adapters.FilesListAdapter;
import com.gee12.mytetroid.adapters.RecordsListAdapter;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.views.RecordAskDialogs;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Collections;
import java.util.List;

public class MainPageFragment extends TetroidFragment {

    public static final int MAIN_VIEW_GLOBAL_FOUND = -1;
    public static final int MAIN_VIEW_NONE = 0;
    public static final int MAIN_VIEW_NODE_RECORDS = 1;
//    public static final int MAIN_VIEW_RECORD_TEXT = 2;
    public static final int MAIN_VIEW_RECORD_FILES = 2;
    public static final int MAIN_VIEW_TAG_RECORDS = 3;
//    public static final int VIEW_FOUND_RECORDS = 5;

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
        // список файлов
        this.mListViewFiles = view.findViewById(R.id.list_view_files);
        // обработка нажатия на пустом месте списка файлов
        mListViewFiles.setOnTouchListener(this);
        mListViewFiles.setOnItemClickListener(onFileClicklistener);
        this.mTextViewFilesEmpty = view.findViewById(R.id.text_view_empty_files);
        mListViewFiles.setEmptyView(mTextViewFilesEmpty);

        this.mButtonAddRecord = view.findViewById(R.id.button_add_record);
//        mButtonAddRecord.setAlpha(0.1f);
        mButtonAddRecord.setOnClickListener(v -> createRecord());
        this.mButtonAddFile = view.findViewById(R.id.button_add_file);
        mButtonAddFile.setOnClickListener(v -> createFile());

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

    public void clearView() {
        showView(MAIN_VIEW_NONE);
        this.mCurRecord = null;
        mTextViewRecordsEmpty.setText(R.string.select_the_node);
        mListViewRecords.setAdapter(null);
        mListViewFiles.setAdapter(null);
    }

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
                showRecord(record);
            } else {
                LogManager.addLog(getString(R.string.log_record_create_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            }
        });
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
     * Отображение списка прикрепленных файлов.
     * @param position Индекс записи в списке записей ветки
     */
    private void showRecordFiles(int position) {
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(position);
        showRecordFiles(record);
    }

    /**
     * Отображение списка прикрепленных файлов.
     * @param record Запись
     */
    public void showRecordFiles(TetroidRecord record) {
        if (record == null)
            return;
        this.mCurRecord = record;
        showRecordFiles(record.getAttachedFiles(), record);
    }

    public void showRecordFiles(List<TetroidFile> files, TetroidRecord record) {
        showView(MAIN_VIEW_RECORD_FILES);
        this.mListAdapterFiles.reset(files, record);
        mListViewFiles.setAdapter(mListAdapterFiles);
    }

    /**
     * Открытие прикрепленного файла
     * @param position Индекс файла в списке прикрепленных файлов записи
     */
    private void openFile(int position) {
        if (mCurRecord.isCrypted() && !SettingsManager.isDecryptFilesInTemp()) {
            LogManager.addLog(R.string.log_viewing_decrypted_not_possible, Toast.LENGTH_LONG);
            return;
        }
        TetroidFile file = mCurRecord.getAttachedFiles().get(position);
        mMainView.openFile(file);
    }

    /**
     * Прикрепление нового файла к записи.
     */
    public void createFile() {

        // TODO:
        if (!BuildConfig.DEBUG) {
            LogManager.addLog(getString(R.string.debug_not_implemented_yet), LogManager.Types.INFO, Toast.LENGTH_SHORT);
            return;
        }

    }

    /**
     * Открытие каталога записи.
     * @param record
     */
    private void openRecordFolder(TetroidRecord record) {
        mMainView.openFolder(DataManager.getRecordDirUri(record));
    }

    public void openRecordFolder() {
        if (mCurRecord != null) {
            mMainView.openFolder(DataManager.getRecordDirUri(mCurRecord));
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
            } else {
                LogManager.addLog(getString(R.string.log_record_edit_fields_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
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
     * Копирование ссылки на запись в буфер обмена.
     * @param record
     */
    private void copyRecordLink(TetroidRecord record) {
        if (record != null) {
            Utils.writeToClipboard(getContext(), getString(R.string.link_to_record), record.createUrl());
        } else {
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    /**
     * Удаление записи.
     * @param record
     */
    private void deleteRecord(TetroidRecord record) {
        RecordAskDialogs.deleteRecord(getContext(), () -> {
            int res = DataManager.deleteRecord(record, false);
            if (res == -1) {
                RecordAskDialogs.deleteRecordWithoutDir(getContext(), () -> {
                    int res1 = DataManager.deleteRecord(record, true);
                    onDeleteRecordResult(record, res1);
                });
            } else {
                onDeleteRecordResult(record, res);
            }
        });
    }

    /**
     * Обработка результата удаления записи.
     * @param record
     * @param res
     */
    private void onDeleteRecordResult(TetroidRecord record, int res) {
        if (res > 0) {
            mListAdapterRecords.getDataSet().remove(record);
            mListAdapterRecords.notifyDataSetChanged();
            mMainView.updateTags();
            LogManager.addLog(getString(R.string.record_deleted), LogManager.Types.INFO, Toast.LENGTH_SHORT);
        } else {
            LogManager.addLog(getString(R.string.log_record_delete_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
        }
    }

    private void moveRecord(int pos, boolean isUp) {
        if (isUp) {
            if (pos > 0) {
                Collections.swap(mListAdapterRecords.getDataSet(), pos-1, pos);
                mListAdapterRecords.notifyDataSetChanged();
            }
        } else {
            if (pos < mListAdapterRecords.getDataSet().size()) {
                Collections.swap(mListAdapterRecords.getDataSet(), pos, pos+1);
                mListAdapterRecords.notifyDataSetChanged();
            }
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.list_view_records) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.record_context, menu);
        }
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи.
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info.targetView.getParent() != mListViewRecords)
            return false;
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(info.position);
        if (record == null) {
            LogManager.addLog(getString(R.string.log_get_item_is_null), LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_open_record:
                showRecord(record);
                return true;
            case R.id.action_attached_files:
                showRecordFiles(record);
                return true;
            case R.id.action_open_record_folder:
                openRecordFolder(record);
                return true;
            case R.id.action_record_edit_fields:
                editRecordFields(record);
                return true;
            case R.id.action_copy_link:
                copyRecordLink(record);
                return true;
            case R.id.action_move_up:
                moveRecord(info.position, true);
                return true;
            case R.id.action_move_down:
                moveRecord(info.position, false);
                return true;
            case R.id.action_delete:
                deleteRecord(record);
                return true;
            default:
                return super.onContextItemSelected(item);
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

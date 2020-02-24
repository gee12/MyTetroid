package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
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
import com.gee12.mytetroid.views.AddRecordDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainPageFragment extends TetroidFragment {

    public static final int MAIN_VIEW_GLOBAL_FOUND = -1;
    public static final int MAIN_VIEW_NONE = 0;
    public static final int MAIN_VIEW_NODE_RECORDS = 1;
//    public static final int MAIN_VIEW_RECORD_TEXT = 2;
    public static final int MAIN_VIEW_RECORD_FILES = 2;
    public static final int MAIN_VIEW_TAG_RECORDS = 3;
//    public static final int VIEW_FOUND_RECORDS = 5;

    public static final int MENU_ITEM_ID_OPEN_RECORD = 1;
    public static final int MENU_ITEM_ID_SHOW_FILES = 2;
    public static final int MENU_ITEM_ID_OPEN_RECORD_FOLDER = 3;

    private ViewFlipper mViewFlipperfMain;
    private ListView mListViewRecords;
    private ListView mListViewFiles;
    private TextView mTextViewRecordsEmpty;
    private TextView mTextViewFilesEmpty;
    private MenuItem mMenuItemCurNode;
    private MenuItem mMenuItemCurRecord;
//    private MenuItem mMenuItemAttachedFiles;
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
        // mainView уже должен быть установлен
//        initRecordViews();
        // можно загружать настройки и хранилище
        mainView.onMainPageCreated();

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
//        mMenuItemAttachedFiles.setVisible(false);
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
//        boolean isShowRecordViewerMenus = (viewId != MainPageFragment.MAIN_VIEW_RECORD_TEXT
//                || curRecordViewId == RECORD_VIEW_VIEWER);
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
//            case MainPageFragment.MAIN_VIEW_RECORD_TEXT:
//                mMenuItemAttachedFiles.setVisible(isShowRecordViewerMenus);
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
                mMenuItemCurNode.setVisible(true);
                mMenuItemCurRecordFolder.setVisible(true);
//                if (viewId != MainPageFragment.MAIN_VIEW_RECORD_TEXT)
                    mMenuItemCurRecord.setVisible(true);
                mButtonAddFile.show();
                title = ((mCurRecord != null) ? mCurRecord.getName() : "");
                break;
        }
        mainView.updateMainToolbar(viewId, title);
//        mainView.updateMenuItems(viewId);
//        mainView.checkKeepScreenOn(viewId);
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
//            case MainPageFragment.MAIN_VIEW_RECORD_TEXT:
            case MainPageFragment.MAIN_VIEW_RECORD_FILES:
                title = ((mCurRecord != null) ? mCurRecord.getName() : "");
            break;
        }
        mainView.updateMainToolbar(restoredViewId, title);
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
        mainView.openRecord(record);
    }

    /**
     *
     */
    public void createRecord() {
        if (!BuildConfig.DEBUG) {
            LogManager.addLog("Not implemented yet..", LogManager.Types.INFO, Toast.LENGTH_SHORT);
            return;
        }
        AddRecordDialog.createTextSizeDialog(getContext(), null, new AddRecordDialog.INewRecordResult() {
            @Override
            public void onApply(String name, String tags, String author, String url) {
                TetroidRecord record = DataManager.createRecord(name, tags, author, url, mCurNode);
                if (record != null) {
                    mListAdapterRecords.notifyDataSetInvalidated();
                    showRecord(record);
                } else {
                    LogManager.addLog(getString(R.string.record_create_error), LogManager.Types.ERROR, Toast.LENGTH_LONG);
                }
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
            LogManager.addLog(getString(R.string.incorrect_dateformat_in_settings), LogManager.Types.WARNING, Toast.LENGTH_LONG);
            return getContext().getString(R.string.def_date_format_string);
        }
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param position Индекс записи в списке записей ветки
     */
    private void showRecordFiles(int position) {
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(position);
        showRecordFiles(record);
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param record Запись
     */
    public void showRecordFiles(TetroidRecord record) {
        if (record == null)
            return;
        this.mCurRecord = record;
        showRecordFiles(record.getAttachedFiles(), record);
    }

//    public void showCurRecordFiles() {
//        showRecordFiles(mCurRecord);
//    }

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
            LogManager.addLog(R.string.viewing_decrypted_not_possible, Toast.LENGTH_LONG);
            return;
        }
        TetroidFile file = mCurRecord.getAttachedFiles().get(position);
        mainView.openFile(file);
    }

    /**
     *
     */
    public void createFile() {
        if (!BuildConfig.DEBUG) {
            LogManager.addLog("Not implemented yet..", LogManager.Types.INFO, Toast.LENGTH_SHORT);
            return;
        }
    }

    private void openRecordFolder(int position) {
        TetroidRecord record = (TetroidRecord) mListAdapterRecords.getItem(position);
        mainView.openFolder(DataManager.getRecordDirUri(record));
    }

    public void openRecordFolder() {
        if (mCurRecord != null) {
            mainView.openFolder(DataManager.getRecordDirUri(mCurRecord));
        }
    }

//    public void setFullscreen(boolean isFullscreen) {
//        RecordView view = getCurRecordView();
//        if (view != null)
//            view.setFullscreen(isFullscreen);
//    }

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
//        this.mMenuItemAttachedFiles = menu.findItem(R.id.action_attached_files);
        this.mMenuItemCurRecordFolder = menu.findItem(R.id.action_cur_record_folder);
//        this.miRecordSave = menu.findItem(R.id.action_record_save);
//        this.miRecordHtml = menu.findItem(R.id.action_record_html);
    }

//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    public boolean onOptionsItemSelected(int menuId) {
//        int id = item.getItemId();
        switch (menuId) {
            case R.id.action_cur_record:
                showCurRecord();
                return true;
//            case R.id.action_attached_files:
//                showCurRecordFiles();
//                return true;
            case R.id.action_cur_record_folder:
                openRecordFolder();
                return true;
//            case R.id.action_record_save:
//                saveCurRecord();
//                return true;
//            case R.id.action_record_html:
//                showCurRecord(RECORD_VIEW_HTML);
//                return true;
        }
//        return super.onOptionsItemSelected(item);
        return false;
    }

    /**
     * Обработчик создания контекстного меню при долгом тапе на записи
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, MENU_ITEM_ID_OPEN_RECORD, Menu.NONE, R.string.show_record_content);
        menu.add(Menu.NONE, MENU_ITEM_ID_SHOW_FILES, Menu.NONE, R.string.show_attached_files);
        menu.add(Menu.NONE, MENU_ITEM_ID_OPEN_RECORD_FOLDER, Menu.NONE, R.string.open_record_folder);
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case MENU_ITEM_ID_OPEN_RECORD:
                showRecord(info.position);
                return true;
            case MENU_ITEM_ID_SHOW_FILES:
                showRecordFiles(info.position);
                return true;
            case MENU_ITEM_ID_OPEN_RECORD_FOLDER:
                openRecordFolder(info.position);
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
        if (/*curView == MAIN_VIEW_RECORD_TEXT || */curView == MAIN_VIEW_RECORD_FILES) {
            res = true;
            switch (mLastViewId) {
                case MAIN_VIEW_NODE_RECORDS:
                case MAIN_VIEW_TAG_RECORDS:
//                case VIEW_FOUND_RECORDS:
//                case MAIN_VIEW_RECORD_TEXT: // если curView=MAIN_VIEW_RECORD_FILES
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

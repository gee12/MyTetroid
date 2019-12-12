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

import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.views.FilesListAdapter;
import com.gee12.mytetroid.views.RecordsListAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainPageFragment extends TetroidFragment {

    public static final int VIEW_GLOBAL_FOUND = -1;
    public static final int VIEW_NONE = 0;
    public static final int VIEW_NODE_RECORDS = 1;
    public static final int VIEW_RECORD_TEXT = 2;
    public static final int VIEW_RECORD_FILES = 3;
    public static final int VIEW_TAG_RECORDS = 4;
//    public static final int VIEW_FOUND_RECORDS = 5;

    public static final int VIEW_RECORD_VIEWER = 1;
    public static final int VIEW_RECORD_EDITOR = 2;
    public static final int VIEW_RECORD_HTML = 3;

    public static final int OPEN_RECORD_MENU_ITEM_ID = 1;
    public static final int SHOW_FILES_MENU_ITEM_ID = 2;
    public static final int OPEN_RECORD_FOLDER_MENU_ITEM_ID = 3;

    private ViewFlipper vfMain;
    private ListView lvRecords;
    private ListView lvFiles;
    private TextView tvRecordsEmpty;
    private TextView tvFilesEmpty;
    private ViewFlipper vfRecord;
    private ViewerFragment recordViewer;
    private EditorFragment recordEditor;
    private MenuItem miCurNode;
    private MenuItem miCurRecord;
    private MenuItem miAttachedFiles;
    private MenuItem miCurRecordFolder;

    private int curViewId;
    private int lastViewId;
    private RecordsListAdapter recordsListAdapter;
    private FilesListAdapter filesListAdapter;
//    private TetroidRecord prevRecord;
    private TetroidRecord curRecord;

    private boolean editMode;

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

        this.vfMain = view.findViewById(R.id.view_flipper);
        // обработка нажатия на пустом месте экрана, когда записей в ветке нет
        vfMain.setOnTouchListener(this);
        // список записей
        this.lvRecords = view.findViewById(R.id.list_view_records);
        // обработка нажатия на пустом месте списка записей
        lvRecords.setOnTouchListener(this);
        //
        lvRecords.setOnItemClickListener(onRecordClicklistener);
        this.tvRecordsEmpty = view.findViewById(R.id.text_view_empty_records);
        lvRecords.setEmptyView(tvRecordsEmpty);
        registerForContextMenu(lvRecords);
        // список файлов
        this.lvFiles = view.findViewById(R.id.list_view_files);
        // обработка нажатия на пустом месте списка файлов
        lvFiles.setOnTouchListener(this);
        lvFiles.setOnItemClickListener(onFileClicklistener);
        this.tvFilesEmpty = view.findViewById(R.id.text_view_empty_files);
        lvFiles.setEmptyView(tvFilesEmpty);

        this.vfRecord = view.findViewById(R.id.view_flipper);
        this.recordViewer = view.findViewById(R.id.record_viewer_view);
        this.recordEditor = view.findViewById(R.id.record_editor_view);

        FloatingActionButton fab = view.findViewById(R.id.button_edit_record);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchViewEditMode();
            }
        });

        this.curViewId = MainPageFragment.VIEW_NONE;
        setMainView(getArguments());

        mainView.onMainPageCreated();

        return view;
    }

    void initListAdapters(Context context) {
        // список записей
        if (lvRecords != null) {
            this.recordsListAdapter = new RecordsListAdapter(context, onRecordAttachmentClickListener);
            lvRecords.setAdapter(recordsListAdapter);
        }
        // список файлов
        if (lvFiles != null) {
            this.filesListAdapter = new FilesListAdapter(context);
            lvFiles.setAdapter(filesListAdapter);
        }
    }

    /**
     *
     * @param viewId
     */
    public void showView(int viewId) {
        miAttachedFiles.setVisible(false);
        miCurNode.setVisible(false);
        miCurRecord.setVisible(false);
        miCurRecordFolder.setVisible(false);
        // сохраняем значение для возврата на старое View
        // (только, если осуществляется переключение на действительно другую вьюшку)
        if (viewId != curViewId)
            this.lastViewId = curViewId;
        int whichChild = viewId;
        String title = null;
        switch (viewId) {
            // для "очистки" активности выводим пустой список записей
            case MainPageFragment.VIEW_NONE:
            // один контрол на записи ветки и метки
            case MainPageFragment.VIEW_TAG_RECORDS:
                whichChild = VIEW_NODE_RECORDS;
                break;
            case MainPageFragment.VIEW_RECORD_TEXT:
                miAttachedFiles.setVisible(true);
            case MainPageFragment.VIEW_RECORD_FILES:
                miCurNode.setVisible(true);
                miCurRecordFolder.setVisible(true);
                if (viewId != MainPageFragment.VIEW_RECORD_TEXT)
                    miCurRecord.setVisible(true);
                title = ((curRecord != null) ? curRecord.getName() : "");
                break;
        }
        mainView.updateMainToolbar(viewId, title);
        mainView.checkKeepScreenOn(viewId);
        this.curViewId = viewId;
        vfMain.setDisplayedChild(whichChild-1);
    }

    /**
     * Восстанавливаем состояние Toolbar при переключении обратно к фрагменту MainPageFragment.
     */
    public void restoreLastMainToolbarState() {
        String title = null;
        int restoredViewId = curViewId;
        switch (restoredViewId) {
            case MainPageFragment.VIEW_RECORD_TEXT:
            case MainPageFragment.VIEW_RECORD_FILES:
                title = ((curRecord != null) ? curRecord.getName() : "");
            break;
        }
        mainView.updateMainToolbar(restoredViewId, title);
    }

    public void clearView() {
        showView(VIEW_NONE);
        this.curRecord = null;
        tvRecordsEmpty.setText(R.string.select_the_node);
        lvRecords.setAdapter(null);
        lvFiles.setAdapter(null);
    }

    public void showRecords(List<TetroidRecord> records, int viewId) {
        String dateTimeFormat = checkDateFormatString();
        showView(viewId);
        tvRecordsEmpty.setText(R.string.records_is_missing);
        this.recordsListAdapter.setDataItems(records, viewId, dateTimeFormat);
        lvRecords.setAdapter(recordsListAdapter);
    }

    private void showRecordView(int viewId) {
        vfRecord.setDisplayedChild(viewId-1);
    }

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей
     */
    private void showRecord(int position) {
        TetroidRecord record = (TetroidRecord) recordsListAdapter.getItem(position);
        recordViewer.showRecord(record);
    }

    public void showCurRecord() {
        recordViewer.showRecord(curRecord);
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

    private void switchViewEditMode() {
        if (editMode)
            viewCurRecord();
        else
            editCurRecord();
        editMode = !editMode;
    }

    private void viewCurRecord() {
//        recordEditor.setVisibility(View.GONE);
//        recordWebView.setVisibility(View.VISIBLE);
//        recordFieldsLayout.setVisibility(View.VISIBLE);
    }

    private void editCurRecord() {
//        recordWebView.setVisibility(View.GONE);
//        recordFieldsLayout.setVisibility(View.GONE);
//        recordEditor.setVisibility(View.VISIBLE);
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param position Индекс записи в списке записей ветки
     */
    private void showRecordFiles(int position) {
        TetroidRecord record = (TetroidRecord) recordsListAdapter.getItem(position);
        showRecordFiles(record);
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param record Запись
     */
    public void showRecordFiles(TetroidRecord record) {
        if (record == null)
            return;
        this.curRecord = record;
        showRecordFiles(record.getAttachedFiles(), record);
    }

    public void showCurRecordFiles() {
        showRecordFiles(curRecord);
    }

    public void showRecordFiles(List<TetroidFile> files, TetroidRecord record) {
        showView(VIEW_RECORD_FILES);
        this.filesListAdapter.reset(files, record);
        lvFiles.setAdapter(filesListAdapter);
    }

    /**
     * Открытие прикрепленного файла
     * @param position Индекс файла в списке прикрепленных файлов записи
     */
    private void openFile(int position) {
        if (curRecord.isCrypted() && !SettingsManager.isDecryptFilesInTemp()) {
            LogManager.addLog(R.string.viewing_decrypted_not_possible, Toast.LENGTH_LONG);
            return;
        }
        TetroidFile file = curRecord.getAttachedFiles().get(position);
        mainView.openFile(curRecord, file);
    }

    private void openRecordFolder(int position) {
        TetroidRecord record = (TetroidRecord) recordsListAdapter.getItem(position);
        mainView.openFolder(DataManager.getRecordDirUri(record));
    }

    public void openRecordFolder() {
        if (curRecord != null) {
            mainView.openFolder(DataManager.getRecordDirUri(curRecord));
        }
    }

    /**
     * Обработчик клика на записи
     */
    private AdapterView.OnItemClickListener onRecordClicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showRecord(position);
        }
    };

    /**
     * Обработчик клика на иконке прикрепленных файлов записи
     */
    RecordsListAdapter.OnRecordAttachmentClickListener onRecordAttachmentClickListener = new RecordsListAdapter.OnRecordAttachmentClickListener() {
        @Override
        public void onClick(TetroidRecord record) {
            showRecordFiles(record);
        }
    };

    /**
     * Обработчик клика на прикрепленном файле
     */
    private AdapterView.OnItemClickListener onFileClicklistener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            openFile(position);
        }
    };

    public void onCreateOptionsMenu(Menu menu) {
        this.miCurNode = menu.findItem(R.id.action_cur_node);
        this.miCurRecord = menu.findItem(R.id.action_cur_record);
        this.miAttachedFiles = menu.findItem(R.id.action_attached_files);
        this.miCurRecordFolder = menu.findItem(R.id.action_cur_record_folder);
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

        menu.add(Menu.NONE, OPEN_RECORD_MENU_ITEM_ID, Menu.NONE, R.string.show_record_content);
        menu.add(Menu.NONE, SHOW_FILES_MENU_ITEM_ID, Menu.NONE, R.string.show_attached_files);
        menu.add(Menu.NONE, OPEN_RECORD_FOLDER_MENU_ITEM_ID, Menu.NONE, R.string.open_record_folder);
    }

    /**
     * Обработчик выбора пунктов контекстного меню записи
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case OPEN_RECORD_MENU_ITEM_ID:
                showRecord(info.position);
                return true;
            case SHOW_FILES_MENU_ITEM_ID:
                showRecordFiles(info.position);
                return true;
            case OPEN_RECORD_FOLDER_MENU_ITEM_ID:
                openRecordFolder(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Обработчик нажатия кнопки Назад
     */
    public boolean onBackPressed() {
        boolean res = false;
        int curView = vfMain.getDisplayedChild() + 1;
        if (curView == VIEW_RECORD_TEXT || curView == VIEW_RECORD_FILES) {
            res = true;
            switch (lastViewId) {
                case VIEW_NODE_RECORDS:
                case VIEW_TAG_RECORDS:
//                case VIEW_FOUND_RECORDS:
                case VIEW_RECORD_TEXT: // если curView=VIEW_RECORD_FILES
                    showView(lastViewId);
                    break;
                default:
                    showView(VIEW_NONE);
            }
        }
        /*if (curView == VIEW_RECORD_TEXT) {
            res = true;
            // смотрим какая страница была перед этим
            if (lastViewId == VIEW_NODE_RECORDS)
                showView(VIEW_NODE_RECORDS);
            else if (lastViewId == VIEW_TAG_RECORDS)
                showView(VIEW_TAG_RECORDS);
            else
                showView(VIEW_NONE);
        } else if (curView == VIEW_RECORD_FILES) {
            res = true;
            // смотрим какая страница была перед этим
            if (lastViewId == VIEW_RECORD_TEXT)
                showView(VIEW_RECORD_TEXT);
            else if (lastViewId == VIEW_NODE_RECORDS)
                showView(VIEW_NODE_RECORDS);
            else if (lastViewId == VIEW_TAG_RECORDS)
                showView(VIEW_TAG_RECORDS);
            else
                showView(VIEW_NONE);
        }*/
        return res;
    }

    public void setRecordsEmptyViewText(String s) {
        tvRecordsEmpty.setText(s);
    }

    public void setFilesEmptyViewText(String s) {
        tvFilesEmpty.setText(s);
    }

    public TetroidRecord getCurRecord() {
        return curRecord;
    }

    public int getCurViewId() {
        return curViewId;
    }

    public int getLastViewId() {
        return lastViewId;
    }

    @Override
    public String getTitle() {
        return "Главная";
    }
}

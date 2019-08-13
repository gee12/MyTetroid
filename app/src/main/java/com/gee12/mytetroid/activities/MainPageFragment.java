package com.gee12.mytetroid.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.views.FilesListAdapter;
import com.gee12.mytetroid.views.RecordsListAdapter;
import com.gee12.mytetroid.views.TetroidFragment;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.List;

public class MainPageFragment extends TetroidFragment implements CompoundButton.OnCheckedChangeListener {

    public static final int VIEW_GLOBAL_FOUND = -1;
    public static final int VIEW_NONE = 0;
    public static final int VIEW_NODE_RECORDS = 1;
    public static final int VIEW_RECORD_TEXT = 2;
    public static final int VIEW_RECORD_FILES = 3;
    public static final int VIEW_TAG_RECORDS = 4;
    public static final int VIEW_FOUND_RECORDS = 5;

    public static final int OPEN_RECORD_MENU_ITEM_ID = 1;
    public static final int SHOW_FILES_MENU_ITEM_ID = 2;
    public static final int OPEN_RECORD_FOLDER_MENU_ITEM_ID = 3;

    private ViewFlipper viewFlipper;
    private ListView lvRecords;
    private ListView lvFiles;
    private TextView tvRecordsEmpty;
    private TextView tvFilesEmpty;
    private ExpandableLayout expRecordFieldsLayout;
    private ToggleButton tbRecordFieldsExpander;
    private TextView tvRecordTags;
    private TextView tvRecordAuthor;
    private TextView tvRecordUrl;
    private TextView tvRecordDate;
    private WebView recordContentWebView;

    private int curViewId;
    private int lastViewId;
    private RecordsListAdapter recordsListAdapter;
    private FilesListAdapter filesListAdapter;
    private TetroidRecord prevRecord;
    private TetroidRecord curRecord;

    public MainPageFragment() {
    }
//
//    public MainPageFragment(IMainView mainView) {
//        super(mainView);
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        this.viewFlipper = view.findViewById(R.id.view_flipper);
        // список записей
        this.lvRecords = view.findViewById(R.id.list_view_records);
        lvRecords.setOnItemClickListener(onRecordClicklistener);
        this.tvRecordsEmpty = view.findViewById(R.id.text_view_empty_records);
        lvRecords.setEmptyView(tvRecordsEmpty);
        registerForContextMenu(lvRecords);
        // список файлов
        this.lvFiles = view.findViewById(R.id.list_view_files);
        lvFiles.setOnItemClickListener(onFileClicklistener);
        this.tvFilesEmpty = view.findViewById(R.id.text_view_empty_files);
        lvFiles.setEmptyView(tvFilesEmpty);
        // текст записи
        this.recordContentWebView = view.findViewById(R.id.web_view_record_content);
        this.tvRecordTags =  view.findViewById(R.id.text_view_record_tags);
        this.tvRecordAuthor =  view.findViewById(R.id.text_view_record_author);
        this.tvRecordUrl =  view.findViewById(R.id.text_view_record_url);
        this.tvRecordDate =  view.findViewById(R.id.text_view_record_date);
        this.expRecordFieldsLayout =  view.findViewById(R.id.layout_expander);
        this.tbRecordFieldsExpander =  view.findViewById(R.id.toggle_button_expander);
        tbRecordFieldsExpander.setOnCheckedChangeListener(this);

        this.curViewId = MainPageFragment.VIEW_NONE;
        setMainView(getArguments());

        return view;
    }

    public void initListViews() {
        // список записей
        this.recordsListAdapter = new RecordsListAdapter(this.getContext(), onRecordAttachmentClickListener);
        lvRecords.setAdapter(recordsListAdapter);
        // список файлов
        this.filesListAdapter = new FilesListAdapter(this.getContext());
        lvFiles.setAdapter(filesListAdapter);
    }

    /**
     *
     * @param viewId
     */
    public void showView(int viewId) {
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
            case MainPageFragment.VIEW_RECORD_FILES:
                title = ((curRecord != null) ? curRecord.getName() : "");
                break;
//            case MainPageFragment.VIEW_TAG_RECORDS:
                // один контрол на записи ветки и метки
//                whichChild = VIEW_NODE_RECORDS;
//                break;
        }
        mainView.updateMainToolbar(viewId, title);
        this.curViewId = viewId;
        viewFlipper.setDisplayedChild(whichChild-1);
    }

    /**
     * Восстанавливаем
     */
    public void restoreLastMainTooltipState() {
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
//        this.recordsListAdapter.setDataItems(new ArrayList<TetroidRecord>());
        lvRecords.setAdapter(null);
        lvFiles.setAdapter(null);
    }

    public void showRecords(List<TetroidRecord> records, int viewId) {
        showView(viewId);
//        if (viewId == VIEW_NODE_RECORDS)
            tvRecordsEmpty.setText(R.string.records_is_missing);
//        else
//            tvRecordsEmpty.setText(R.string.);

        this.recordsListAdapter.setDataItems(records);
        lvRecords.setAdapter(recordsListAdapter);
    }

    /**
     * Отображение записи
     * @param position Индекс записи в списке записей
     */
    private void showRecord(int position) {
//        TetroidRecord record = (curViewId == VIEW_NODE_RECORDS)
//                ? currentNode.getRecords().get(position)
//                : DataManager.getTagsHashMap().get(currentTag).get(position);
        TetroidRecord record = (TetroidRecord) recordsListAdapter.getItem(position);
        showRecord(record);
    }

    /**
     * Отображение записи
     * @param record Запись
     */
    public void showRecord(final TetroidRecord record) {
        this.curRecord = record;
        LogManager.addLog("Чтение записи: id=" + record.getId());
        String text = DataManager.getRecordHtmlTextDecrypted(record);
        if (text == null) {
            LogManager.addLog("Ошибка чтения записи", Toast.LENGTH_LONG);
            return;
        }
        recordContentWebView.loadDataWithBaseURL(DataManager.getRecordDirUri(record),
                text, "text/html", "UTF-8", null);
//            recordContentWebView.loadUrl(recordContentUrl);
        recordContentWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                tvRecordTags.setText(record.getTagsString());
                tvRecordAuthor.setText(record.getAuthor());
                tvRecordUrl.setText(record.getUrl());
                if (record.getCreated() != null)
                    tvRecordDate.setText(record.getCreatedString(SettingsManager.getDateFormatString()));
                showView(VIEW_RECORD_TEXT);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("mytetra")) {
                    // обрабатываем внутреннюю ссылку
                    String id = url.substring(url.lastIndexOf('/')+1);
                    TetroidRecord record = DataManager.getRecord(id);

                    // !!
                    // вот тут пока неясно что делать потом с командой Back, например.
                    showRecord(record);

//                    return super.shouldOverrideUrlLoading(view, request);
                } else {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    } catch (Exception ex) {
                        LogManager.addLog(ex);
                    }
                }
                return true;
            }
        });
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param position Индекс записи в списке записей ветки
     */
    private void showRecordFiles(int position) {
//        TetroidRecord record = currentNode.getRecords().get(position);
        TetroidRecord record = (TetroidRecord) recordsListAdapter.getItem(position);
        showRecordFiles(record);
    }

    /**
     * Отображение списка прикрепленных файлов
     * @param record Запись
     */
    public void showRecordFiles(TetroidRecord record) {
        this.curRecord = record;
        showRecordFiles(record.getAttachedFiles());
//        setTitle(record.getName());
    }

    public void showRecordFiles(List<TetroidFile> files) {
        showView(VIEW_RECORD_FILES);
        this.filesListAdapter.reset(files);
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
//        TetroidRecord record = currentNode.getRecords().get(position);
        TetroidRecord record = (TetroidRecord) recordsListAdapter.getItem(position);
        mainView.openFolder(DataManager.getRecordDirUri(record));
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

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        expRecordFieldsLayout.toggle();
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
        int curView = viewFlipper.getDisplayedChild() + 1;
        if (curView == VIEW_RECORD_TEXT) {
            res = true;
            // смотрим какая страница была перед этим
            if (lastViewId == VIEW_NODE_RECORDS)
                showView(VIEW_NODE_RECORDS);
            else
                showView(VIEW_TAG_RECORDS);
        } else if (curView == VIEW_RECORD_FILES) {
            res = true;
            // смотрим какая страница была перед этим
            if (lastViewId == VIEW_RECORD_TEXT)
                showView(VIEW_RECORD_TEXT);
            else if (lastViewId == VIEW_NODE_RECORDS)
                showView(VIEW_NODE_RECORDS);
            else
                showView(VIEW_TAG_RECORDS);
        }
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

//    public int getLastViewIdBeforeSearch() {
//        return lastViewId;
//    }

    @Override
    public String getTitle() {
        return "Главная";
    }
}

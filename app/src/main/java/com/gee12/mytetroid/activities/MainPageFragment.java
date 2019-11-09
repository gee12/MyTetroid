package com.gee12.mytetroid.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
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
import com.gee12.mytetroid.views.TetroidFragment;
import com.gee12.mytetroid.views.TetroidWebView;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
    private RelativeLayout recordFieldsLayout;
    private ExpandableLayout expRecordFieldsLayout;
    private ToggleButton tbRecordFieldsExpander;
//    private TextView tvRecordTags;
    private WebView wvRecordTags;
    private TextView tvRecordAuthor;
    private TextView tvRecordUrl;
    private TextView tvRecordDate;
    private TetroidWebView recordContentWebView;
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

        this.viewFlipper = view.findViewById(R.id.view_flipper);
        // обработка нажатия на пустом месте экрана, когда записей в ветке нет
        viewFlipper.setOnTouchListener(this);
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
        // текст записи
        this.recordContentWebView = view.findViewById(R.id.web_view_record_content);
        // обработка нажатия на тексте записи
        recordContentWebView.setOnTouchListener(this);
        recordContentWebView.getSettings().setBuiltInZoomControls(true);
        recordContentWebView.getSettings().setDisplayZoomControls(false);
        this.recordFieldsLayout = view.findViewById(R.id.layout_record_fields);
//        this.tvRecordTags =  view.findViewById(R.id.text_view_record_tags);
        this.wvRecordTags =  view.findViewById(R.id.web_view_record_tags);
        wvRecordTags.setBackgroundColor(Color.TRANSPARENT);
        wvRecordTags.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                String decodedUrl;
                // декодируем url
                try {
                    decodedUrl = URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    LogManager.addLog("Ошибка декодирования url: " + url, ex);
                    return true;
                }
                // избавляемся от приставки "tag:"
                String tagName = decodedUrl.substring(TetroidRecord.TAG_LINKS_PREF.length());
                mainView.openTag(tagName);
                return true;
            }
        });
        this.tvRecordAuthor =  view.findViewById(R.id.text_view_record_author);
        this.tvRecordUrl =  view.findViewById(R.id.text_view_record_url);
        this.tvRecordDate =  view.findViewById(R.id.text_view_record_date);
        this.expRecordFieldsLayout =  view.findViewById(R.id.layout_expander);
        this.tbRecordFieldsExpander =  view.findViewById(R.id.toggle_button_expander);
        tbRecordFieldsExpander.setOnCheckedChangeListener(this);

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
        viewFlipper.setDisplayedChild(whichChild-1);
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
     * Отображение записи
     * @param position Индекс записи в списке записей
     */
    private void showRecord(int position) {
        TetroidRecord record = (TetroidRecord) recordsListAdapter.getItem(position);
        showRecord(record);
    }

    public void showCurRecord() {
        showRecord(curRecord);
    }

    /**
     * Отображение записи
     * @param record Запись
     */
    public void showRecord(final TetroidRecord record) {
        if (record == null)
            return;
        this.curRecord = record;
        LogManager.addLog("Чтение записи: id=" + record.getId());
        String text = DataManager.getRecordHtmlTextDecrypted(record);
        if (text == null) {
            LogManager.addLog("Ошибка чтения записи", Toast.LENGTH_LONG);
            return;
        }
        // поля
//                tvRecordTags.setText(record.getTagsString());
        String tagsString = record.getTagsLinksString();
        int id = R.id.label_record_tags;
        if (tagsString != null) {
            // указываем charset в mimeType для кириллицы
            wvRecordTags.loadData(tagsString, "text/html; charset=UTF-8", null);
            id = R.id.web_view_record_tags;
        }
        // указываем относительно чего теперь выравнивать следующую панель
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, id);
        expRecordFieldsLayout.setLayoutParams(params);

        tvRecordAuthor.setText(record.getAuthor());
        tvRecordUrl.setText(record.getUrl());
        if (record.getCreated() != null)
            tvRecordDate.setText(record.getCreatedString(getString(R.string.full_date_format_string)));

        // текст
        recordContentWebView.loadDataWithBaseURL(DataManager.getRecordDirUri(record),
                text, "text/html", "UTF-8", null);
//            recordContentWebView.loadUrl(recordContentUrl);
        recordContentWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                showView(VIEW_RECORD_TEXT);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                onUrlClick(url);
                return true;
            }
        });

    }

    private void onUrlClick(String url) {
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

    public void setRecordFieldsVisibility(boolean isVisible) {
        recordFieldsLayout.setVisibility((isVisible) ? View.VISIBLE : View.GONE);
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
        int curView = viewFlipper.getDisplayedChild() + 1;
        if (curView == VIEW_RECORD_TEXT || curView == VIEW_RECORD_FILES) {
            res = true;
            switch (lastViewId) {
                case VIEW_NODE_RECORDS:
                case VIEW_TAG_RECORDS:
                case VIEW_FOUND_RECORDS:
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

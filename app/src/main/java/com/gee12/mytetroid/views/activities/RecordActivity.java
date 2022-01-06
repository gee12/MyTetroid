package com.gee12.mytetroid.views.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SearchView;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.htmlwysiwygeditor.IImagePicker;
import com.gee12.htmlwysiwygeditor.INetworkWorker;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidSuggestionProvider;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.helpers.HtmlHelper;
import com.gee12.mytetroid.data.settings.CommonSettings;
import com.gee12.mytetroid.model.TetroidImage;
import com.gee12.mytetroid.viewmodels.ActivityResult;
import com.gee12.mytetroid.viewmodels.RecordViewModel;
import com.gee12.mytetroid.viewmodels.ResultObj;
import com.gee12.mytetroid.views.dialogs.AskDialogs;
import com.gee12.mytetroid.views.dialogs.record.RecordDialogs;
import com.gee12.mytetroid.helpers.TetroidClipboardListener;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.services.FileObserverService;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;
import com.gee12.mytetroid.views.TetroidImagePicker;
import com.gee12.mytetroid.views.SearchViewXListener;
import com.gee12.mytetroid.views.TetroidEditText;
import com.gee12.mytetroid.views.TetroidEditor;
import com.gee12.mytetroid.views.dialogs.record.RecordFieldsDialog;
import com.gee12.mytetroid.views.dialogs.record.RecordInfoDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.lumyjuwon.richwysiwygeditor.IColorPicker;
import com.lumyjuwon.richwysiwygeditor.RichEditor.EditableWebView;
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/**
 * Активность просмотра и редактирования содержимого записи.
 */
public class RecordActivity extends TetroidActivity<RecordViewModel> implements
        EditableWebView.IPageLoadListener,
        EditableWebView.ILinkLoadListener,
        EditableWebView.IHtmlReceiveListener,
        EditableWebView.IYoutubeLinkLoadListener,
        IImagePicker,
        IColorPicker,
        INetworkWorker,
        ColorPickerDialogListener,
        TetroidEditor.IEditorListener {

    private ExpandableLayout mFieldsExpanderLayout;
    private FloatingActionButton mButtonToggleFields;
    private FloatingActionButton mButtonFullscreen;
    private TextView mTextViewTags;
    private ScrollView mScrollViewHtml;
    private TetroidEditText mEditTextHtml;
    protected ProgressBar mHtmlProgressBar;
    private TetroidEditor mEditor;
    private FloatingActionButton mButtonScrollTop;
    private FloatingActionButton mButtonFindNext;
    private FloatingActionButton mButtonFindPrev;

    private TextFindListener mFindListener;
    private SearchView mSearchView;


    public RecordActivity() {
        super();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_record;
    }

    @Override
    protected Class<RecordViewModel> getViewModelClazz() {
        return RecordViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (receivedIntent == null || receivedIntent.getAction() == null) {
            finish();
            return;
        }

        startInitStorage();

        viewModel.onCreate(receivedIntent);

        this.mEditor = findViewById(R.id.html_editor);
        mEditor.setColorPickerListener(this);
        mEditor.setImagePickerListener(this);
        mEditor.setNetworkWorkerListener(this);
        mEditor.setToolBarVisibility(false);
//        mEditor.setOnTouchListener(this);
        mEditor.setOnPageLoadListener(this);
        mEditor.setEditorListener(this);
        EditableWebView webView = mEditor.getWebView();
        webView.setOnTouchListener(this);
        webView.setOnUrlLoadListener(this);
        webView.setOnHtmlReceiveListener(this);
        webView.setYoutubeLoadLinkListener(this);
        webView.setClipboardListener(new TetroidClipboardListener(this));

        this.mTextViewTags = findViewById(R.id.text_view_record_tags);
        mTextViewTags.setMovementMethod(LinkMovementMethod.getInstance());

        this.mFieldsExpanderLayout = findViewById(R.id.layout_fields_expander);
//        ToggleButton tbRecordFieldsExpander = findViewById(R.id.toggle_button_expander);
//        tbRecordFieldsExpander.setOnCheckedChangeListener((buttonView, isChecked) -> mFieldsExpanderLayout.toggle());
        this.mButtonToggleFields = findViewById(R.id.button_toggle_fields);
        mButtonToggleFields.setOnClickListener(v -> toggleRecordFieldsVisibility());
        setRecordFieldsVisibility(false);
//        ViewUtils.setOnGlobalLayoutListener(mButtonToggleFields, () -> updateScrollButtonLocation());
//        this.mButtonFieldsEdit = findViewById(R.id.button_edit_fields);
//        mButtonFieldsEdit.setOnClickListener(v -> editFields());
        this.mButtonFullscreen = findViewById(R.id.button_fullscreen);
        mButtonFullscreen.setOnClickListener(v -> toggleFullscreen(false));

        FloatingActionButton mButtonScrollBottom = findViewById(R.id.button_scroll_bottom);
        this.mButtonScrollTop = findViewById(R.id.button_scroll_top);
        // прокидывание кнопок пролистывания в редактор
        mEditor.initScrollButtons(mButtonScrollBottom, mButtonScrollTop);

        this.mButtonFindNext = findViewById(R.id.button_find_next);
        mButtonFindNext.setOnClickListener(v -> findNext());
        this.mButtonFindPrev = findViewById(R.id.button_find_prev);
        mButtonFindPrev.setOnClickListener(v -> findPrev());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.mFindListener = new TextFindListener();
            mEditor.getWebView().setFindListener(mFindListener);
        } else {
            // TODO: что здесь для API=15 ?
        }

        this.mScrollViewHtml = findViewById(R.id.scroll_html);
        this.mEditTextHtml = findViewById(R.id.edit_text_html);
//        mEditTextHtml.setOnTouchListener(this); // работает криво
        mEditTextHtml.setTetroidListener(new HtmlEditTextEditTextListener());
        mEditTextHtml.initSearcher(mScrollViewHtml);

        this.mHtmlProgressBar = findViewById(R.id.progress_bar);

        // не гасим экран, если установлена опция
        App.checkKeepScreenOn(this);

        afterOnCreate();
    }

    @Override
    protected void initViewModel() {
        super.initViewModel();
        viewModel.getObjectAction().observe(this, it -> onEvent((RecordViewModel.RecordEvents) it.getState(), it.getData()));
        viewModel.getCurRecord().observe(this, it -> viewModel.openRecord(it));
    }

    /**
     *
     * @param event
     * @param data
     */
    @Override
    protected void onViewEvent(Constants.ViewEvents event, Object data) {
        switch (event) {
            // activity
            case StartActivity:
                startActivity((Intent) data);
                break;
            case SetActivityResult:
                ActivityResult result1 = (ActivityResult) data;
                setResult(result1.getCode(), result1.getIntent());
                break;
            case FinishActivity:
                finish();
                break;
            case FinishWithResult:
                ActivityResult result = (ActivityResult) data;
                finishWithResult(result.getCode(), result.getBundle());
                break;

            // ui
            case UpdateTitle:
                setTitle((String) data);
                break;
            case UpdateOptionsMenu:
                updateOptionsMenu();
                break;
            case ShowHomeButton:
                setVisibilityActionHome(false);
                break;

            // long-term tasks
            case TaskStarted:
                taskPreExecute((data instanceof Integer) ? (int)data : R.string.task_wait);
                break;
            case TaskFinished:
                taskPostExecute();
                break;
        }
        super.onViewEvent(event, data);
    }

    /**
     * Обработчик изменения состояния хранилища.
     * @param event
     * @param data
     */
    @Override
    protected void onStorageEvent(Constants.StorageEvents event, Object data) {
        switch (event) {
            case PermissionCheck:
                break;
            case PermissionGranted:
                // загружаем параметры хранилища только после проверки разрешения на запись во внешнюю память
                startInitStorage();
                break;
            case PermissionCanceled:
                // закрываем активити, если нет разрешения на запись во внешнюю память
                // TODO: поведение потребуется изменить, если хранилище загружается только на чтение
                finish();
                break;
            case Inited:
                viewModel.onStorageInited(receivedIntent);
                break;
        }
        super.onStorageEvent(event, data);
    }

    private void startInitStorage() {
        viewModel.checkWriteExtStoragePermission(this, Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE, () -> {
            switch (receivedIntent.getAction()) {
                // открытие или создание записи из главной активности
                case Intent.ACTION_MAIN:
                    // создание записи из виджета
                case Constants.ACTION_ADD_RECORD:
                    if (!viewModel.initStorage(receivedIntent)) {
                        finish();
                    }
                    break;
                default:
                    finish();
                    break;
            }
            return null;
        });
    }

    private void showNeedMigrationDialog() {
        AskDialogs.showOkDialog(this, R.string.dialog_need_migration, R.string.answer_ok, false, () -> {
            finish();
        });
    }

    /**
     * Обработчик действий в окне записи.
     * @param action
     * @param data
     */
    protected void onEvent(RecordViewModel.RecordEvents action, Object data) {
        switch (action) {
            case NeedMigration:
                showNeedMigrationDialog();
                break;
            case Save:
                // если сохраняем запись перед выходом, то учитываем, что можем находиться в режиме HTML
                String htmlText = TetroidEditor.getDocumentHtml((viewModel.isHtmlMode())
                        ? mEditTextHtml.getText().toString()
                        : mEditor.getWebView().getEditableHtml());
                viewModel.save(htmlText);
                break;
            case LoadFields:
                loadFields((TetroidRecord) data);
                expandFieldsIfNeed();
                break;
            case EditFields:
                editFields((ResultObj) data);
                break;
            case LoadRecordTextFromFile:
                loadRecordText((String) data);
                break;
            case LoadRecordTextFromHtml:
                loadRecordTextFromHtmlEditor();
                break;
            case AskForLoadAllNodes:
                ResultObj obj = (ResultObj) data;
                AskDialogs.showLoadAllNodesDialog(this, () -> {
                    switch (obj.getType()) {
                        case ResultObj.OPEN_RECORD:
                            viewModel.showAnotherRecord(obj.getId());
                            break;
                        case ResultObj.OPEN_NODE:
                            viewModel.showAnotherNodeDirectly(obj.getId());
                            break;
                        case ResultObj.OPEN_TAG:
                            // id - это tagName
                            viewModel.openTagDirectly(obj.getId());
                            break;
                    }
                });
                break;
            case FileAttached:
                onFileAttached((TetroidFile) data);
                break;
            case SwitchViews:
                switchViews((int) data);
                break;
            case AskForSaving:
                askForSaving((ResultObj) data);
                break;
            case BeforeSaving:
                onBeforeSavingAsync();
                break;
            case IsEditedChanged:
                mEditor.setIsEdited((boolean) data);
                break;
            case EditedDateChanged:
                ((TextView)findViewById(R.id.text_view_record_edited)).setText((String) data);
                break;
            case StartCaptureCamera:
                setProgressVisibility(true, getString(R.string.progress_camera_capture));
                break;
            case StartLoadImages:
                setProgressVisibility(true, getString(R.string.progress_images_loading));
                break;
            case InsertImages:
                List<TetroidImage> images = (List<TetroidImage>) data;
                mEditor.insertImages(images, viewModel.getPathToRecordFolder(viewModel.getCurRecord().getValue()));
                setProgressVisibility(false, null);
                break;
            case OpenWebLink:
                openWebLink((String) data);
                break;
            case InsertWebPageContent:
                mEditor.insertWebPageContent((String) data, false);
                break;
            case InsertWebPageText:
                mEditor.insertWebPageContent((String) data, true);
                break;
        }
    }

    @Override
    protected void onUICreated(boolean uiCreated) {
        viewModel.checkMigration();
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.setFromAnotherActivity(isFromAnotherActivity());
    }

    // region OpenRecord

    /**
     * Отображние свойств записи.
     * @param record
     */
    private void loadFields(TetroidRecord record) {
        if (record == null)
            return;

        int id = R.id.label_record_tags;
        // метки
        String tagsHtml = HtmlHelper.createTagsHtmlString(record);
        if (tagsHtml != null) {
            CharSequence sequence = Utils.fromHtml(tagsHtml);
            SpannableString spannable = new SpannableString(sequence);
            URLSpan[] urls = spannable.getSpans(0, sequence.length(), URLSpan.class);
            for (URLSpan span : urls) {
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                ClickableSpan clickableSpan = new ClickableSpan() {
                    public void onClick(View view) {
                        viewModel.onTagUrlLoad(span.getURL());
                    }
                };
                spannable.removeSpan(span);
                spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            mTextViewTags.setText(spannable);
            id = R.id.text_view_record_tags;
        }
        // указываем относительно чего теперь выравнивать следующее за метками поле
        // (т.к. метки - многострочный элемент)
        TextView tvLabelAuthor = findViewById(R.id.label_record_author);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, id);
        tvLabelAuthor.setLayoutParams(params);

        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params2.addRule(RelativeLayout.BELOW, id);
        params2.addRule(RelativeLayout.RIGHT_OF, R.id.label_record_author);

        // автор
        TextView tvAuthor = findViewById(R.id.text_view_record_author);
        tvAuthor.setLayoutParams(params2);
        tvAuthor.setText(record.getAuthor());
        // url
        TextView tvUrl = findViewById(R.id.text_view_record_url);
        tvUrl.setText(record.getUrl());
        // даты
        String dateFormat = getString(R.string.full_date_format_string);
        TextView tvCreated = findViewById(R.id.text_view_record_created);
        Date created = record.getCreated();
        tvCreated.setText((created != null) ? Utils.dateToString(created, dateFormat) : "");

        if (App.INSTANCE.isFullVersion()) {
            (findViewById(R.id.label_record_edited)).setVisibility(View.VISIBLE);
            TextView tvEdited = findViewById(R.id.text_view_record_edited);
            tvEdited.setVisibility(View.VISIBLE);
            Date edited = viewModel.getRecordEditedDate(record);
            tvEdited.setText((edited != null) ? Utils.dateToString(edited, dateFormat) : "-");
        }
    }

    /**
     * Загрузка html-кода записи из редактора html-кода (fromHtmlEditor) в WebView.
     */
    private void loadRecordTextFromHtmlEditor() {
        String textHtml = mEditTextHtml.getText().toString();
        loadRecordText(textHtml);
    }

    private void loadRecordText(String textHtml) {
        mEditTextHtml.reset();
        //mEditor.getWebView().clearAndFocusEditor();
        String baseUrl = viewModel.getUriToRecordFolder(viewModel.getCurRecord().getValue());
        mEditor.getWebView().loadDataWithBaseURL(
                baseUrl,
                textHtml,
                "text/html",
                "UTF-8",
                null
        );
    }

    // endregion OpenRecord

    // region LoadPage

    /**
     * Событие запуска загрузки страницы.
     */
    @Override
    public void onStartPageLoading() {
    }

    @Override
    public void onPageLoading(int progress) {
    }

    /**
     * Событие окончания загрузки страницы.
     */
    @Override
    public void onPageLoaded() {
       viewModel.onPageLoaded();
    }

    /**
     * Событие начала загрузки Javascript-кода для редактирования текста.
     */
    @Override
    public void onStartEditorJSLoading() {
    }

    /**
     * Событие окончания загрузки Javascript-кода для редактирования текста.
     */
    @Override
    public void onEditorJSLoaded() {
        viewModel.onEditorJSLoaded(getIntent());
    }

    @Override
    public void onIsEditedChanged(boolean isEdited) {
        viewModel.setEdited(isEdited);
    }

    /**
     * Обработчик события возврата html-текста заметки из WebView.
     * @param htmlText
     */
    @Override
    public void onReceiveEditableHtml(String htmlText) {
        if (mEditor.isCalledHtmlRequest()) {
            // если этот метод был вызван в результате запроса isCalledHtmlRequest, то:
            mEditor.setHtmlRequestHandled();
            runOnUiThread(() -> {
                viewModel.onHtmlRequestHandled();
            });
        } else {
            // метод вызывается в параллельном потоке, поэтому устанавливаем текст в основном
            runOnUiThread(() -> {
                mEditTextHtml.setText(htmlText);
                mEditTextHtml.requestFocus();
//            setProgressVisibility(false);
            });
        }
    }

    // endregion LoadPage

    // region LoadLinks

    /**
     * Открытие ссылки в тексте.
     * @param url
     */
    @Override
    public boolean onLinkLoad(String url) {
        String baseUrl = mEditor.getWebView().getBaseUrl();
        viewModel.onLinkLoad(url, baseUrl);
        return true;
    }

    @Override
    public void onYoutubeLinkLoad(String videoId) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + videoId));
        startActivity(webIntent);
    }

    private void openWebLink(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception ex) {
            viewModel.logError(ex, true);
        }
    }

    // endregion LoadLinks

    // region ColorPicker

    @Override
    public void onPickColor(int curColor) {
        int defColor = curColor;
        if (defColor == 0) {
            // если не передали цвет, то достаем последний из сохраненных
            int[] savedColors = CommonSettings.getPickedColors(this);
            defColor = (savedColors != null && savedColors.length > 0)
                    ? savedColors[savedColors.length - 1] : Color.BLACK;
        }
        ColorPickerDialog.newBuilder()
                .setColor(defColor)
                .show(this);
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        CommonSettings.addPickedColor(this, color, WysiwygEditor.MAX_SAVED_COLORS);
        mEditor.setPickedColor(color);
    }

    @Override
    public int[] getSavedColors() {
        return CommonSettings.getPickedColors(this);
    }

    @Override
    public void removeSavedColor(int index, int color) {
        CommonSettings.removePickedColor(this, color);
    }

    @Override
    public void onDialogDismissed(int dialogId) {
    }

    // endregion ColorPicker

    // region Image

    @Override
    public void startPicker() {
        TetroidImagePicker.startPicker(this, Constants.REQUEST_CODE_IMAGES);
    }

    @Override
    public void startCamera() {
        // проверка разрешения
        if (!viewModel.getPermissionInteractor().checkCameraPermission(this, Constants.REQUEST_CODE_PERMISSION_CAMERA)) {
            return;
        }
        TetroidImagePicker.startCamera(this, Constants.REQUEST_CODE_CAMERA);
    }

    public void saveImage(Uri uri, boolean deleteSrcFile) {
        viewModel.saveImage(uri, deleteSrcFile);
    }

    /**
     * Загрузка содержимого Web-страницы по ссылке.
     * @param url
     * @return
     */
    @Override
    public void downloadWebPageContent(String url, boolean isTextOnly) {
        viewModel.downloadWebPageContent(url, isTextOnly);
    }

    /**
     * Загрузка изображение по ссылке.
     * @param url
     * @return
     */
    @Override
    public void downloadImage(String url) {
        viewModel.downloadImage(url);
    }

    // endregion Image

    // region Attach

    public void attachFile(Uri uri, boolean deleteSrcFile) {
        viewModel.attachFile(uri, deleteSrcFile);
    }

    private void onFileAttached(TetroidFile res) {
        AskDialogs.showYesNoDialog(this, new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
                viewModel.openRecordAttaches();
            }
            @Override
            public void onCancel() {
            }
        }, R.string.ask_open_attaches);
    }

    public void downloadAndAttachFile(Uri uri) {
        viewModel.downloadAndAttachFile(uri);
    }

    //endregion Attach

    //region Mode

    /**
     *
     * @param newMode
     */
    private void switchViews(int newMode) {
        viewModel.logDebug("switchViews: mode=" + newMode);
        switch (newMode) {
            case Constants.MODE_VIEW : {
                mEditor.setVisibility(View.VISIBLE);
                mEditor.setToolBarVisibility(false);
                mScrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(true);
                mEditor.setEditMode(false);
                mEditor.setScrollButtonsVisibility(true);
                setSubtitle(getString(R.string.subtitle_record_view));
                ViewUtils.hideKeyboard(this, mEditor.getWebView());
            } break;
            case Constants.MODE_EDIT : {
                mEditor.setVisibility(View.VISIBLE);
                // загружаем Javascript (если нужно)
//                if (!mEditor.getWebView().isEditorJSLoaded()) {
//                    setProgressVisibility(true);
//                }
                mEditor.getWebView().loadEditorJSScript(false);

                mEditor.setToolBarVisibility(true);
                mScrollViewHtml.setVisibility(View.GONE);
                setRecordFieldsVisibility(false);
                mEditor.setEditMode(true);
                mEditor.setScrollButtonsVisibility(false);
                setSubtitle(getString(R.string.subtitle_record_edit));
                mEditor.getWebView().focusEditor();
                ViewUtils.showKeyboard(this, mEditor.getWebView(), false);
            } break;
            case Constants.MODE_HTML : {
                mEditor.setVisibility(View.GONE);
                if (mEditor.getWebView().isHtmlRequestMade()) {
                    String htmlText = mEditor.getWebView().getEditableHtml();
                    mEditTextHtml.setText(htmlText);
                } else {
                    setEditorProgressVisibility(true);
                    // загружаем Javascript (если нужно), и затем делаем запрос на html-текст
                    mEditor.getWebView().loadEditorJSScript(true);

                }
//                mEditor.getWebView().makeEditableHtmlRequest();
                mScrollViewHtml.setVisibility(View.VISIBLE);
                setRecordFieldsVisibility(false);
                setSubtitle(getString(R.string.subtitle_record_html));
                ViewUtils.showKeyboard(this, mEditor.getWebView(), false);
            } break;
        }
    }

    // endregion Image

    // region Save record

    private void askForSaving(ResultObj obj) {
        RecordDialogs.saveRecord(RecordActivity.this, new Dialogs.IApplyCancelResult() {
            @Override
            public void onApply() {
                viewModel.saveRecord(obj);
            }
            @Override
            public void onCancel() {
                viewModel.onAfterSaving(obj);
            }
        });
    }

    /**
     * Предобработка html-текста перед сохранением.
     * Выполнение кода продолжиться в функции onReceiveEditableHtml().
     */
    private void onBeforeSavingAsync() {
        mEditor.beforeSaveAsync(true);
    }

    // endregion Save record

    // region Storage

    @Override
    public void afterStorageLoaded(boolean res) {
        viewModel.afterStorageLoaded();
    }

    // endregion Storage

    // region Options record

    /**
     * Отправка записи.
     */
    public void shareRecord() {
        String text = (viewModel.isHtmlMode())
                ? mEditTextHtml.getText().toString()
                : Utils.fromHtml(mEditor.getWebView().getEditableHtml()).toString();
        viewModel.shareRecord(text);
    }

    /**
     * Удаление записи.
     */
    public void deleteRecord() {
        RecordDialogs.deleteRecord(this, viewModel.getRecordName(), () -> {
            viewModel.deleteRecord();
        });
    }

    void showRecordInfoDialog() {
        new RecordInfoDialog(
                viewModel.getCurRecord().getValue()
        ).showIfPossible(getSupportFragmentManager());
    }

    // endregion Options record

    // region Search

    /**
     * Поиск по тексту записи.
     * @param query
     */
    private void searchInRecordText(String query) {
        TetroidSuggestionProvider.saveRecentQuery(this, query);
        if (viewModel.isHtmlMode()) {
            int matches = mEditTextHtml.getSearcher().findAll(query);
            if (matches >= 0) {
                onFindTextResult(matches);
            }
        } else {
            mEditor.searchText(query);
        }
        // сбрасываем счетчик совпадений от прежнего поиска
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mFindListener.reset();
        }
    }

    private void findPrev() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.getSearcher().prevMatch();
        } else {
            mEditor.prevMatch();
        }
    }

    private void findNext() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.getSearcher().nextMatch();
        } else {
            mEditor.nextMatch();
        }
    }

    private void stopSearch() {
        if (viewModel.isHtmlMode()) {
            mEditTextHtml.getSearcher().stopSearch();
        } else {
            mEditor.stopSearch();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    class TextFindListener implements WebView.FindListener {
        boolean isStartSearch = true;

        @Override
        public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting) {
            if (isDoneCounting && isStartSearch) {
                this.isStartSearch = false;
                onFindTextResult(numberOfMatches);
            }
        }

        void reset() {
            this.isStartSearch = true;
        }
    }

    public void onFindTextResult(int matches) {
        if (matches > 0) {
            viewModel.log(String.format(getString(R.string.log_n_matches_found), matches), true);
        } else {
            viewModel.log(getString(R.string.log_matches_not_found), true);
        }
    }

    // endregion Search


    private boolean isFromAnotherActivity() {
//        ActivityCompat.getReferrer()
        return getCallingActivity() != null;
    }

    /**
     * Сохранение записи при любом скрытии активности.
     */
    @Override
    public void onPause() {
        viewModel.onPause();
        super.onPause();
    }

    /**
     * Обработка возвращаемого результата других активностей.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE_STORAGE_SETTINGS_ACTIVITY) {
            if (data != null) {
                if (data.getBooleanExtra(Constants.EXTRA_IS_REINIT_STORAGE, false)) {
                    // хранилище изменено
                    if (viewModel.isLoaded()) {
                        // спрашиваем о перезагрузке хранилище, только если оно уже загружено
                        boolean isCreate = data.getBooleanExtra(Constants.EXTRA_IS_CREATE_STORAGE, false);
                        AskDialogs.showReloadStorageDialog(this, isCreate, true, () -> {
                            // перезагружаем хранилище в главной активности, если изменили путь,
                            finishWithResult(Constants.RESULT_REINIT_STORAGE, data.getExtras());
                        });
                    }
                } else if (data.getBooleanExtra(Constants.EXTRA_IS_PASS_CHANGED, false)) {
                    // пароль изменен
                    finishWithResult(Constants.RESULT_PASS_CHANGED, data.getExtras());
                }
            }
        } else if (requestCode == Constants.REQUEST_CODE_COMMON_SETTINGS_ACTIVITY) {
            // не гасим экран, если установили опцию
            App.checkKeepScreenOn(this);
        } else if (requestCode == Constants.REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            viewModel.saveSelectedImages(data, true);
        } else if (requestCode == Constants.REQUEST_CODE_IMAGES && resultCode == RESULT_OK) {
            viewModel.saveSelectedImages(data, false);
        }
    }

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
        if (action.equals(FileObserverService.ACTION_OBSERVER_EVENT_COME)) {
            // обработка внешнего изменения дерева записей
//            mOutsideChangingHandler.run(true);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // обработка результата голосового поиска
            String query = intent.getStringExtra(SearchManager.QUERY);
            mSearchView.setQuery(query, true);
        }
    }

    // region Options menu

    /**
     * Обработчик создания системного меню.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!super.onBeforeCreateOptionsMenu(menu))
            return true;
        getMenuInflater().inflate(R.menu.record, menu);
        this.optionsMenu = menu;

        initSearchView(menu);

        return super.onAfterCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isOnCreateProcessed())
            return true;
        // режимы
        int mode = viewModel.getCurMode();
        activateMenuItem(menu.findItem(R.id.action_record_view), mode == Constants.MODE_EDIT);
        activateMenuItem(menu.findItem(R.id.action_record_edit), mode == Constants.MODE_VIEW
            || mode == Constants.MODE_HTML);
        activateMenuItem(menu.findItem(R.id.action_record_html), mode == Constants.MODE_VIEW
                || mode == Constants.MODE_EDIT);
        activateMenuItem(menu.findItem(R.id.action_record_save), mode == Constants.MODE_EDIT);

        boolean isLoaded = viewModel.isLoaded();
        boolean isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly();
        boolean isTemp = viewModel.isRecordTemprorary();
        activateMenuItem(menu.findItem(R.id.action_record_edit_fields), isLoaded && !isLoadedFavoritesOnly, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_record_node), isLoaded && !isLoadedFavoritesOnly, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_delete), isLoaded && !isLoadedFavoritesOnly, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_attached_files), isLoaded, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_cur_record_folder), true, !isTemp);
        activateMenuItem(menu.findItem(R.id.action_info), isLoaded, !isTemp);
        enableMenuItem(menu.findItem(R.id.action_storage_settings), isLoaded);
        return super.onPrepareOptionsMenu(menu);
    }

    private void activateMenuItem(MenuItem menuItem, boolean isVisible, boolean isEnabled) {
        menuItem.setVisible(isVisible);
        menuItem.setEnabled(isEnabled);
    }

    private void activateMenuItem(MenuItem menuItem, boolean isVisible) {
        menuItem.setVisible(isVisible);
    }

    /**
     * Инициализация элементов для поиска.
     * @param menu
     */
    public void initSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        this.mSearchView = (SearchView) menu.findItem(R.id.action_search_text).getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(true);
        // добавлять кнопки справа в выпадающем списке предложений (suggestions), чтобы вставить выбранное
        // предложение в строку запроса для дальнейшего уточнения (изменения), а не для поиска по нему
        mSearchView.setQueryRefinementEnabled(true);
        new SearchViewXListener(mSearchView) {
            @Override
            public void onSearchClick() { }
            @Override
            public void onQuerySubmit(String query) {
                searchInRecordText(query);
                setFindButtonsVisibility(true);
            }
            @Override
            public void onQueryChange(String query) {
            }
            @Override
            public void onSuggestionSelectOrClick(String query) {
                mSearchView.setQuery(query, true);
            }
            @Override
            public void onClose() {
                setFindButtonsVisibility(false);
                stopSearch();
            }
        };
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
            case R.id.action_record_view:
                viewModel.switchMode(Constants.MODE_VIEW);
                return true;
            case R.id.action_record_edit:
                viewModel.switchMode(Constants.MODE_EDIT);
                return true;
            case R.id.action_record_html:
                viewModel.switchMode(Constants.MODE_HTML);
                return true;
            case R.id.action_record_save:
                viewModel.saveRecord(null);
                return true;
            case R.id.action_record_edit_fields:
                editFields(null);
                return true;
            case R.id.action_record_node:
                viewModel.showRecordNode();
                return true;
            case R.id.action_attached_files:
                viewModel.openRecordAttaches();
                return true;
            case R.id.action_cur_record_folder:
                viewModel.openRecordFolder();
                return true;
            case R.id.action_share:
                shareRecord();
                return true;
            case R.id.action_delete:
                deleteRecord();
                return true;
            case R.id.action_info:
                showRecordInfoDialog();
                return true;
            case R.id.action_fullscreen:
                toggleFullscreen(false);
                return true;
            case R.id.action_storage_settings:
                showStorageSettingsActivity(viewModel.getStorage());
                break;
            case R.id.action_settings:
                showActivityForResult(SettingsActivity.class, Constants.REQUEST_CODE_COMMON_SETTINGS_ACTIVITY);
                return true;
            case R.id.action_storage_info:
                ViewUtils.startActivity(this, StorageInfoActivity.class, null);
                return true;
            case android.R.id.home:
                return viewModel.onHomePressed();
        }
        return super.onOptionsItemSelected(item);
    }

    // endregion Options menu

    // region Record fields

    /**
     * Редактирование свойств записи.
     */
    private void editFields(ResultObj obj) {
//        RecordDialogs.createRecordFieldsDialog(this, viewModel.getMRecord(), true, null,
//                (name, tags, author, url, node, isFavor) -> {
//                    viewModel.editFields(obj, name, tags, author, url, node, isFavor);
//                });

        new RecordFieldsDialog(
                viewModel.getCurRecord().getValue(),
                true,
                null,
                (name, tags, author, url, node, isFavor) -> {
                    viewModel.editFields(obj, name, tags, author, url, node, isFavor);
                }
        ).show(getSupportFragmentManager(), RecordFieldsDialog.TAG);
    }

    /**
     * Скрытие/раскрытие панели со свойствами записи.
     */
    private void toggleRecordFieldsVisibility() {
        /*mFieldsExpanderLayout.toggle();

        if (mFieldsExpanderLayout.isExpanded()) {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white);
        } else {
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white);
        }*/
        expandRecordFields(!mFieldsExpanderLayout.isExpanded());
        updateScrollButtonLocation();
    }

    private void expandRecordFields(boolean expand) {
        if (expand) {
            mFieldsExpanderLayout.expand();
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_up_white);
        } else {
            mFieldsExpanderLayout.collapse();
            mButtonToggleFields.setImageResource(R.drawable.ic_arrow_drop_down_white);
        }
        updateScrollButtonLocation();
    }

    /**
     * Отображение или скрытие панели свойств в зависимости от настроек.
     */
    private void expandFieldsIfNeed() {
        expandRecordFields(viewModel.isNeedExpandFields());
    }

    /**
     * Управление видимостью панели со свойствами записи.
     * Полностью, вместе с кнопкой управления видимостью панели.
     * @param isVisible
     */
    private void setRecordFieldsVisibility(boolean isVisible) {
        if (isVisible) {
            mFieldsExpanderLayout.setVisibility(View.VISIBLE);
            mButtonToggleFields.show();
        } else {
            mFieldsExpanderLayout.setVisibility(View.GONE);
            mButtonToggleFields.hide();
        }
    }

    // endregion Record fields


    @Override
    public int toggleFullscreen(boolean fromDoubleTap) {
        int res = super.toggleFullscreen(fromDoubleTap);
        if (res >= 0) {
            boolean isFullscreen = (res == 1);
            if (isFullscreen) {
                mButtonFullscreen.show();
            } else {
                mButtonFullscreen.hide();
            }
            if (viewModel.isViewMode()) {
                setRecordFieldsVisibility(!isFullscreen);
            }
        }
        return res;
    }

    /**
     * Обновление расположения кнопок скроллинга записи вниз/вверх.
     */
    private void updateScrollButtonLocation() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mButtonScrollTop.getLayoutParams();
//        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) scrollTopButton.getLayoutParams();
//        float density = getResources().getDisplayMetrics().density;
//        int fabMargin = (int) (getResources().getDimension(R.dimen.fab_small_margin) / density);

        if (mFieldsExpanderLayout.isExpanded()) {
//            params.topMargin = fabMargin;
            params.addRule(RelativeLayout.ALIGN_TOP, R.id.html_editor);
//            params.topMargin = fabMargin;
        } else {
//            int aboveButtonHeight = (int) (mButtonToggleFields.getMeasuredHeight() / density);
//            params.topMargin = fabMargin + aboveButtonHeight + fabMargin;
//            params.rightMargin = fabMargin; // для совпадения с mButtonToggleFields
            params.addRule(RelativeLayout.BELOW, R.id.button_toggle_fields);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_TOP);
            }
//            params.topMargin = 0;
        }
        mButtonScrollTop.setLayoutParams(params);
    }

    /**
     * Установка видимости и позиционирования Fab кнопок при включении/выключении режима поиска.
     * @param vis
     */
    public void setFindButtonsVisibility(boolean vis) {
        ViewUtils.setFabVisibility(mButtonFindNext, vis);
        ViewUtils.setFabVisibility(mButtonFindPrev, vis);
        setRecordFieldsVisibility(!vis && viewModel.getCurMode() != Constants.MODE_HTML);
        // установим позиционирование кнопки FindNext от правого края
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mButtonFindNext.getLayoutParams();
        if (vis) {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.RIGHT_OF);
            }
        } else {
            params.addRule(RelativeLayout.RIGHT_OF, R.id.button_toggle_fields);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }
        }
        mButtonFindNext.setLayoutParams(params);
        // установим позиционирование кнопки ScrollToTop от кнопки FindNext
        if (vis) {
            params = (RelativeLayout.LayoutParams) mButtonScrollTop.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, R.id.button_find_next);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.ALIGN_TOP);
            }
            mButtonScrollTop.setLayoutParams(params);
        } else {
            updateScrollButtonLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        switch (requestCode) {
            case Constants.REQUEST_CODE_PERMISSION_CAMERA: {
                if (permGranted) {
                    viewModel.log(R.string.log_camera_perm_granted);
                    startCamera();
                } else {
                    viewModel.logWarning(R.string.log_missing_camera_perm, true);
                }
            }
            break;
        }
    }

    public void showActivityForResult(Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent, requestCode);
    }

    /**
     * Формирование результата активности и ее закрытие.
     * При необходимости запуск главной активности, если текущая была запущена самостоятельно.
     * @param resCode
     * @param bundle
     */
    private void finishWithResult(int resCode, Bundle bundle) {
        if (getCallingActivity() != null) {
            if (bundle != null) {
                bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode);
                Intent intent = new Intent(Constants.ACTION_RECORD);
                intent.putExtras(bundle);
                setResult(resCode, intent);
            } else {
                setResult(resCode);
            }
        } else {
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putInt(Constants.EXTRA_RESULT_CODE, resCode);
            ViewUtils.startActivity(this, MainActivity.class, bundle, Constants.ACTION_RECORD, 0, 0);
        }
        finish();
    }

    /**
     * Обработчик нажатия кнопки Назад.
     */
    @Override
    public void onBackPressed() {
        if (!onBeforeBackPressed()) {
            return;
        }
        // выполняем родительский метод только если не был запущен асинхронный код
        if (viewModel.isCanBack(isFromAnotherActivity())) {
            super.onBackPressed();
        }
    }

    /**
     * Обработчик события закрытия активности.
     */
    @Override
    protected void onStop() {
        // отключаем блокировку выключения экрана
        ViewUtils.setKeepScreenOn(this, false);
        super.onStop();
    }

    public void setEditorProgressVisibility(boolean vis) {
        mHtmlProgressBar.setVisibility(ViewUtils.toVisibility(vis));
    }

    public TetroidEditor getEditor() {
        return mEditor;
    }

    /**
     * Обработчк изменения текста в окне HTML-кода записи.
     */
    private class HtmlEditTextEditTextListener implements TetroidEditText.ITetroidEditTextListener {

        @Override
        public void onAfterTextInited() {
            if (viewModel.isHtmlMode()) {
                setEditorProgressVisibility(false);
            }
        }
        @Override
        public void onAfterTextChanged() {
            if (viewModel.isHtmlMode()) {
                mEditor.setIsEdited();
                viewModel.setEdited(true);
            }
        }
    }
}

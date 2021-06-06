package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.gee12.htmlwysiwygeditor.ActionButton;
import com.gee12.htmlwysiwygeditor.ActionType;
import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.helpers.HtmlHelper;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.views.dialogs.AskDialogs;
import com.gee12.mytetroid.model.TetroidImage;
import com.gee12.mytetroid.utils.ImageUtils;
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor;

import java.util.ArrayList;
import java.util.List;

public class TetroidEditor extends WysiwygEditor {

    boolean mIsCalledHtmlRequest;

    public TetroidEditor(Context context) {
        super(context);
        init();
    }

    public TetroidEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TetroidEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
    }

    public List<ActionButton> getActionButtons() {
        List<ActionButton> buttons = new ArrayList<>();
        for (int i = 0; i < mToolbarActions.getChildCount(); i++) {
            View view = mToolbarActions.getChildAt(i);
            if (view instanceof ActionButton) {
                buttons.add((ActionButton) view);
            }
        }
        return buttons;
    }

    /**
     * Отображение команд панели инструментов исходя из настроек приложения.
     */
/*    @Override
    protected void initToolbarActions() {
        super.initToolbarActions();

        // TODO: реализовать хранение параметров команд в базе данных
//        List<EditorAction> actions = new ArrayList<>();//Database.getEditorActions();
//
//        this.actionButtons = new HashMap<>();
//        for (int i = 0; i < layoutButtons.getChildCount(); i++) {
//            ActionButton button = (ActionButton) layoutButtons.getChildAt(i);
//            initActionButton(button);
//            EditorAction dbAction = actions.get(i);
//            if (dbAction == null)
//                continue;
//            // отображение
//            if (!dbAction.isEnabled()) {
//                button.setVisibility(GONE);
////                layoutButtons.removeView(button);
//            } else {
//                // сортировка
//                if (button.getType() != dbAction.getType()) {
//                    layoutButtons.removeView(button);
//                    layoutButtons.addView(button, dbAction.getOrder());
//                }
//            }
//        }
    }*/

    @Override
    protected void initActionButton(ActionButton button, ActionType type, boolean isCheckable, boolean isPopup) {
        super.initActionButton(button, type, isCheckable, isPopup);
        if ((App.isFreeVersion()
                // TODO: в качестве исключения
                 || type != ActionType.INSERT_VIDEO)
                && !type.isFree()) {
            button.setEnabled(false);
        }
    }

    /**
     * Вызывается перед сохранением текста записи в файл.
     */
    public void beforeSaveAsync(boolean deleteStyleEmpty) {

        if (deleteStyleEmpty) {
            deleteStyleEmpty();
        }

        this.mIsCalledHtmlRequest = true;
        mWebView.makeEditableHtmlRequest();
    }

    /**
     * Специально для совместимости с MyTetroid:
     *
     * удаление стиля "-qt-paragraph-type:empty;" в абзацах "p", содержащих
     * какой-то текст, а не просто элемент "br".
     */
    private void deleteStyleEmpty() {
        String script = "" +
                "var paragraphs = document.getElementsByTagName(\"p\");\n" +
                "for(var i = 0; i < paragraphs.length; i++) {\n" +
                "\tvar elem = paragraphs[i];\n" +
                "\tif (elem.hasAttribute(\"style\")) {\n" +
                "\t\tvar styleAttr = elem.getAttribute(\"style\");\n" +
                "\t\tif (!(elem.children.length == 1 && elem.children[0].tagName == \"BR\")\n" +
                "\t\t\t&& styleAttr.indexOf(\"-qt-paragraph-type:empty;\") > -1) {\n" +
                "\t\t\tvar newStyleAttr = styleAttr.replace(\"-qt-paragraph-type:empty;\",\"\");\n" +
                "\t\t\telem.setAttribute(\"style\", newStyleAttr);\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";
        mWebView.execJavascript(script);
    }

    public void insertImage(TetroidImage image) {
        ImageUtils.setImageDimensions(
                RecordsManager.getPathToRecordFolder(getContext(), image.getRecord()), image);
        showEditImageDialog(image.getName(), image.getWidth(), image.getHeight(), false);
    }

    /**
     * Вставка выбранных изображений.
     * @param images
     */
    public void insertImages(List<TetroidImage> images) {
        if (images == null)
            return;
        int size = images.size();
        if (size > 0) {
            if (size == 1) {
                // выводим диалог установки размера
                insertImage(images.get(0));
            } else {
                // спрашиваем о необходимости изменения размера
                AskDialogs.showYesNoDialog(getContext(), new Dialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        createImageDimensDialog(images, 0);
                    }
                    @Override
                    public void onCancel() {
                        for (TetroidImage fileName : images) {
                            mWebView.insertImage(fileName.getName(), null);
                        }
                    }
                }, R.string.ask_change_image_dimens);
                setIsEdited();
            }
        }
    }

    private void createImageDimensDialog(List<TetroidImage> images, int pos) {
        if (images == null || pos < 0 || pos >= images.size())
            return;
        TetroidImage image = images.get(pos);
        ImageUtils.setImageDimensions(
                RecordsManager.getPathToRecordFolder(getContext(), image.getRecord()), image);
        // выводим диалог установки размера
        boolean isSeveral = (pos < images.size() - 1);
        Dialogs.createImageDimensDialog(getContext(), image.getWidth(), image.getHeight(), isSeveral,
                (width, height, similar) -> {
                    mWebView.insertImage(image.getName(), width, height);
                    if (!similar) {
                        // вновь выводим диалог установки размера
                        createImageDimensDialog(images, pos + 1);
                    } else {
                        // устанавливаем "сохраненный" размер
                        for (int i = pos + 1; i < images.size(); i++) {
                            mWebView.insertImage(images.get(i).getName(), width, height);
                        }
                    }
                });
    }

    public static String getDocumentHtml(String bodyHtml) {
        StringBuilder sb = new StringBuilder(3);
        sb.append(HtmlHelper.HTML_START_WITH);
        sb.append(bodyHtml);
        sb.append(HtmlHelper.HTML_END_WITH);
        return sb.toString();
    }

    public String getDocumentHtml() {
        return getDocumentHtml(mWebView.getEditableHtml());
    }

    public boolean isCalledHtmlRequest() {
        return mIsCalledHtmlRequest;
    }

    public void setHtmlRequestHandled() {
        this.mIsCalledHtmlRequest = false;
    }
}

package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.gee12.htmlwysiwygeditor.ActionButton;
import com.gee12.htmlwysiwygeditor.ActionType;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.TetroidImage;
import com.gee12.mytetroid.utils.ImageUtils;
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor;

import java.util.ArrayList;
import java.util.List;

public class TetroidEditor extends WysiwygEditor {

    public static final String HTML_START_WITH =
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">\n" +
                    "<html><head>" +
                    "<meta name=\"qrichtext\" content=\"1\" />" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" +
                    "<style type=\"text/css\">\n" +
                    "p, li { white-space: pre-wrap; }\n" +
                    "</style></head>" +
                    "<body style=\" font-family:'DejaVu Sans'; font-size:11pt; font-weight:400; font-style:normal;\">";
    public static final String HTML_END_WITH = "</body></html>";

    public TetroidEditor(Context context) {
        super(context);
    }

    public TetroidEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TetroidEditor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public List<ActionButton> getActionButtons() {
        List<ActionButton> buttons = new ArrayList<>();
        for (int i = 0; i < layoutButtons.getChildCount(); i++) {
            View view = layoutButtons.getChildAt(i);
            if (view instanceof ActionButton) {
                buttons.add((ActionButton) view);
            }
        }
        return buttons;
    }

    /**
     * Отображение команд панели инструментов исходя из настроек приложения.
     */
    @Override
    protected void initToolbar() {
        super.initToolbar();

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
    }

    @Override

    protected void initActionButton(ActionButton button, ActionType type, boolean isCheckable, boolean isPopup) {
        super.initActionButton(button, type, isCheckable, isPopup);
        //
        if (!App.isFullVersion() && !type.isFree()) {
            button.setEnabled(false);
        }
    }

    /**
     * Вставка выбранных изображений.
     * @param imagesFileNames
     */
    public void onSelectImages(List<TetroidImage> imagesFileNames) {
        if (imagesFileNames == null)
            return;
        int size = imagesFileNames.size();
        if (size > 0) {
            if (size == 1) {
                // обрабатываем изображение только когда выбран один файл
                TetroidImage image = imagesFileNames.get(0);
                ImageUtils.setImageDimensions(DataManager.getStoragePathBase(), image);
                showEditImageDialog(image.getName(), image.getWidth(), image.getHeight());
            } else {
                for (TetroidImage fileName : imagesFileNames) {
                    webView.insertImage(fileName.getName(), null);
                }
            }
            setIsEdited();
        }
    }

    public static String getDocumentHtml(String bodyHtml) {
        StringBuilder sb = new StringBuilder(3);
        sb.append(HTML_START_WITH);
        sb.append(bodyHtml);
        sb.append(HTML_END_WITH);
        return sb.toString();
    }

    public String getDocumentHtml() {
        return getDocumentHtml(webView.getEditableHtml());
    }
}

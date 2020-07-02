package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.gee12.htmlwysiwygeditor.ActionButton;
import com.gee12.htmlwysiwygeditor.ActionType;
import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.HtmlHelper;
import com.gee12.mytetroid.model.TetroidImage;
import com.gee12.mytetroid.utils.ImageUtils;
import com.lumyjuwon.richwysiwygeditor.WysiwygEditor;

import java.util.ArrayList;
import java.util.List;

public class TetroidEditor extends WysiwygEditor {

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
        for (int i = 0; i < mLayoutButtons.getChildCount(); i++) {
            View view = mLayoutButtons.getChildAt(i);
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
        // TODO: пока отключаем команды независимо от версии
        boolean isFreeVersion = true;//App.isFreeVersion();
        if (isFreeVersion && !type.isFree()) {
            button.setEnabled(false);
        }
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
                TetroidImage image = images.get(0);
                ImageUtils.setImageDimensions(DataManager.getStoragePathBase(), image);
                showEditImageDialog(image.getName(), image.getWidth(), image.getHeight(), false);
            } else {
                // спрашиваем о необходимости изменения размера
                AskDialogs.showYesNoDialog(getContext(), new AskDialogs.IApplyCancelResult() {
                    @Override
                    public void onApply() {
                        createImageDimensDialog(images, 0);
                        /*AtomicBoolean isSimilarParams = new AtomicBoolean(false);
                        int[] lastDimens = new int[2];
                        for (TetroidImage image : images) {
                            if (!isSimilarParams.get()) {
                                // выводим диалог установки размера
                                ImageUtils.setImageDimensions(DataManager.getStoragePathBase(), image);
                                Dialogs.createImageDimensDialog(getContext(), image.getWidth(), image.getHeight(), true,
                                        (width, height, similar) -> {
                                    isSimilarParams.set(similar);
                                    if (similar) {
                                        lastDimens[0] = width;
                                        lastDimens[1] = height;
                                    }
                                    mWebView.insertImage(image.getName(), width, height);
                                });
                            } else {
                                // устанавливаем "сохраненный" размер
                                mWebView.insertImage(image.getName(), lastDimens[0], lastDimens[1]);
                            }
                        }*/
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
        ImageUtils.setImageDimensions(DataManager.getStoragePathBase(), image);
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
}

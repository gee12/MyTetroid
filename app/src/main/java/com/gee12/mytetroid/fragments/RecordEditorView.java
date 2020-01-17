package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.lumyjuwon.richwysiwygeditor.RichWysiwyg;

public class RecordEditorView extends RecordView {

    private RichWysiwyg wysiwyg;

    public RecordEditorView(Context context) {
        super(context);
        initView();
    }

    public RecordEditorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RecordEditorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RecordEditorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        wysiwyg = findViewById(R.id.richwysiwygeditor);

    }

//    @Override
//    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
//        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
//            List<Image> images = ImagePicker.getImages(data);
//            insertImages(images);
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//    private void insertImages(List<Image> images) {
//        if (images == null) return;
//
//        StringBuilder stringBuffer = new StringBuilder();
//        for (int i = 0, l = images.size(); i < l; i++) {
//            stringBuffer.append(images.get(i).getPath()).append("\n");
//            // Handle this
//            wysiwyg.getEditor().insertImage("file://" + images.get(i).getPath(), "A");
//        }
//    }

    @Override
    protected int getViewId() {
        return R.layout.layout_record_editor;
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.layout_record_editor, container, false);
//        setMainView(getArguments());
//
//        return view;
//    }


    @Override
    public void openRecord() {
//        editor.clearAllContents();
        wysiwyg.getEditor().clearAndFocusEditor();
        String textHtml = recordExt.getTextHtml();
        try {
//            editor.render(textHtml);
            wysiwyg.getEditor().setHtml(textHtml);

        } catch (Exception ex) {
            LogManager.addLog("Ошибка отображения записи", ex, Toast.LENGTH_LONG);
        }
//            editor.clearFocus();
    }

    @Override
    public String getRecordHtml() {
//        return null;
//        return editor.getContentAsHTML();
        return wysiwyg.getEditor().getHtml();
    }

    @Override
    public void setFullscreen(boolean isFullscreen) {

    }

//    @Override
//    public String getTitle() {
//        return null;
//    }

}

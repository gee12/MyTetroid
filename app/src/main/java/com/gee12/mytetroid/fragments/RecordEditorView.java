package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.view.View;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidRecordExt;
import com.github.irshulx.Editor;
import com.github.irshulx.models.EditorTextStyle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RecordEditorView extends RecordView {

    private Editor editor;

    public RecordEditorView(Context context) {
        super(context);

        this.editor =  findViewById(R.id.editor);
        initEditor();

        FloatingActionButton fab = findViewById(R.id.button_view_record);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_editor, container, false);
//        setMainView(getArguments());
//
//
//
//        return view;
//    }

    private void initEditor() {
        findViewById(R.id.action_h1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.H1);
            }
        });
    }

    @Override
    public void openRecord(final TetroidRecordExt record) {
//    public void showCurRecord() {
        if (record == null)
//            LogManager.addLog("Текущая запись не установлена", LogManager.Types.ERROR, Toast.LENGTH_LONG);
            return;
        this.recordExt = record;

        editor.render(record.getTextHtml());
    }

    @Override
    public void setFullscreen(boolean isFullscreen) {

    }

//    @Override
//    public String getTitle() {
//        return null;
//    }
}

package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.view.View;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidRecord;
import com.github.irshulx.Editor;
import com.github.irshulx.models.EditorTextStyle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

public class EditorFragment extends RecordFragment {

    private Editor editor;

    public EditorFragment(Context context) {
        super(context);

        this.editor =  findViewById(R.id.editor);
        setUpEditor();

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

    private void setUpEditor() {
        findViewById(R.id.action_h1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.updateTextStyle(EditorTextStyle.H1);
            }
        });
    }

    public void editRecord(@NotNull TetroidRecord record) {

    }

//    @Override
//    public String getTitle() {
//        return null;
//    }
}

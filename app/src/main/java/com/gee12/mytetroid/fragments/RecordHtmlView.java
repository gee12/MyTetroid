package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.view.View;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidRecordExt;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RecordHtmlView extends RecordView {

    public RecordHtmlView(Context context) {
        super(context);

        FloatingActionButton fab = findViewById(R.id.button_edit_record);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    @Override
    protected int getViewId() {
        return R.layout.layout_record_html;
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.layout_record_html, container, false);
//        setMainView(getArguments());
//
//
//        return rootView;
//    }


    /**
     * Отображение записи
     */
    @Override
    public void openRecord(final TetroidRecordExt record) {

    }

    @Override
    public void setFullscreen(boolean isFullscreen) {

    }

//    @Override
//    public String getTitle() {
//        return null;
//    }
}

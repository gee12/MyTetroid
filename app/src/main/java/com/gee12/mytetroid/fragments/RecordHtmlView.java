package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.view.View;

import com.gee12.mytetroid.R;
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

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.fragment_html, container, false);
//        setMainView(getArguments());
//
//
//        return rootView;
//    }

    @Override
    public void setFullscreen(boolean isFullscreen) {

    }

//    @Override
//    public String getTitle() {
//        return null;
//    }
}

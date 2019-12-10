package com.gee12.mytetroid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gee12.mytetroid.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class EditorFragment extends RecordFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_editor, container, false);
        setMainView(getArguments());


        FloatingActionButton fab = rootView.findViewById(R.id.button_view_record);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainView.closeFoundFragment();
            }
        });

        return rootView;
    }


    @Override
    public String getTitle() {
        return null;
    }
}

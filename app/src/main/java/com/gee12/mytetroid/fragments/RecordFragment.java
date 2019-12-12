package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.view.View;

import com.gee12.mytetroid.data.TetroidRecord;

public abstract class RecordFragment extends View {//TetroidFragment {

    protected TetroidRecord record;

    public RecordFragment(Context context) {
        super(context);
    }
}

package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.views.TetroidFragment;

public class FoundPageFragment extends TetroidFragment {

    public FoundPageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_found, container, false);

        return rootView;
    }

    @Override
    public String getTitle() {
        return String.format("Найдено: %d", 42);
    }
}

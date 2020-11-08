package com.gee12.mytetroid.fragments;

import android.os.Bundle;

import com.gee12.mytetroid.R;

public class SettingsFragment extends TetroidSettingsFragment {

    public static final String EXTRA_IS_REINIT_STORAGE = "EXTRA_IS_REINIT_STORAGE";
    public static final String EXTRA_IS_CREATE_STORAGE = "EXTRA_IS_CREATE_STORAGE";
    public static final String EXTRA_IS_LOAD_STORAGE = "EXTRA_IS_LOAD_STORAGE";
    public static final String EXTRA_IS_LOAD_ALL_NODES = "EXTRA_IS_LOAD_ALL_NODES";
    public static final String EXTRA_IS_PASS_CHANGED = "EXTRA_IS_PASS_CHANGED";

    public static final int REQUEST_CODE_OPEN_STORAGE_PATH = 1;
    public static final int REQUEST_CODE_CREATE_STORAGE_PATH = 2;
    public static final int REQUEST_CODE_OPEN_TEMP_PATH = 3;
    public static final int REQUEST_CODE_OPEN_LOG_PATH = 4;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs, rootKey);

        getActivity().setTitle(R.string.title_settings);
    }
}

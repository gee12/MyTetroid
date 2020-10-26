package com.gee12.mytetroid.fragments;

import android.os.Bundle;

import com.gee12.mytetroid.R;

public class SettingsEditFragment extends TetroidSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs_edit, rootKey);

        getActivity().setTitle(R.string.pref_category_edit);
    }
}

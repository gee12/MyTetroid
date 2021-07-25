package com.gee12.mytetroid.views.fragments.settings;

import android.os.Bundle;

import com.gee12.mytetroid.R;

public class SettingsFragment extends TetroidSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs, rootKey);

        getActivity().setTitle(R.string.title_settings);
    }
}

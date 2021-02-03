package com.gee12.mytetroid.fragments.settings;

import android.os.Bundle;

import com.gee12.mytetroid.R;

public class SettingsHandlingFragment extends TetroidSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.prefs_handling, rootKey);

        getActivity().setTitle(R.string.pref_category_handling);
    }
}

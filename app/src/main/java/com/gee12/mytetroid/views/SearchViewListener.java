package com.gee12.mytetroid.views;

import android.widget.SearchView;

public abstract class SearchViewListener {

    public SearchViewListener(SearchView searchView) {
        searchView.setOnCloseListener(() -> {
            OnClose();
            return false;
        });
        searchView.setOnSearchClickListener(v -> OnSearch());
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onQuerySubmit(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    public abstract void OnClose();
    public abstract void OnSearch();
    public abstract void onQuerySubmit(String query);
}

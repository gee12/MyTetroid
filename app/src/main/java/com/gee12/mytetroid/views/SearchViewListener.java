package com.gee12.mytetroid.views;

import android.widget.SearchView;

public abstract class SearchViewListener {

    public SearchViewListener(SearchView searchView) {
        searchView.setOnSearchClickListener(v -> onSearch());
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
        searchView.setOnCloseListener(() -> {
            onClose();
            return false;
        });
    }

    public abstract void onSearch();
    public abstract void onQuerySubmit(String query);
    public abstract void onClose();
}

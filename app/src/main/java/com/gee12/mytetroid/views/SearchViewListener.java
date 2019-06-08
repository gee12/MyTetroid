package com.gee12.mytetroid.views;

import android.view.View;
import android.widget.SearchView;

public abstract class SearchViewListener {
    private SearchView searchView;

    public SearchViewListener(SearchView searchView) {
        this.searchView = searchView;
        searchView.setOnCloseListener(new android.widget.SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                OnClose();
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnSearch();
            }
        });
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

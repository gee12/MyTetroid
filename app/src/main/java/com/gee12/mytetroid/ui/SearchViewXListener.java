package com.gee12.mytetroid.ui;


import android.database.Cursor;

import androidx.appcompat.widget.SearchView;

public abstract class SearchViewXListener {

    public SearchViewXListener(SearchView searchView) {
        searchView.setOnSearchClickListener(v -> onSearchClick());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onQuerySubmit(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onQueryChange(newText);
                return true;
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                Cursor searchCursor = searchView.getSuggestionsAdapter().getCursor();
                if (searchCursor.moveToPosition(position)) {
                    String query = searchCursor.getString(2);
                    onSuggestionSelectOrClick(query);
                }
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor searchCursor = searchView.getSuggestionsAdapter().getCursor();
                if (searchCursor.moveToPosition(position)) {
                    String query = searchCursor.getString(2);
                    onSuggestionSelectOrClick(query);
                }
                return true;
            }
        });
        searchView.setOnCloseListener(() -> {
            onClose();
            return false;
        });
    }

    public abstract void onSearchClick();
    public abstract void onQuerySubmit(String query);
    public abstract void onQueryChange(String query);
    public abstract void onSuggestionSelectOrClick(String query);
    public abstract void onClose();
}

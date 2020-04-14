package com.gee12.mytetroid.views;


import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.CursorAdapter;

public abstract class SearchViewXListener {

    public SearchViewXListener(SearchView searchView) {
        searchView.setOnSearchClickListener(v -> onSearch());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onQuerySubmit(query);
//                return false;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                CursorAdapter cursor = (CursorAdapter) searchView.getSuggestionsAdapter().getItem(position);
                onSuggestionSelectOrClick(cursor.toString());
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                CursorAdapter cursor = (CursorAdapter) searchView.getSuggestionsAdapter().getItem(position);
                onSuggestionSelectOrClick(cursor.toString());
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
    public abstract void onSuggestionSelectOrClick(String query);
    public abstract void onClose();
}

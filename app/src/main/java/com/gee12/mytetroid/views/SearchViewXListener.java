package com.gee12.mytetroid.views;


import androidx.appcompat.widget.SearchView;

public abstract class SearchViewXListener {

    public SearchViewXListener(SearchView searchView) {
        searchView.setOnSearchClickListener(v -> onSearch());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                onSuggestionSelectOrClick(position);
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                onSuggestionSelectOrClick(position);
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
    public abstract void onSuggestionSelectOrClick(int position);
    public abstract void onClose();
}

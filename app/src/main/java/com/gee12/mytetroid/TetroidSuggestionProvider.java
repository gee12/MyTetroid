package com.gee12.mytetroid;
import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

public class TetroidSuggestionProvider extends SearchRecentSuggestionsProvider {

    public final static String AUTHORITY = "com.gee12.TetroidSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public TetroidSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    public static void SaveRecentQuery(Context context, String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
                TetroidSuggestionProvider.AUTHORITY, TetroidSuggestionProvider.MODE);
        suggestions.saveRecentQuery(query, null);
    }
}
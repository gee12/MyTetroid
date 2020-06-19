package com.gee12.mytetroid;
import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

public class TetroidSuggestionProvider extends SearchRecentSuggestionsProvider {

//    public final static String AUTHORITY = "com.gee12.TetroidSuggestionProvider";
    public final static String AUTHORITY = BuildConfig.APPLICATION_ID + ".TetroidSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;


    public TetroidSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    /**
     * Сохранить запрос в системной бд.
     * @param context
     * @param query
     */
    public static void saveRecentQuery(Context context, String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.saveRecentQuery(query, null);
    }

    /**
     * Очистить историю поиска.
     * @param context
     */
    public static void clearHistory(Context context) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context, AUTHORITY, MODE);
        suggestions.clearHistory();
    }
}
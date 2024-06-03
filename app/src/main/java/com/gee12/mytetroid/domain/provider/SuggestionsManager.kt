package com.gee12.mytetroid.domain.provider

import android.content.Context
import android.content.SearchRecentSuggestionsProvider
import android.provider.SearchRecentSuggestions
import com.gee12.mytetroid.BuildConfig

private const val AUTHORITY = BuildConfig.APPLICATION_ID + ".TetroidSuggestionProvider"
private const val MODE = SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES

class SuggestionsProvider : SearchRecentSuggestionsProvider() {

    init {
        setupSuggestions(AUTHORITY, MODE)
    }

}

class SuggestionsManager(
    private val context: Context,
) {

    /**
     * Сохранить запрос в системной бд.
     */
    fun saveRecentQuery(query: String) {
        val suggestions = SearchRecentSuggestions(context, AUTHORITY, MODE)
        suggestions.saveRecentQuery(query, null)
    }

    /**
     * Очистить историю поиска.
     */
    fun clearHistory() {
        val suggestions = SearchRecentSuggestions(context, AUTHORITY, MODE)
        suggestions.clearHistory()
    }
}
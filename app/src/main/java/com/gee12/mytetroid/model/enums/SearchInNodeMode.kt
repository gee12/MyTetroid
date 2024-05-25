package com.gee12.mytetroid.model.enums

enum class SearchInNodeMode(val id: Int) {
    NONE(0),
    IN_CURRENT_NODE(1),
    IN_SELECTED_NODE(2);

    companion object {
        fun getById(id: Int): SearchInNodeMode? {
            return values().firstOrNull { it.id == id }
        }
    }
}
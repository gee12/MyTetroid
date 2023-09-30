package com.gee12.mytetroid.ui.main


enum class PageType(val index: Int) {
    MAIN(0),
    FOUND(1);

    companion object {
        fun fromIndex(index: Int): PageType? {
            return values().firstOrNull { it.index == index }
        }
    }
}
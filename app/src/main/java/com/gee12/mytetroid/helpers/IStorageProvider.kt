package com.gee12.mytetroid.helpers

import com.gee12.mytetroid.model.TetroidStorage

interface IStorageProvider {
    fun getStorageOrNull(): TetroidStorage?
}
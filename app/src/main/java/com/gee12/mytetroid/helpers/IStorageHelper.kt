package com.gee12.mytetroid.helpers

interface IStorageHelper {
    fun getStorageId(): Int
    fun createDefaultNode(): Boolean
    fun onBeforeStorageTreeSave()
    fun onStorageTreeSaved()
}
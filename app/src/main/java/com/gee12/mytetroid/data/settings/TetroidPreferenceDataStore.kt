package com.gee12.mytetroid.data.settings

import androidx.preference.PreferenceDataStore

class TetroidPreferenceDataStore(val callback: IDataStore) : PreferenceDataStore() {

    interface IDataStore {
        fun saveValue(key: String, value: Any?)
        fun getValue(key: String): Any?
    }

    override fun putString(key: String, value: String?) {
        callback.saveValue(key, value)
    }

    override fun putInt(key: String, value: Int) {
        callback.saveValue(key, value)
    }

    override fun putLong(key: String, value: Long) {
        callback.saveValue(key, value)
    }

    override fun putFloat(key: String, value: Float) {
        callback.saveValue(key, value)
    }

    override fun putBoolean(key: String, value: Boolean) {
        callback.saveValue(key, value)
    }

    override fun getString(key: String, defValue: String?): String? {
        return callback.getValue(key)?.toString()
    }

    override fun getInt(key: String, defValue: Int): Int {
        return callback.getValue(key)?.toString()?.toInt() ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return callback.getValue(key)?.toString()?.toLong() ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return callback.getValue(key)?.toString()?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return callback.getValue(key)?.toString()?.toBoolean() ?: defValue
    }
}
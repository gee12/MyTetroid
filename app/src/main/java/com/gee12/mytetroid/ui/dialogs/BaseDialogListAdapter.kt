package com.gee12.mytetroid.ui.dialogs

import android.widget.BaseAdapter

abstract class BaseDialogListAdapter<T>(
    private val data: List<T>,
    protected var selectedItem: T? = null,
) : BaseAdapter() {

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): T {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return data[position].hashCode().toLong()
    }

}

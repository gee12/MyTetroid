package com.gee12.mytetroid.ui.dialogs

import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R

/**
 * Диалог со списком.
 */
open class ListDialog<T>(
    private val title: String?,
    private val tag: String,
) : BaseDialogFragment() {

    lateinit var adapter: BaseDialogListAdapter<T>

    override fun getRequiredTag() = tag

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_list_view

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(title)

        val listView = view.findViewById<ListView>(R.id.list_view)
        listView.adapter = adapter
    }

}
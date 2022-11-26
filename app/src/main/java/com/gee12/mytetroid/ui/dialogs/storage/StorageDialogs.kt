package com.gee12.mytetroid.ui.dialogs.storage

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.ui.adapters.StorageChooserAdapter
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyResult
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.toApplyCancelResult
import com.gee12.mytetroid.ui.dialogs.AskDialogs

object StorageDialogs {

    /**
     * Диалог со списком вариантов указания хранилища.
     */
    fun createStorageSelectionDialog(
        context: Context,
        onItemClick: (isNew: Boolean) -> Unit,
    ) {
        val builder = AskDialogBuilder.create(context, R.layout.dialog_list_view)
//        builder.setTitle("Выберите действие");
        val dialog = builder.create()
        val listView = builder.view.findViewById<ListView>(R.id.list_view)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            onItemClick(position == 1)
            dialog.cancel()
        }
        listView.adapter = StorageChooserAdapter(context)
        dialog.show()
    }

    fun askForDefaultStorageNotSpecified(context: Context, callback: IApplyResult?) {
        AskDialogs.showYesNoDialog(
            context,
            callback?.toApplyCancelResult(),
            R.string.ask_no_set_default_storage
        )
    }
}
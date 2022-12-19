package com.gee12.mytetroid.ui.dialogs.storage

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import com.gee12.htmlwysiwygeditor.Dialogs.AskDialogBuilder
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.storages.StorageChooserAdapter
import com.gee12.mytetroid.ui.dialogs.AskDialogs

object StorageDialogs {

    /**
     * Диалог со списком вариантов указания хранилища.
     */
    // TODO: DialogFragment
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

    fun showReloadStorageDialog(
        context: Context,
        toCreate: Boolean,
        pathChanged: Boolean,
        onApply: () -> Unit,
    ) {
        AskDialogs.showYesDialog(
            context = context,
            messageResId = when {
                toCreate -> R.string.ask_create_storage_in_folder
                pathChanged -> R.string.ask_storage_path_was_changed
                else -> R.string.ask_reload_storage
            },
            onApply = onApply,
        )
    }

}
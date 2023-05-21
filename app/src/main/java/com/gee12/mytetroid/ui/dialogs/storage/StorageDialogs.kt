package com.gee12.mytetroid.ui.dialogs.storage

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.dialogs.AskDialogs

object StorageDialogs {

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
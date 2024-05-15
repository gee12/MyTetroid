package com.gee12.mytetroid.ui.dialogs.script

import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.enums.DefaultScript
import com.gee12.mytetroid.ui.dialogs.BaseDialogFragment

/**
 * Диалог со списком стандартных скриптов.
 */
class DefaultScriptsDialog(
    private val resourcesProvider: IResourcesProvider,
    private val onItemClick: (DefaultScript) -> Unit,
) : BaseDialogFragment() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_list_view

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(R.string.title_default_scripts)

        val listView = view.findViewById<ListView>(R.id.list_view)

        val values = DefaultScript.values().toList()
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position: Int, _ ->
            onItemClick(values[position])
            dialog.cancel()
        }
        listView.adapter = DefaultScriptsAdapter(
            context = requireContext(),
            resourcesProvider = resourcesProvider,
            data = values,
        )
    }

    companion object {
        const val TAG = "DefaultScriptsDialog"
    }
}
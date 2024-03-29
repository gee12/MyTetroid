package com.gee12.mytetroid.ui.dialogs

import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.ReceivedData
import com.gee12.mytetroid.ui.IntentsAdapter
import com.gee12.mytetroid.ui.storage.StorageViewModel

/**
 * Диалог со списком вариантов обработки переданного объекта.
 */
class IntentDialog(
    private val resourcesProvider: IResourcesProvider,
    private val isText: Boolean,
    private val onItemClick: (item: ReceivedData) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_list_view

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        //builder.setTitle("Выберите действие");
        val listView = view.findViewById<ListView>(R.id.list_view)
        val dataSet = if (isText) {
            ReceivedData.textIntents()
        } else {
            ReceivedData.imageIntents()
        }
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position: Int, _ ->
            onItemClick(dataSet[position])
            dialog.cancel()
        }
        listView.adapter = IntentsAdapter(
            context = requireContext(),
            resourcesProvider = resourcesProvider,
            dataSet = dataSet,
        )
    }

    companion object {
        const val TAG = "IntentDialog"
    }
}
package com.gee12.mytetroid.ui.dialogs.history

import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.enums.HistoryWriteMode
import com.gee12.mytetroid.ui.dialogs.BaseDialogFragment

/**
 * Диалог выбора режима.
 */
class SelectHistoryWriteModeDialog(
    private val settingsManager: CommonSettingsManager,
    private val resourcesProvider: IResourcesProvider,
) : BaseDialogFragment() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_list_view

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(R.string.pref_title_history_write_mode)

        val listView = view.findViewById<ListView>(R.id.list_view)
        listView.adapter = HistoryWriteModesAdapter(
            context = requireContext(),
            resourcesProvider = resourcesProvider,
            selectedItem = settingsManager.getHistoryWriteMode(),
            data = HistoryWriteMode.values().toList(),
            onItemClickListener = { item, _ ->
                settingsManager.setHistoryWriteMode(item)
                dialog.dismiss()
            }
        )
    }

    companion object {
        const val TAG = "DefaultScriptsDialog"
    }
}
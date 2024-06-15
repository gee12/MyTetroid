package com.gee12.mytetroid.ui.dialogs.image

import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.enums.ImagesSaveMode
import com.gee12.mytetroid.ui.dialogs.BaseDialogFragment
import com.gee12.mytetroid.ui.dialogs.DialogRadioListAdapter

/**
 * Диалог выбора режима сохранения изображений.
 */
class ImagesSaveModeDialog(
    private val settingsManager: CommonSettingsManager,
    private val resourcesProvider: IResourcesProvider,
) : BaseDialogFragment() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_list_view

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(R.string.pref_title_images_save_mode)

        val listView = view.findViewById<ListView>(R.id.list_view)
        listView.adapter = DialogRadioListAdapter(
            context = requireContext(),
            selectedItem = settingsManager.getImagesSaveMode(),
            data = ImagesSaveMode.values().toList(),
            onItemClickListener = { item, _ ->
                settingsManager.setImagesSaveMode(item)
                dialog.dismiss()
            },
            getTitle = { item ->
                item.getTitle(resourcesProvider)
            },
            getDescription = { item ->
                item.getDescription(resourcesProvider)
            },
        )
    }

    companion object {
        const val TAG = "ImagesSaveModeDialog"
    }
}
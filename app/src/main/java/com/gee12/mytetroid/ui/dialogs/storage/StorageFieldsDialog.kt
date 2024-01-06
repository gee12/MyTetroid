package com.gee12.mytetroid.ui.dialogs.storage

import android.content.DialogInterface
import android.net.Uri
import android.text.InputType
import android.view.View
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.common.extensions.uriToAbsolutePathIfPossible
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel

class StorageFieldsDialog(
    override var storageId: Int? = null,
    private val isNew: Boolean,
    private val isDefault: Boolean? = null,
    private val onApply: (storage: TetroidStorage) -> Unit,
    private val onPickStorageFolder: (currentFolderUri: Uri?) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    override var isInitCurrentStorage: Boolean = false

    private lateinit var etPath: EditText
    private lateinit var etName: EditText
    private lateinit var cbIsDefault: CheckedTextView
    private var isPathSelected = false
    private var storageFolder: DocumentFile? = null

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_storage

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(if (isNew) R.string.title_create_new_storage else R.string.title_add_existing_storage)

        etPath = view.findViewById(R.id.edit_text_path)
        etPath.inputType = InputType.TYPE_NULL
        val bPath = view.findViewById<ImageButton>(R.id.button_path)
        etName = view.findViewById(R.id.edit_text_name)
        cbIsDefault = view.findViewById(R.id.check_box_is_default)
        cbIsDefault.setOnClickListener { cbIsDefault.isChecked = !cbIsDefault.isChecked }
        val cbReadOnly = view.findViewById<CheckedTextView>(R.id.check_box_read_only)
        cbReadOnly.isVisible = !isNew
        // TODO: принудительно отключаем (пока)
        cbReadOnly.isEnabled = false

        cbIsDefault.isChecked = isDefault ?: false

        val clickListener = View.OnClickListener {
            onPickStorageFolder(storageFolder?.uri)
        }
        bPath.setOnClickListener(clickListener)
        etPath.setOnClickListener(clickListener)

        // кнопки результата
        setPositiveButton(R.string.answer_ok) { _: DialogInterface?, _: Int ->
            storageFolder?.also { folder ->
                val storage = TetroidStorage(
                    name = etName.text.toString(),
                    uri = folder.uri.toString(),
                    isDefault = cbIsDefault.isChecked,
                    isReadOnly = cbReadOnly.isChecked,
                    isNew = isNew
                )
                onApply(storage)
            }
        }
        setNegativeButton(R.string.answer_cancel)

        etName.addAfterTextChangedListener { checkPositiveButtonIsEnabled() }
    }

    override fun onStorageInited(storage: TetroidStorage) {
        if (storage.uri.isNotEmpty()) {
            val path = storage.uri.uriToAbsolutePathIfPossible(requireContext())
            etPath.setText(path)
            etPath.textSize = 14f
            isPathSelected = true
        }
        etName.setText(storage.name)
        cbIsDefault.isChecked = isDefault ?: storage.isDefault

        etName.setSelectionAtEnd()
        showKeyboard(etName)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        checkPositiveButtonIsEnabled()
    }

    private fun checkPositiveButtonIsEnabled() {
        getPositiveButton()?.isEnabled = etName.text.isNotEmpty() && isPathSelected
    }

    fun setStorageFolder(folder: DocumentFile) {
        this.storageFolder = folder
        val path = folder.getAbsolutePath(requireContext())
        isPathSelected = true
        etPath.setText(path)
        etName.setSelectionAtEnd()
        etPath.textSize = 14f

        val folderName = folder.name
        if (etName.text.isNullOrEmpty()) {
            etName.setText(folderName)
        }
        getPositiveButton()?.isEnabled = !etName.text.isNullOrEmpty()
    }

    companion object {
        const val TAG = "StorageDialog"
    }
}
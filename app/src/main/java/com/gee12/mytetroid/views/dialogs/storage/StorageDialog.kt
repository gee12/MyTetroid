package com.gee12.mytetroid.views.dialogs.storage

import android.content.DialogInterface
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment
import java.io.File

class StorageDialog(
    private val storage: TetroidStorage?,
    private val callback: IStorageResult
) : TetroidDialogFragment<StorageViewModel>() {

    interface IStorageResult {
        fun onApply(storage: TetroidStorage)
        fun onSelectPath(path: String)
    }

    private lateinit var etPath: EditText
    private lateinit var etName: EditText
    private var isPathSelected = false
    private var isNew = false

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = R.layout.dialog_storage

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(if (storage != null) R.string.title_edit_storage else R.string.title_add_storage)

        etPath = view.findViewById(R.id.edit_text_path)
        etPath.inputType = InputType.TYPE_NULL
        val bPath = view.findViewById<ImageButton>(R.id.button_path)
        etName = view.findViewById(R.id.edit_text_name)
        val cbIsDefault = view.findViewById<CheckedTextView>(R.id.check_box_is_default)
        cbIsDefault.setOnClickListener { cbIsDefault.isChecked = !cbIsDefault.isChecked }
        val cbReadOnly = view.findViewById<CheckedTextView>(R.id.check_box_read_only)
        // TODO: принудительно отключаем (пока)
        cbReadOnly.isEnabled = false

        if (storage != null) {
            if (!TextUtils.isEmpty(storage.path)) {
                etPath.setText(storage.path)
                etPath.textSize = 14f
                isPathSelected = true
            }
            etName.setText(storage.name)
            cbIsDefault.isChecked = storage.isDefault
        }

        val clickListener = View.OnClickListener { callback.onSelectPath(etPath.text.toString()) }
        bPath.setOnClickListener(clickListener)
        etPath.setOnClickListener(clickListener)

        // кнопки результата
        setPositiveButton(R.string.answer_ok) { _: DialogInterface?, _: Int ->
            val result = TetroidStorage(
                etName.text.toString(),
                etPath.text.toString(),
                cbIsDefault.isChecked,
                cbReadOnly.isChecked,
                isNew
            )
            callback.onApply(result)
        }
        setNegativeButton(R.string.answer_cancel)

        etName.addAfterTextChangedListener { s ->
            getPositiveButton()?.isEnabled = s.isNotEmpty() && isPathSelected
        }

        showKeyboard()
        etName.setSelectionAtEnd()
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = etName.text.isNotEmpty() && isPathSelected
    }

    fun setPath(path: String?, isNew: Boolean) {
        if (TextUtils.isEmpty(path)) {
            return
        }
        this.isNew = isNew
        isPathSelected = true
        etPath.setText(path)
        etName.setSelectionAtEnd()
        etPath.textSize = 14f
        val folderName = File(path!!).name
        etName.setText(folderName)
        getPositiveButton()?.isEnabled = !TextUtils.isEmpty(path) && !TextUtils.isEmpty(folderName)
    }

    companion object {
        const val TAG = "StorageDialog"
    }
}
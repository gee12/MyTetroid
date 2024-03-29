package com.gee12.mytetroid.ui.dialogs.attach

import android.annotation.SuppressLint
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import java.util.*
import kotlin.math.abs

class AttachFieldsDialog(
    private val file: TetroidFile?,
    private val onApply: (name: String) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    private lateinit var etName: EditText

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_attach

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        etName = view.findViewById(R.id.edit_text_name)

        @SuppressLint("SetTextI18n")
        if (BuildConfig.DEBUG && file == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etName.setText("file $num.test")
        }

        if (file != null) {
            etName.setText(file.name)
        }

        setPositiveButton(R.string.answer_ok) { _, _ -> onApply(etName.text.toString()) }
        setNegativeButton(R.string.answer_cancel)

        etName.addAfterTextChangedListener { checkPositiveButtonIsEnabled() }

        etName.setSelectionAtEnd()
        showKeyboard(etName)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        checkPositiveButtonIsEnabled()
    }

    private fun checkPositiveButtonIsEnabled() {
        getPositiveButton()?.isEnabled = etName.text.isNotEmpty()
    }

    companion object {
        const val TAG = "AttachFieldsDialog"
    }

}
package com.gee12.mytetroid.ui.dialogs.pin

import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.htmlwysiwygeditor.TextChangedListener
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class PinCodeLengthDialog(
    private val curSize: Int,
    private val minSize: Int,
    private val maxSize: Int,
    private val callback: IPinLengthInputResult
) : TetroidStorageDialogFragment<StorageViewModel>() {

    interface IPinLengthInputResult {
        fun onApply(length: Int)
        fun onCancel()
    }

    private lateinit var etSize: EditText

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_pin_code_length

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setCanceledOnTouchOutside(false)
        setTitle(R.string.title_enter_pin_code_length)

        view.findViewById<TextInputLayout>(R.id.text_input_layout_size).also {
            it.hint = getString(R.string.hint_pin_code_size_mask, minSize, maxSize)
        }
        etSize = view.findViewById(R.id.edit_text_size)
        if (curSize in minSize..maxSize) {
            etSize.setText("%d".format(Locale.getDefault(), curSize))
        }

        setPositiveButton(R.string.answer_ok) { _, _ ->
            val stringValue = etSize.text.toString()
            val size = stringValue.toIntOrNull()
            if (size != null && size >= minSize && size <= maxSize) {
                callback.onApply(size)
            } else {
                viewModel.showMessage(getString(R.string.invalid_number_mask, stringValue))
            }
        }
        setNegativeButton(R.string.answer_cancel) { _, _ -> callback.onCancel() }

        etSize.setSelectionAtEnd()
        showKeyboard(etSize)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = etSize.text.isNotEmpty()

        etSize.addTextChangedListener(TextChangedListener {
            val size = etSize.text.toString().toIntOrNull()
            getPositiveButton()?.isEnabled = (size != null && size >= minSize && size <= maxSize)
        })
    }

    companion object {
        const val TAG = "PinCodeLengthDialog"
    }
}
package com.gee12.mytetroid.views.dialogs.pin

import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.htmlwysiwygeditor.ViewUtils.TextChangedListener
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment
import com.lumyjuwon.richwysiwygeditor.RichEditor.Utils
import java.util.*

class PinCodeLengthDialog(
    private val curSize: Int,
    private val callback: IPinLengthInputResult
) : TetroidDialogFragment<StorageViewModel>() {

    interface IPinLengthInputResult {
        fun onApply(length: Int)
        fun onCancel()
    }

    lateinit var etSize: EditText

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = R.layout.dialog_pin_code_length

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setCanceledOnTouchOutside(false)
        setTitle(R.string.title_enter_pin_code_length)

        etSize = view.findViewById(R.id.edit_text_size)
        if (curSize in 4..8) {
            etSize.setText(String.format(Locale.getDefault(), "%d", curSize))
        }

        setPositiveButton(R.string.answer_ok) { _, _ ->
            val s = etSize.text.toString()
            val size = Utils.parseInt(s)
            if (size != null) {
                callback.onApply(size)
            } else {
                viewModel.showMessage(getString(R.string.invalid_number) + s)
            }
        }
        setNegativeButton(R.string.answer_cancel) { _, _ -> callback.onCancel() }

        showKeyboard()
        etSize.setSelectionAtEnd()
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = etSize.text.isNotEmpty()

        etSize.addTextChangedListener(TextChangedListener {
            val size = Utils.parseInt(etSize.text.toString())
            getPositiveButton()?.isEnabled = size != null && size >= 4 && size <= 8
        })
    }

    companion object {
        const val TAG = "PinCodeLengthDialog"
    }
}
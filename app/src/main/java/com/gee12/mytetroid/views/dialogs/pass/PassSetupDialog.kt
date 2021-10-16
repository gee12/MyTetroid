package com.gee12.mytetroid.views.dialogs.pass

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.htmlwysiwygeditor.ViewUtils.TextChangedListener
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.utils.ViewUtils
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment

class PassSetupDialog(
    private val node: TetroidNode?,
    private val callback: PassDialogs.IPassInputResult
) : TetroidDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = R.layout.dialog_pass_setup

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setCanceledOnTouchOutside(false)
        setTitle(R.string.title_password_set)

        val tvPass = dialogView.findViewById<EditText>(R.id.edit_text_pass)
        val tvConfirmPass = dialogView.findViewById<EditText>(R.id.edit_text_confirm_pass)

        // проверка на пустоту паролей
        val listener = TextChangedListener { newText: String? ->
            getPositiveButton()?.isEnabled = !(TextUtils.isEmpty(newText)
                    || tvPass.text.isEmpty() || tvConfirmPass.text.isEmpty())
        }
        tvPass.addTextChangedListener(listener)
        tvConfirmPass.addTextChangedListener(listener)

        setPositiveButton(R.string.answer_ok) { _ , _ ->
            val pass = tvPass.text.toString()
            val confirmPass = tvConfirmPass.text.toString()
            // проверка совпадения паролей
            if (!pass.contentEquals(confirmPass)) {
                callback.applyPass(pass, node)
                dialog.dismiss()
            } else {
                viewModel.showMessage(getString(R.string.log_pass_confirm_not_match))
            }
        }
        setNegativeButton(R.string.answer_cancel)

        showKeyboard()
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = false
    }

    companion object {
        const val TAG = "PassSetupDialog"
    }
}
package com.gee12.mytetroid.views.dialogs.pass

import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.htmlwysiwygeditor.ViewUtils.TextChangedListener
import com.gee12.mytetroid.R
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment
import org.koin.java.KoinJavaComponent.get

class PassSetupDialog(
    private val callback: PassDialogs.IPassInputResult
) : TetroidDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_pass_setup

    override fun createViewModel() {
        this.viewModel = get(StorageViewModel::class.java)
    }

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

        setPositiveButton(R.string.answer_ok, isCloseDialog = false) { _ , _ ->
            val pass = tvPass.text.toString()
            val confirmPass = tvConfirmPass.text.toString()
            // проверка совпадения паролей
            if (pass.contentEquals(confirmPass)) {
                callback.applyPass(pass/*, node*/)
                dialog.dismiss()
            } else {
                viewModel.showMessage(R.string.log_pass_confirm_not_match)
            }
        }
        setNegativeButton(R.string.answer_cancel)

        showKeyboard(tvPass)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = false
    }

    companion object {
        const val TAG = "PassSetupDialog"
    }
}
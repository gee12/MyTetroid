package com.gee12.mytetroid.ui.dialogs.pass

import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.htmlwysiwygeditor.ViewUtils.TextChangedListener
import com.gee12.mytetroid.R
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.ui.dialogs.TetroidDialogFragment
import org.koin.java.KoinJavaComponent.get

class PassChangeDialog(
    private val onApplyPass: (curPass: String, newPass: String) -> Boolean,
) : TetroidDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_pass_change

    override fun createViewModel() {
        this.viewModel = get(StorageViewModel::class.java)
    }

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setCanceledOnTouchOutside(false)
        setTitle(R.string.title_password_change)

        val tvCurPass = dialogView.findViewById<EditText>(R.id.edit_text_cur_pass)
        val tvNewPass = dialogView.findViewById<EditText>(R.id.edit_text_new_pass)
        val tvConfirmPass = dialogView.findViewById<EditText>(R.id.edit_text_confirm_pass)

        // проверка на пустоту паролей
        val listener = TextChangedListener { newText: String? ->
            getPositiveButton()?.isEnabled = !(TextUtils.isEmpty(newText)
                    || tvCurPass.text.isEmpty() || tvNewPass.text.isEmpty() || tvConfirmPass.text.isEmpty())
        }
        tvCurPass.addTextChangedListener(listener)
        tvNewPass.addTextChangedListener(listener)
        tvConfirmPass.addTextChangedListener(listener)

        setPositiveButton(R.string.answer_ok, isCloseDialog = false) { _, _ ->
            val curPass = tvCurPass.text.toString()
            val newPass = tvNewPass.text.toString()
            val confirmPass = tvConfirmPass.text.toString()
            // проверка совпадения паролей
            if (newPass.contentEquals(confirmPass)) {
                // проверка текущего пароля
                if (onApplyPass(curPass, newPass)) {
                    dialog.dismiss()
                }
            } else {
                viewModel.showMessage(R.string.log_pass_confirm_not_match)
            }
        }
        setNegativeButton(R.string.answer_cancel)

        showKeyboard(tvCurPass)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = false
    }

    companion object {
        const val TAG = "PassChangeDialog"
    }
}
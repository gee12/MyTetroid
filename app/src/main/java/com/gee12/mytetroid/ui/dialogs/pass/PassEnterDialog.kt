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

class PassEnterDialog(
    private val callback: PassDialogs.IPassInputResult
) : TetroidDialogFragment<StorageViewModel>() {

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_pass_enter

    override fun createViewModel() {
        this.viewModel = get(StorageViewModel::class.java)
    }

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setCanceledOnTouchOutside(false)
        setTitle(R.string.title_password_enter)

        val tvPass = view.findViewById<EditText>(R.id.edit_text_pass)
        setPositiveButton(R.string.answer_ok) { _, _ -> callback.applyPass(tvPass.text.toString()/*, node*/) }
        setNegativeButton(R.string.answer_cancel) { _, _ -> callback.cancelPass() }

        // проверка на пустоту пароля
        tvPass.addTextChangedListener(TextChangedListener { newText: String? ->
            getPositiveButton()?.isEnabled = !TextUtils.isEmpty(newText)
        })

        showKeyboard(tvPass)
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = false
    }

    companion object {
        const val TAG = "PassEnterDialog"
    }
}
package com.gee12.mytetroid.views.dialogs.attach

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.views.dialogs.TetroidDialogFragment
import com.lumyjuwon.richwysiwygeditor.Utils.Keyboard
import java.util.*
import kotlin.math.abs

class AttachFieldsDialog(
    private val file: TetroidFile?,
    private val callback: IFileFieldsResult
) : TetroidDialogFragment<StorageViewModel>() {

    interface IFileFieldsResult {
        fun onApply(name: String)
    }

    private lateinit var etName: EditText

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun getLayoutResourceId() = R.layout.dialog_attach

    companion object {
        const val TAG = "AttachFieldsDialog"
    }

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        etName = view.findViewById(R.id.edit_text_name)

        if (BuildConfig.DEBUG && file == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etName.setText("file $num.test")
        }

        if (file != null) {
            etName.setText(file.name)
        }

        setPositiveButton(R.string.answer_ok) { _, _ -> callback.onApply(etName.text.toString()) }
        setNegativeButton(R.string.answer_cancel)

        etName.addAfterTextChangedListener { s ->
            getPositiveButton()?.isEnabled = s.isNotEmpty()
        }

        showKeyboard()
        etName.setSelectionAtEnd()
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        getPositiveButton()?.isEnabled = etName.text.isNotEmpty()
    }
}
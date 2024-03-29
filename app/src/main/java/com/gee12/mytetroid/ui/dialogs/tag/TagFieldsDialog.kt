package com.gee12.mytetroid.ui.dialogs.tag

import android.annotation.SuppressLint
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.setSelectionAtEnd
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel
import java.util.*
import kotlin.math.abs

class TagFieldsDialog(
    private val tag: TetroidTag?,
    private val onApply: (name: String) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    private lateinit var etName: EditText

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_tag

    override fun getViewModelClazz() = StorageViewModel::class.java

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        etName = view.findViewById(R.id.edit_text_name)

        @SuppressLint("SetTextI18n")
        if (BuildConfig.DEBUG && tag == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etName.setText("tag $num.test")
        }

        if (tag != null) {
            etName.setText(tag.name.lowercase(Locale.getDefault()))
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
        const val TAG = "TagFieldsDialog"
    }

}
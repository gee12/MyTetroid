package com.gee12.mytetroid.ui.dialogs.script

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.common.extensions.withIo
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.abs

/**
 * Диалог создания/изменения скрипта.
 */
class ScriptFieldsDialog(
    private val script: TetroidScript?,
    private val scriptText: String?,
    override var storageId: Int?,
    private val onApply: (name: String, fileName: String, description: String, text: String) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    private lateinit var scriptsManager: ScriptsManager

    private var isTextChanged = false

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_script

    override fun getViewModelClazz() = StorageViewModel::class.java

    private lateinit var etName: EditText
    private lateinit var etFileName: EditText
    private lateinit var ivError: ImageView
    private lateinit var tvError: TextView

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        scriptsManager = koinScope.get<ScriptsManager>()

        isCancelable = false
        setTitle(if (script != null) R.string.title_edit_script else R.string.title_create_script)

        etName = dialogView.findViewById(R.id.edit_text_name)
        etFileName = dialogView.findViewById(R.id.edit_text_file_name)
        val etDescription = dialogView.findViewById<EditText>(R.id.edit_text_description)
        etDescription.imeOptions = EditorInfo.IME_ACTION_NEXT
        etDescription.setRawInputType(InputType.TYPE_CLASS_TEXT)
        val etText = dialogView.findViewById<EditText>(R.id.edit_text_text)

        ivError = dialogView.findViewById(R.id.image_view_error)
        tvError = dialogView.findViewById(R.id.text_view_error)

        if (BuildConfig.DEBUG && script == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etName.setText("Test script ${num}")
            etFileName.setText("script_${num}.js")
            etDescription.setText("Description $num")
            etText.setText("Test script text $num")
        }

        if (script != null) {
            etName.setText(script.name)
            etFileName.setText(script.fileName)
            etDescription.setText(script.description)
            etText.setText(scriptText)
        }

        setPositiveButton(R.string.answer_ok) { _,_ ->
            onApply(
                etName.text.toString(),
                etFileName.text.toString(),
                etDescription.text.toString(),
                etText.text.toString(),
            )
        }
        setNegativeButton(R.string.answer_cancel, isCloseDialog = false) { _, _ ->
            if (isTextChanged) {
                AskDialogs.showYesDialog(
                    context = requireContext(),
                    message = getString(R.string.ask_script_was_changed_request),
                    onApply = {
                        dismiss()
                    }
                )
            } else {
                dismiss()
            }
        }

        etName.addAfterTextChangedListener {
            checkPositiveButtonIsEnabled()
        }
        etFileName.addAfterTextChangedListener {
            checkPositiveButtonIsEnabled()
        }
        etText.addAfterTextChangedListener {
            isTextChanged = true
        }
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        checkPositiveButtonIsEnabled()
    }

    private fun checkPositiveButtonIsEnabled() {
        val enteredName = etName.text.toString()
        val enteredFileName = etFileName.text.toString()
        getPositiveButton()?.isEnabled = false

        lifecycleScope.launch {
            val isUniqueFileName = withIo {
                scriptsManager.isUniqueFileName(
                    storageId = storageId.orZero(),
                    scriptId = script?.id,
                    fileName = enteredFileName,
                )
            }
            ivError.isVisible = enteredFileName.isNotEmpty() && !isUniqueFileName
            tvError.isVisible = enteredFileName.isNotEmpty() && !isUniqueFileName
            tvError.text = buildString {
                if (enteredFileName.isNotEmpty() && !isUniqueFileName) {
                    append(getString(R.string.error_script_file_name_is_not_unique))
                }
            }

            getPositiveButton()?.isEnabled = enteredFileName.isNotEmpty() && enteredName.isNotEmpty() && isUniqueFileName
        }
    }

    companion object {
        const val TAG = "ScriptFieldsDialog"

    }

}

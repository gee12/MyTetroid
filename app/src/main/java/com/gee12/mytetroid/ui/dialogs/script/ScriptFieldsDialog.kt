package com.gee12.mytetroid.ui.dialogs.script

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.addAfterTextChangedListener
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.ui.dialogs.TetroidStorageDialogFragment
import com.gee12.mytetroid.ui.storage.StorageViewModel
import org.koin.android.ext.android.inject
import java.util.Random
import kotlin.math.abs

/**
 * Диалог создания/изменения скрипта.
 */
class ScriptFieldsDialog(
    private val script: TetroidScript?,
    private val scriptText: String?,
    private val onApply: (fileName: String, description: String, text: String) -> Unit,
) : TetroidStorageDialogFragment<StorageViewModel>() {

    val resourcesProvider: IResourcesProvider by inject()


    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_script

    override fun getViewModelClazz() = StorageViewModel::class.java

    private lateinit var etFileName: EditText

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        setTitle(if (script != null) R.string.title_edit_script else R.string.title_create_script)

        etFileName = dialogView.findViewById(R.id.edit_text_file_name)
        val etDescription = dialogView.findViewById<EditText>(R.id.edit_text_description)
        etDescription.imeOptions = EditorInfo.IME_ACTION_NEXT
        etDescription.setRawInputType(InputType.TYPE_CLASS_TEXT)
        val etText = dialogView.findViewById<EditText>(R.id.edit_text_text)

        if (BuildConfig.DEBUG && script == null) {
            val rand = Random()
            val num = abs(rand.nextInt())
            etFileName.setText("script_${num}.js")
            etDescription.setText("Description $num")
            etText.setText("Test script text $num")
        }

        if (script != null) {
            etFileName.setText(script.fileName)
            etDescription.setText(script.description)
            etText.setText(scriptText)
        }

        setPositiveButton(R.string.answer_ok) { _,_ ->
            onApply(
                etFileName.text.toString(),
                etDescription.text.toString(),
                etText.text.toString(),
            )
        }
        setNegativeButton(R.string.answer_cancel)

        etFileName.addAfterTextChangedListener { checkPositiveButtonIsEnabled() }
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        checkPositiveButtonIsEnabled()
    }

    private fun checkPositiveButtonIsEnabled() {
        // TODO: проверять уникальность имени файла

        getPositiveButton()?.isEnabled = etFileName.text.isNotEmpty()
    }

    companion object {
        const val TAG = "ScriptFieldsDialog"

    }

}

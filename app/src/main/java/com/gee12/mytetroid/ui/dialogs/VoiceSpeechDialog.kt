package com.gee12.mytetroid.ui.dialogs

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.gee12.mytetroid.R

class VoiceSpeechDialog(
    private val onCancel: () -> Unit,
) : BaseDialogFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewState: TextView

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_voice_speech

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        isCancelable = false
        setTitle(R.string.title_voice_input)
        setPositiveButton(R.string.answer_cancel) { _, _ ->
            onCancel()
        }

        progressBar = dialogView.findViewById(R.id.progress_circle)
        textViewState = dialogView.findViewById(R.id.text_view_state)
        textViewState.text = getString(R.string.title_voice_input_started)
    }

    fun onBeginningOfSpeech() {
        progressBar.isVisible = true
        textViewState.text = getString(R.string.state_voice_input_recognition)
    }

    companion object {
        const val TAG = "VoiceSpeechDialog"

    }

}
package com.gee12.mytetroid.ui.base.views

import android.view.View
import android.widget.EditText
import androidx.preference.PreferenceDialogFragmentCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.buildBundle
import com.gee12.mytetroid.common.extensions.checkDateFormatString
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.ui.TetroidMessage
import com.gee12.mytetroid.ui.base.views.prefs.DateTimeFormatPreference

class DateTimeFormatDialog : PreferenceDialogFragmentCompat() {

    private var mEditText: EditText? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val pref = preference as DateTimeFormatPreference
        val value = pref.value
        mEditText = view.findViewById(R.id.edit_text_value)
        mEditText?.setText(value)
        mEditText?.setSelection(value.length)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val newValue = mEditText?.text?.toString()
            if (!newValue.isNullOrEmpty() && checkDateFormatString(newValue)) {
                // сохраняем значение опции
                (preference as? DateTimeFormatPreference)?.also { pref ->
                    pref.value = newValue
                }
            } else {
                TetroidMessage.show(context, Message(getString(R.string.mes_input_wrong_date_format), LogType.WARNING))
            }
        }
    }

    companion object {

        fun newInstance(key: String?): DateTimeFormatDialog {
            val fragment = DateTimeFormatDialog()
            fragment.arguments = buildBundle {
                putString(ARG_KEY, key)
            }
            return fragment
        }

    }
}

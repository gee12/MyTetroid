package com.gee12.mytetroid.ui.base.views.prefs

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText
import androidx.preference.EditTextPreference
import com.gee12.mytetroid.R


class IntEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextPreferenceStyle,
    defStyleRes: Int = android.R.attr.editTextPreferenceStyle,
) : EditTextPreference(context, attrs, defStyleAttr, defStyleRes),
    EditTextPreference.OnBindEditTextListener {

    init {
        setOnBindEditTextListener(this)
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        val defaultIntValue = defaultReturnValue?.toIntOrNull() ?: -1
        return getPersistedInt(defaultIntValue).toString()
    }

    override fun persistString(value: String?): Boolean {
        return value?.toIntOrNull()?.let { intValue ->
            persistInt(intValue)
        } ?: false
    }

    override fun onBindEditText(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }

}
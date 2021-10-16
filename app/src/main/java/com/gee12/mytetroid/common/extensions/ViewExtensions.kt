package com.gee12.mytetroid.common.extensions

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText


fun EditText.addAfterTextChangedListener(listener: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            listener.invoke(s.toString())
        }
    })
}

fun EditText.setSelectionAtEnd() {
    setSelection(text.length)
}

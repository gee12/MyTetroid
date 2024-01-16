package com.gee12.mytetroid.common.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


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

/**
 * Принудительное отображение иконок у пунктов меню.
 */
@SuppressLint("RestrictedApi")
fun MenuBuilder.showForcedWithIcons(view: View) {
    val menuHelper = MenuPopupHelper(view.context, this, view)
    menuHelper.setForceShowIcon(true)
    menuHelper.show()
}

/**
 * @see <a href="https://developer.squareup.com/blog/showing-the-android-keyboard-reliably/">Взято отсюда</a>
 */
fun View.focusAndShowKeyboard() {
    /**
     * This is to be called when the window already has focus.
     */
    fun View.showTheKeyboardNow() {
        if (isFocused) {
            post {
                // We still post the call, just in case we are being notified of the windows focus
                // but InputMethodManager didn't get properly setup yet.
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    requestFocus()
    if (hasWindowFocus()) {
        // No need to wait for the window to get focus.
        showTheKeyboardNow()
    } else {
        // We need to wait until the window gets focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.addOnWindowFocusChangeListener(
                object : ViewTreeObserver.OnWindowFocusChangeListener {
                    override fun onWindowFocusChanged(hasFocus: Boolean) {
                        // This notification will arrive just before the InputMethodManager gets set up.
                        if (hasFocus) {
                            this@focusAndShowKeyboard.showTheKeyboardNow()
                            // It’s very important to remove this listener once we are done.
                            viewTreeObserver.removeOnWindowFocusChangeListener(this)
                        }
                    }
                })
        }
    }
}

fun View.showKeyboard() {
    //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    post { imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT) }
}

fun View.hideKeyboard() {
    //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    post { imm.hideSoftInputFromWindow(windowToken, 0) }
}

fun Window.resizeWindowWithKeyboard(rootView: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, windowInsets ->
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigationBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            rootView.setPadding(0, 0, 0, imeHeight - navigationBarHeight)
            windowInsets
        }
        //WindowCompat.setDecorFitsSystemWindows(this, false)
    } else {
        @Suppress("DEPRECATION")
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}

private fun createPopupWindow(
    anchorView: View,
    contentViewId: Int,
    layoutInflater: LayoutInflater,
    window: Window,
): PopupWindow {
    val popupView = layoutInflater.inflate(contentViewId, null).apply {
        measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
    }
    val popupWindow = PopupWindow(
        popupView,
        RelativeLayout.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT
    )
    popupWindow.isFocusable = false // если true - не будет кликабельно все вокруг popup
    popupWindow.isOutsideTouchable = true // true - клик за пределами popup закрывает его
    // (в Android 4.4 не работает)
    popupWindow.setBackgroundDrawable(ColorDrawable()) // чтобы заработал setOutsideTouchable()
    popupWindow.animationStyle = -1 // -1 - генерация анимации, 0 - отключить анимацию
    val xOffset = 0
    val yOffset = 36
    if (Build.VERSION.SDK_INT < 24) {
        popupWindow.showAsDropDown(
            anchorView,
            xOffset,
            -anchorView.measuredHeight - popupView.measuredHeight + yOffset
        )
    } else {
        val location = IntArray(2)
        anchorView.getLocationInWindow(location)
        popupWindow.showAtLocation(
            window.decorView,
            Gravity.NO_GRAVITY,
            location[0] + xOffset,
            location[1] - anchorView.measuredHeight + yOffset
        )
    }
    return popupWindow
}

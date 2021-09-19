package com.gee12.mytetroid.views.dialogs

import android.content.Context
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

abstract class TetroidDialogFragment : DialogFragment() {

    abstract fun getRequiredTag(): String

    abstract fun isPossibleToShow(): Boolean

    interface OnDialogOkCallback {
        fun onPositive()
    }

    interface OnDialogYesNoCallback {
        fun onPositive()
        fun onNegative()
    }

    interface OnDialogYesNoCancelCallback {
        fun onPositive()
        fun onNegative()
        fun onCancel()
    }

    var oneButtonCallback: OnDialogOkCallback? = null
    var twoButtonsCallback: OnDialogYesNoCallback? = null
    var threeButtonsCallback: OnDialogYesNoCancelCallback? = null


    fun showIfPossible(manager: FragmentManager) {
        if (isPossibleToShow()) {
            show(manager, getRequiredTag())
        } else {
            Log.i(getRequiredTag(), "Dialog fragment is not possible to show.")
        }
    }

    fun showIfNeeded(manager: FragmentManager?) {
        manager?.apply {
            if (findFragmentByTag(getRequiredTag()) == null) {
                show(this, getRequiredTag())
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (oneButtonCallback == null && context is OnDialogOkCallback) {
            this.oneButtonCallback = context
        } else if (twoButtonsCallback == null && context is OnDialogYesNoCallback) {
            this.twoButtonsCallback = context
        } else if (threeButtonsCallback == null && context is OnDialogYesNoCancelCallback) {
            this.threeButtonsCallback = context
        }
    }

    override fun onDetach() {
        oneButtonCallback = null
        twoButtonsCallback = null
        threeButtonsCallback = null
        super.onDetach()
    }

}
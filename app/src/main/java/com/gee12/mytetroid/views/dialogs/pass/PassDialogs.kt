package com.gee12.mytetroid.views.dialogs.pass

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.mytetroid.R

object PassDialogs {

    interface IPassInputResult {
        fun applyPass(pass: String)
        fun cancelPass()
    }

    /**
     * Диалог установки/ввода пароля.
     * @param node
     * @param isSetup
     * @param passResult
     */
    fun showPassEnterDialog(
        isSetup: Boolean,
        fragmentManager: FragmentManager,
        passResult: IPassInputResult
    ) {
        if (isSetup) {
            PassSetupDialog(passResult).showIfPossible(fragmentManager)
        } else {
            PassEnterDialog(passResult).showIfPossible(fragmentManager)
        }
    }

    fun showEmptyPassCheckingFieldDialog(
        context: Context,
        fieldName: String,
        callback: IApplyCancelResult
    ) {
        Dialogs.showAlertDialog(
            context,
            context.getString(R.string.log_empty_middle_hash_check_data_field, fieldName),
            callback
        )
    }

}
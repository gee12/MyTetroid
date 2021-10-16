package com.gee12.mytetroid.views.dialogs.pass

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.views.dialogs.pass.PassDialogs
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyCancelResult
import com.gee12.htmlwysiwygeditor.Dialogs
import com.gee12.mytetroid.R
import com.gee12.mytetroid.views.dialogs.pin.PINCodeDialog

object PassDialogs {

    interface IPassInputResult {
        fun applyPass(pass: String, node: TetroidNode?)
        fun cancelPass()
    }

    /**
     * Диалог установки/ввода пароля.
     * @param node
     * @param isNewPass
     * @param passResult
     */
    fun showPassEnterDialog(
        node: TetroidNode?,
        isNewPass: Boolean,
        fragmentManager: FragmentManager,
        passResult: IPassInputResult
    ) {
        if (isNewPass) {
            PassSetupDialog(node, passResult).showIfPossible(fragmentManager)
        } else {
            PassEnterDialog(node, passResult).showIfPossible(fragmentManager)
        }
    }

    fun showEmptyPassCheckingFieldDialog(context: Context, fieldName: String, callback: IApplyCancelResult) {
        Dialogs.showAlertDialog(context, String.format(context.getString(R.string.log_empty_middle_hash_check_data_field), fieldName), callback)
    }

}
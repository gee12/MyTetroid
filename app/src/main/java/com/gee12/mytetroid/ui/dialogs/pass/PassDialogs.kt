package com.gee12.mytetroid.ui.dialogs.pass

import androidx.fragment.app.FragmentManager

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
    fun showPasswordEnterDialog(
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

}
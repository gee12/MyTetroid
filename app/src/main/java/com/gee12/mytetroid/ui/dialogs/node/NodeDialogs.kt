package com.gee12.mytetroid.ui.dialogs.node

import android.content.Context
import com.gee12.htmlwysiwygeditor.Dialogs.IApplyResult
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.model.TetroidNode

object NodeDialogs {

    interface INodeChooserResult {
        fun onApply(node: TetroidNode?)
        fun onProblem(code: Int)

        companion object {
            const val LOAD_STORAGE = 1
            const val LOAD_ALL_NODES = 2
        }
    }

    /**
     * Вопрос об удалении ветки.
     */
    @JvmStatic
    fun deleteNode(context: Context, nodeName: String, isQuicklyNode: Boolean, callback: IApplyResult) {
        val mesId = if (isQuicklyNode) R.string.ask_quickly_node_delete_mask else R.string.ask_node_delete_mask
        AskDialogs.showYesDialog(context, callback, context.getString(mesId, nodeName))
    }

}
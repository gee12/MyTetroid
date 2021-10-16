package com.gee12.mytetroid.views.dialogs.node;

import android.content.Context;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.views.dialogs.AskDialogs;


public class NodeDialogs {

    public interface INodeChooserResult {
        int LOAD_STORAGE = 1;
        int LOAD_ALL_NODES = 2;

        void onApply(TetroidNode node);
        void onProblem(int code);
    }

    /**
     * Вопрос об удалении ветки.
     * @param context
     * @param callback
     */
    public static void deleteNode(Context context, String nodeName, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, context.getString(R.string.ask_node_delete_mask, nodeName));
    }

}

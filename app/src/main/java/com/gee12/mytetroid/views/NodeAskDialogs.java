package com.gee12.mytetroid.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.model.TetroidNode;

import java.util.Random;

public class NodeAskDialogs {

    public interface INodeFieldsResult {
        void onApply(String name);
    }

    /**
     * Диалог создания/изменения ветки.
     * @param context
     * @param handler
     */
    public static void createNodeDialog(Context context, TetroidNode node, INodeFieldsResult handler) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_node);

        EditText etName = builder.getView().findViewById(R.id.edit_text_name);

        if (BuildConfig.DEBUG && node == null) {
            Random rand = new Random();
            int num = Math.abs(rand.nextInt());
            etName.setText("node " + num);
        }

        if (node != null) {
            etName.setText(node.getName());
        }

        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            handler.onApply(etName.getText().toString());
        }).setNegativeButton(R.string.answer_cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialog12 -> {
            // получаем okButton уже после вызова show()
            final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (TextUtils.isEmpty(etName.getText().toString())) {
                okButton.setEnabled(false);
            }
            etName.setSelection(etName.getText().length());
//            Keyboard.showKeyboard(etName);
        });
        dialog.show();

        // получаем okButton тут отдельно после вызова show()
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(!TextUtils.isEmpty(s));
            }
        });
    }

    /**
     * Диалог информации о записи.
     * @param context
     * @param node
     */
    public static void createNodeInfoDialog(Context context, TetroidNode node) {
        if (node == null || !node.isNonCryptedOrDecrypted())
            return;
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_node_info);
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setTitle(node.getName());

        View view = builder.getView();
        ((TextView)view.findViewById(R.id.text_view_id)).setText(node.getId());
        ((TextView)view.findViewById(R.id.text_view_crypted)).setText(node.isCrypted()
                ? R.string.answer_yes : R.string.answer_no);
        int[] nodesRecords = NodesManager.getNodesRecordsCount(node);
        if (nodesRecords != null) {
            ((TextView) view.findViewById(R.id.text_view_nodes)).setText(String.valueOf(nodesRecords[0]));
            ((TextView) view.findViewById(R.id.text_view_records)).setText(String.valueOf(nodesRecords[1]));
        }
        builder.show();
    }

    public static void deleteNode(Context context, final AskDialogs.IApplyResult applyHandler) {
        AskDialogs.showYesDialog(context, applyHandler, R.string.ask_node_delete);
    }

}

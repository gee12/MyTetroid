package com.gee12.mytetroid.dialogs;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.adapters.NodesListAdapter;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.views.SearchViewListener;

import java.util.List;
import java.util.Random;

import pl.openrnd.multilevellistview.MultiLevelListView;

public class NodeDialogs {

    public interface INodeFieldsResult {
        void onApply(String name);
    }

    public interface INodeChooserResult {
        void onApply(TetroidNode node);
    }

    /**
     * Диалог создания/изменения ветки.
     * @param context
     * @param callback
     */
    public static void createNodeDialog(Context context, TetroidNode node, INodeFieldsResult callback) {
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
            callback.onApply(etName.getText().toString());
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
     * Диалог информации о ветке.
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

    /**
     * Диалог выбора ветки.
     * @param context
     * @param canCrypted Могут быть выбраны даже нерасшифрованные ветки.
     * @param canDecrypted Могут быть выбраны расшифрованные ветки.
     * @param onlyRoot
     * @param callback
     */
    public static void createNodeChooserDialog(Context context, TetroidNode node, boolean canCrypted, boolean canDecrypted,
                                               boolean onlyRoot, INodeChooserResult callback) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_nodes);
        builder.setTitle("Выберите ветку");

        final AlertDialog dialog = builder.create();
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        View view = builder.getView();
        // уведомление
//        TextView tvNoticeTop = view.findViewById(R.id.text_view_notice_top);
        TextView tvNoticeBottom = view.findViewById(R.id.text_view_notice_bottom);
        // список веток
        MultiLevelListView listView = view.findViewById(R.id.list_view_nodes);
        final NodesListAdapter adapter = new NodesListAdapter(context, null);
        adapter.setCurNode(node);
        adapter.setNodeHeaderClickListener(new NodesListAdapter.OnNodeHeaderClickListener() {

            private void onSelectNode(TetroidNode node) {
                boolean crypted = !canCrypted && node.isCrypted() && !node.isDecrypted();
                boolean decrypted = !canDecrypted && node.isCrypted() && node.isDecrypted();
                boolean notRoot = node.getLevel() > 1;
                if (crypted || notRoot) {
                    tvNoticeBottom.setVisibility(View.VISIBLE);
                    okButton.setEnabled(false);
                    String mes = null;
                    if (crypted) {
                        mes = context.getString(R.string.mes_select_non_encrypted_node);
                    } else if (decrypted) {
                        mes = context.getString(R.string.mes_select_decrypted_node);
                    }
                    if (notRoot) {
                        mes += ((mes == null) ? "" : "\n") + context.getString(R.string.mes_select_first_level_node);;
                    }
                    tvNoticeBottom.setText(mes);
                } else {
                    tvNoticeBottom.setVisibility(View.GONE);
                    adapter.setCurNode(node);
                    okButton.setEnabled(true);
                }
            }
            @Override
            public void onClick(TetroidNode node) {
                onSelectNode(node);
            }
            @Override
            public boolean onLongClick(View view, TetroidNode node, int pos) {
                onSelectNode(node);
                return true;
            }
        });
        listView.setAdapter(adapter);
        // строка поиска
        SearchView searchView = view.findViewById(R.id.search_view_nodes);
        new SearchViewListener(searchView) {
            @Override
            public void onClose() {
                adapter.setDataItems(DataManager.getRootNodes());
            }
            @Override
            public void onSearch() {
            }
            @Override
            public void onQuerySubmit(String query) {
                List<TetroidNode> found = ScanManager.searchInNodesNames(DataManager.getRootNodes(), query);
                adapter.setDataItems(found);
            }
        };
        // обработчик результата
        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            callback.onApply(adapter.getCurNode());
        }).setNegativeButton(R.string.answer_cancel, null);

        // загружаем список веток
        adapter.setDataItems(DataManager.getRootNodes());
    }

    /**
     * Вопрос об удалении ветки.
     * @param context
     * @param callback
     */
    public static void deleteNode(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_node_delete);
    }
}

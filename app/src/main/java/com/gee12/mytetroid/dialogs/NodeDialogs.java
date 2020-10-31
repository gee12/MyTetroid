package com.gee12.mytetroid.dialogs;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.adapters.NodesListAdapter;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.NodesManager;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.model.TetroidNode;

import java.util.List;
import java.util.Random;

import pl.openrnd.multilevellistview.MultiLevelListView;

public class NodeDialogs {

    public interface INodeFieldsResult {
        void onApply(String name);
    }

    public interface INodeChooserResult {
        public static final int LOAD_STORAGE = 1;
        public static final int LOAD_ALL_NODES = 2;

        void onApply(TetroidNode node);
        void onProblem(int code);
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
        // проверяем загружено ли хранилище
        if (!DataManager.isLoaded()) {
            callback.onProblem(INodeChooserResult.LOAD_STORAGE);
            return;
        }
        // проверяем загружены ли все ветки
        if (DataManager.isFavoritesMode()) {
            callback.onProblem(INodeChooserResult.LOAD_ALL_NODES);
            return;
        }

        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_nodes);
        builder.setTitle(R.string.title_choose_node);

        final NodesListAdapter adapter = new NodesListAdapter(context, null);
        // обработчик результата
        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            callback.onApply(adapter.getCurNode());
        }).setNegativeButton(R.string.answer_cancel, null);

        final AlertDialog dialog = builder.create();
//        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        View view = builder.getView();
        // уведомление
//        TextView tvNoticeTop = view.findViewById(R.id.text_view_notice_top);
        TextView tvNoticeBottom = view.findViewById(R.id.text_view_notice_bottom);
        // список веток
        MultiLevelListView listView = view.findViewById(R.id.list_view_nodes);
        adapter.setCurNode(node);
        adapter.setNodeHeaderClickListener(new NodesListAdapter.OnNodeHeaderClickListener() {
            private void onSelectNode(TetroidNode node) {
                final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                boolean crypted = !canCrypted && node.isCrypted() && !node.isDecrypted();
                boolean decrypted = !canDecrypted && node.isCrypted() && node.isDecrypted();
                boolean notRoot = node.getLevel() > 0;
                if (crypted || decrypted || notRoot) {
                    String mes = null;
                    if (crypted) {
                        mes = context.getString(R.string.mes_select_non_encrypted_node);
                    } else if (decrypted) {
                        mes = context.getString(R.string.mes_select_decrypted_node);
                    }
                    if (notRoot) {
                        mes = ((mes == null) ? "" : mes + "\n") + context.getString(R.string.mes_select_first_level_node);;
                    }
                    tvNoticeBottom.setText(mes);
                    tvNoticeBottom.setVisibility(View.VISIBLE);
                    okButton.setEnabled(false);
                } else {
                    tvNoticeBottom.setVisibility(View.GONE);
                    okButton.setEnabled(true);
                }
                adapter.setCurNode(node);
                adapter.notifyDataSetChanged();
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
        TextView tvEmpty = view.findViewById(R.id.nodes_text_view_empty);
        SearchView searchView = view.findViewById(R.id.search_view_nodes);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                List<TetroidNode> found = ScanManager.searchInNodesNames(DataManager.getRootNodes(), query);
                adapter.setDataItems(found);
                if (found.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(String.format(context.getString(R.string.search_nodes_not_found_mask), query));
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        // Catch event on [x] button inside search view
//        int searchCloseButtonId = searchView.getContext().getResources()
//                .getIdentifier("app:id/search_close_btn", null, null);
//        AppCompatImageView closeButton = searchView.findViewById(searchCloseButtonId);
        View closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            searchView.setQuery("", false);
            adapter.setDataItems(DataManager.getRootNodes());
            tvEmpty.setVisibility(View.GONE);
        });

        // загружаем список веток
        adapter.setDataItems(DataManager.getRootNodes());

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(adapter.getCurNode() != null);
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

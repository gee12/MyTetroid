package com.gee12.mytetroid.dialogs;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.Utils;

import java.util.Date;
import java.util.Random;

public class RecordDialogs {

    public interface IRecordFieldsResult {
        void onApply(String name, String tags, String author, String url, TetroidNode node, boolean isFavor);
    }

    /**
     * Диалог создания/изменения записи.
     * @param context
     * @param handler
     */
    public static void createRecordFieldsDialog(Context context, TetroidRecord record, boolean isNeedNode, IRecordFieldsResult handler) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_record);

        View view = builder.getView();
        EditText etName = view.findViewById(R.id.edit_text_name);
        EditText etAuthor = view.findViewById(R.id.edit_text_author);
        EditText etUrl = view.findViewById(R.id.edit_text_url);
        EditText etTags = view.findViewById(R.id.edit_text_tags);
        LinearLayout layoutNode = view.findViewById(R.id.layout_node);
        EditText etNode = view.findViewById(R.id.edit_text_node);
        CheckedTextView ctvFavor = view.findViewById(R.id.check_box_favor);

        if (BuildConfig.DEBUG && record == null) {
            Random rand = new Random();
            int num = Math.abs(rand.nextInt());
            etName.setText("record " + num);
            etAuthor.setText("author " + num);
            etUrl.setText("http://url" + num + ".com");
            etTags.setText("new record , tag " + num);
        }

        if (record != null) {
            etName.setText(record.getName());
            etAuthor.setText(record.getAuthor());
            etUrl.setText(record.getUrl());
            String tagsString = record.getTagsString();
            etTags.setText(tagsString);
            layoutNode.setVisibility((isNeedNode) ? View.VISIBLE : View.GONE);
            ctvFavor.setVisibility((App.isFullVersion()) ? View.VISIBLE : View.GONE);
            ctvFavor.setChecked(record.isFavorite());
        }

        // обработчик выбора ветки
        NodeChooserResult callback = new NodeChooserResult();
        if (isNeedNode) {
            layoutNode.setOnClickListener(v -> {
                NodeDialogs.createNodeChooserDialog(context, true, callback);
            });
        }

        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            handler.onApply(etName.getText().toString(),
                    etTags.getText().toString(),
                    etAuthor.getText().toString(),
                    etUrl.getText().toString(),
                    (isNeedNode) ? callback.getSelectedNode() : null,
                    ctvFavor.isChecked());
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
     * Для выбора ветки.
     */
    private static class NodeChooserResult implements NodeDialogs.INodeChooserResult {
        TetroidNode mSelectedNode;

        @Override
        public void onApply(TetroidNode node) {
            this.mSelectedNode = node;
        }

        public TetroidNode getSelectedNode() {
            return mSelectedNode;
        }
    }

    /**
     * Диалог информации о записи.
     * @param context
     * @param record
     */
    public static void createRecordInfoDialog(Context context, TetroidRecord record) {
        if (record == null || !record.isNonCryptedOrDecrypted())
            return;
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_record_info);
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setTitle(record.getName());

        View view = builder.getView();
        ((TextView)view.findViewById(R.id.text_view_id)).setText(record.getId());
        ((TextView)view.findViewById(R.id.text_view_crypted)).setText(record.isCrypted()
                ? R.string.answer_yes : R.string.answer_no);
        String dateFormat = context.getString(R.string.full_date_format_string);
        Date created = record.getCreated();
        ((TextView)view.findViewById(R.id.text_view_created)).setText(
                (created != null) ? Utils.dateToString(created, dateFormat) : "-");

        if (App.isFullVersion()) {
            (view.findViewById(R.id.table_row_edited)).setVisibility(View.VISIBLE);
            Date edited = RecordsManager.getEditedDate(context, record);
            ((TextView)view.findViewById(R.id.text_view_edited))
                    .setText((edited != null) ? Utils.dateToString(edited, dateFormat) : "-");
        }
        String path = RecordsManager.getPathToRecordFolder(record);
        ((TextView)view.findViewById(R.id.text_view_path)).setText(path);
        String size = DataManager.getFileSize(context, path);
        TextView tvSize = view.findViewById(R.id.text_view_size);
        if (size == null) {
            size = context.getString(R.string.title_folder_is_missing);
            tvSize.setTextColor(ContextCompat.getColor(context.getApplicationContext(), R.color.colorDarkRed));
        }
        tvSize.setText(size);

        builder.show();
    }

    public static void saveRecord(Context context, final Dialogs.IApplyCancelResult callback) {
        AskDialogs.showYesNoDialog(context, callback, R.string.ask_save_record);
    }

    public static void deleteRecord(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_record_delete);
    }

    public static void operWithoutDir(Context context, TetroidLog.Opers oper, final Dialogs.IApplyResult callback) {
        int resId = (oper == TetroidLog.Opers.DELETE) ? R.string.title_delete
                : (oper == TetroidLog.Opers.CUT) ? R.string.title_cut
                : R.string.title_insert;
        String mes = String.format(context.getString(R.string.ask_oper_without_record_dir_mask),
                context.getString(resId));
        Dialogs.showAlertDialog(context, mes, true, true, callback);
    }

}

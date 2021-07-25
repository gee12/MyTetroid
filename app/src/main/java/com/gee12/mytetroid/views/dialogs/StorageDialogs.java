package com.gee12.mytetroid.views.dialogs;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.BuildConfig;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidStorage;
import com.gee12.mytetroid.views.EditTextWatcher;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.adapters.StorageChooserAdapter;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class StorageDialogs {

    public interface IStorageResult {
//        void onApply(String path, String name, boolean isReadOnly);
        void onApply(@NotNull TetroidStorage storage);
        void onSelectPath(String path);
    }

    public interface IItemClickListener {
        void onItemClick(boolean isNew);
    }

    /**
     * Диалог создания/изменения хранилища.
     */
    public static class StorageDialog {

        private final AlertDialog mDialog;
        private final EditText mEtPath;
        private final EditText mEtName;
        private boolean mIsPathSelected;
        private boolean isNew;

        public void setPath(String path, boolean isNew) {
            if (TextUtils.isEmpty(path)) {
                return;
            }
            this.mIsPathSelected = true;
            mEtPath.setText(path);
            mEtPath.setTextSize(14);
            String folderName = new File(path).getName();
            mEtName.setText(folderName);
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(!TextUtils.isEmpty(path) && !TextUtils.isEmpty(folderName));
        }

        public StorageDialog(Context context, TetroidStorage storage, IStorageResult callback) {
            Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_storage);
            builder.setTitle(context.getString((storage != null) ? R.string.title_edit_storage : R.string.title_add_storage));

            View view = builder.getView();
            this.mEtPath = view.findViewById(R.id.edit_text_path);
            mEtPath.setInputType(InputType.TYPE_NULL);
            ImageButton bPath = view.findViewById(R.id.button_path);
            this.mEtName = view.findViewById(R.id.edit_text_name);
            CheckedTextView cbIsDefault = view.findViewById(R.id.check_box_is_default);
            cbIsDefault.setOnClickListener(v -> cbIsDefault.setChecked(!cbIsDefault.isChecked()));
            CheckedTextView cbReadOnly = view.findViewById(R.id.check_box_read_only);
            // TODO: принудительно отключаем (пока)
            cbReadOnly.setEnabled(false);

            if (storage != null) {
                if (!TextUtils.isEmpty(storage.getPath())) {
                    mEtPath.setText(storage.getPath());
                    mEtPath.setTextSize(14);
                    this.mIsPathSelected = true;
                }
                mEtName.setText(storage.getName());
                cbIsDefault.setChecked(storage.isDefault());
            }

            View.OnClickListener clickListener = v -> {
                callback.onSelectPath(mEtPath.getText().toString());
            };
            bPath.setOnClickListener(clickListener);
            mEtPath.setOnClickListener(clickListener);

            this.mDialog = builder.create();

            // кнопки результата
            mDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.answer_ok), (dialog1, which) -> {
                TetroidStorage res = new TetroidStorage(
                        mEtName.getText().toString(),
                        mEtPath.getText().toString(),
                        cbIsDefault.isChecked(),
                        cbReadOnly.isChecked(),
                        isNew
                );
                callback.onApply(res);
            });
            mDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.answer_cancel), (dialog1, which) -> {
                dialog1.cancel();
            });

            mDialog.setOnShowListener(dialog12 -> {
                // получаем okButton уже после вызова show()
                if (!mIsPathSelected || TextUtils.isEmpty(mEtName.getText().toString())) {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                mEtName.setSelection(mEtName.getText().length());
            });
            mDialog.show();

            // получаем okButton тут отдельно после вызова show()
            final Button okButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            new EditTextWatcher(mEtName) {
                @Override
                public void onOnlyAfterTextChanged(String text) {
                    okButton.setEnabled(mIsPathSelected && !TextUtils.isEmpty(text));
                }
            };
        }
    }

    /**
     * Диалог со списком вариантов указания хранилища.
     * @param context
     * @param listener
     */
    public static void createStorageSelectionDialog(Context context, IItemClickListener listener) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_list_view);
//        builder.setTitle("Выберите действие");
        final AlertDialog dialog = builder.create();

        ListView listView = builder.getView().findViewById(R.id.list_view);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            listener.onItemClick(position == 1);
            dialog.cancel();
        });
        listView.setAdapter(new StorageChooserAdapter(context));
        dialog.show();
    }
}

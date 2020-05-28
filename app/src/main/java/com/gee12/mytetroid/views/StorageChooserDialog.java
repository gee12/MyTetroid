package com.gee12.mytetroid.views;

import android.content.Context;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.adapters.StorageChooserAdapter;

public class StorageChooserDialog {

    public interface IItemClickListener {
        void onItemClick(boolean isNew);
    }

    /**
     * Диалог со списком вариантов.
     * @param context
     * @param listener
     */
    public static void createDialog(Context context, IItemClickListener listener) {
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

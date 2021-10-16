package com.gee12.mytetroid.views.dialogs.storage;

import android.content.Context;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.views.adapters.StorageChooserAdapter;


public class StorageDialogs {

    public interface IItemClickListener {
        void onItemClick(boolean isNew);
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

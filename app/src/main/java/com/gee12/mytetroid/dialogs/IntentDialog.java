package com.gee12.mytetroid.dialogs;

import android.content.Context;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.adapters.IntentsAdapter;
import com.gee12.mytetroid.model.ReceivedData;

public class IntentDialog {

    public interface IItemClickListener {
        void onItemClick(ReceivedData item);
    }

    /**
     * Диалог со списком вариантов обработки переданного объекта.
     * @param context
     * @param isText
     * @param listener
     */
    public static void createDialog(Context context, boolean isText, IItemClickListener listener) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_list_view);
//        builder.setTitle("Выберите действие");
        final AlertDialog dialog = builder.create();

        ListView listView = builder.getView().findViewById(R.id.list_view);
        ReceivedData[] dataSet = (isText) ? ReceivedData.textIntents() : ReceivedData.imageIntents();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            listener.onItemClick(dataSet[position]);
            dialog.cancel();
        });
        listView.setAdapter(new IntentsAdapter(context, dataSet));
        dialog.show();
    }
}

package com.gee12.mytetroid.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.AttachesManager;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.Utils;

import java.util.Date;

public class AttachDialogs {

    /**
     * Диалог информации о прикрепленном файле.
     * @param context
     * @param attach
     */
    public static void createAttachInfoDialog(Context context, TetroidFile attach) {
        if (attach == null || !attach.isNonCryptedOrDecrypted())
            return;
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_attach_info);
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setTitle(attach.getName());

        TetroidRecord record = attach.getRecord();
        if (record == null) {
            LogManager.log(context, context.getString(R.string.log_file_record_is_null), ILogger.Types.ERROR);
            return;
        }

        View view = builder.getView();
        ((TextView)view.findViewById(R.id.text_view_id)).setText(attach.getId());
        ((TextView) view.findViewById(R.id.text_view_record)).setText(record.getName());
        ((TextView)view.findViewById(R.id.text_view_crypted)).setText(attach.isCrypted()
                ? R.string.answer_yes : R.string.answer_no);
        String dateFormat = context.getString(R.string.full_date_format_string);

        if (App.isFullVersion()) {
            (view.findViewById(R.id.table_row_edited)).setVisibility(View.VISIBLE);
            Date edited = AttachesManager.getEditedDate(context, attach);
            ((TextView)view.findViewById(R.id.text_view_edited))
                    .setText((edited != null) ? Utils.dateToString(edited, dateFormat) : "-");
        }
        String path = RecordsManager.getPathToRecordFolder(context, record);
        ((TextView)view.findViewById(R.id.text_view_path)).setText(path);
        String size = AttachesManager.getAttachedFileSize(context, attach);
        TextView tvSize = view.findViewById(R.id.text_view_size);
        if (size == null) {
            size = context.getString(R.string.title_folder_is_missing);
            tvSize.setTextColor(ContextCompat.getColor(context.getApplicationContext(), R.color.colorDarkRed));
        }
        tvSize.setText(size);

        builder.show();
    }
}

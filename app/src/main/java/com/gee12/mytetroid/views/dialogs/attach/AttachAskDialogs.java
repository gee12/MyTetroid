package com.gee12.mytetroid.views.dialogs.attach;

import android.content.Context;
import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.LogOper;
import com.gee12.mytetroid.views.dialogs.AskDialogs;


public class AttachAskDialogs {

    public static void deleteFile(Context context, String fileName, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, context.getString(R.string.ask_file_delete_mask, fileName));
    }

    public static void deleteAttachWithoutFile(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_attach_delete_without_file);
    }

    public static void renameAttachWithoutFile(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_delete_attach_without_file);
    }

    public static void renameAttachWithoutDir(Context context, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, R.string.ask_delete_record_without_dir);
    }

    public static void operWithoutFile(Context context, LogOper oper, final Dialogs.IApplyResult callback) {
        int resId = (oper == LogOper.DELETE) ? R.string.ask_attach_delete_without_file
                : R.string.ask_delete_attach_without_file;
        String mes = context.getString(resId);
        Dialogs.showAlertDialog(context, mes, true, true, callback);
    }

}

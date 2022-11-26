package com.gee12.mytetroid.ui.dialogs.record;

import android.content.Context;

import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.interactors.NodesInteractor;
import com.gee12.mytetroid.interactors.RecordsInteractor;
import com.gee12.mytetroid.interactors.StorageInteractor;
import com.gee12.mytetroid.logs.LogOper;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.ui.dialogs.AskDialogs;

public class RecordDialogs {

    private StorageInteractor storageInteractor;
    private RecordsInteractor recordsInteractor;
    private NodesInteractor nodesInteractor;

    public RecordDialogs(StorageInteractor storageInteractor, RecordsInteractor recordsInteractor, NodesInteractor nodesInteractor) {
        this.storageInteractor = storageInteractor;
        this.recordsInteractor = recordsInteractor;
        this.nodesInteractor = nodesInteractor;
    }

    public interface IRecordFieldsResult {
        void onApply(String name, String tags, String author, String url, TetroidNode node, boolean isFavor);
    }

    public static void saveRecord(Context context, final Dialogs.IApplyCancelResult callback) {
        AskDialogs.showYesNoDialog(context, callback, R.string.ask_save_record);
    }

    public static void deleteRecord(Context context, String recordName, final Dialogs.IApplyResult callback) {
        AskDialogs.showYesDialog(context, callback, context.getString(R.string.ask_record_delete_mask, recordName));
    }

    public static void operWithoutDir(Context context, LogOper oper, final Dialogs.IApplyResult callback) {
        int resId = (oper == LogOper.DELETE) ? R.string.title_delete
                : (oper == LogOper.CUT) ? R.string.title_cut
                : R.string.title_insert;
        String mes = String.format(context.getString(R.string.ask_oper_without_record_dir_mask),
                context.getString(resId));
        Dialogs.showAlertDialog(context, mes, true, true, callback);
    }

}

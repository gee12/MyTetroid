package com.gee12.mytetroid;

import android.widget.Toast;

public class TetroidLog extends LogManager {

    public enum Objs {
        NODE (R.string.oper_node, R.string.oper_res_node),
        NODE_FIELDS (R.string.oper_node_fields, R.string.oper_res_node_fields),
        RECORD (R.string.oper_record, R.string.oper_res_record),
        RECORD_FIELDS (R.string.oper_record_fields, R.string.oper_res_record_fields),
        FILE (R.string.oper_file, R.string.oper_res_file),
        FILE_FIELDS (R.string.oper_file_fields, R.string.oper_res_file_fields);

        int mRes;
        int mResultRes;

        Objs(int res, int resres) {
            this.mRes = res;
            this.mResultRes = resres;
        }

        int getRes(boolean isResult) {
            return (isResult) ? mResultRes : mRes;
        }
    }

    public enum Opers {
        CREATE (R.string.create, R.string.created),
        ADD (R.string.add, R.string.added),
        CHANGE (R.string.change, R.string.changed),
        RENAME (R.string.rename, R.string.renamed),
        DELETE (R.string.delete, R.string.deleted),
        COPY (R.string.copy, R.string.copied),
        CUT (R.string.cut, R.string.cutted),
        INSERT (R.string.insert, R.string.inserted),
        MOVE (R.string.move, R.string.moved),
        SAVE (R.string.save, R.string.saved);

        int mRes;
        int mResultRes;

        Opers(int res, int resres) {
            this.mRes = res;
            this.mResultRes = resres;
        }

        int getRes(boolean isResult) {
            return (isResult) ? mResultRes : mRes;
        }
    }

    public static void addOperResLog(Objs obj, Opers oper) {
        String mes = getString(obj.getRes(true)) + getString(oper.getRes(true));
        LogManager.addLog(mes, LogManager.Types.INFO, Toast.LENGTH_SHORT);
    }

    public static void addOperErrorLog(Objs obj, Opers oper) {
        String mes = String.format(getString(R.string.log_oper_error_mask),
                getString(obj.getRes(false)), getString(oper.getRes(false)));
        LogManager.addLog(mes, Types.ERROR, Toast.LENGTH_LONG);
    }

    private static String getString(int resId) {
        return context.getString(resId);
    }
}

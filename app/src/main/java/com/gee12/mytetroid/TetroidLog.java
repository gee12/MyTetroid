package com.gee12.mytetroid;

import android.widget.Toast;

public class TetroidLog extends LogManager {

    public static final int PRESENT_SIMPLE = 0;
    public static final int PAST_PERFECT = 1;
    public static final int PRESENT_CONTINUOUS = 2;

    public enum Objs {
        NODE (R.string.oper_simple_node, R.string.oper_past_node, R.string.oper_continuous_node),
        NODE_FIELDS (R.string.oper_simple_node_fields, R.string.oper_past_node_fields, R.string.oper_continuous_node_fields),
        RECORD (R.string.oper_simple_record, R.string.oper_past_record, R.string.oper_continuous_record),
        RECORD_FIELDS (R.string.oper_simple_record_fields, R.string.oper_past_record_fields, R.string.oper_continuous_record_fields),
        RECORD_DIR (R.string.oper_simple_record_dir, R.string.oper_past_record_dir, R.string.oper_continuous_record_dir),
        FILE (R.string.oper_simple_file, R.string.oper_past_file, R.string.oper_continuous_file),
        FILE_FIELDS (R.string.oper_simple_file_fields, R.string.oper_past_file_fields, R.string.oper_continuous_file_fields);

        int[] mRes = new int[3];

        Objs(int simple, int perfect, int continuous) {
            this.mRes[0] = simple;
            this.mRes[1] = perfect;
            this.mRes[2] = continuous;
        }

        int getResId(int tense) {
            return (tense >= 0 && tense < 3) ? mRes[tense] : 0;
        }
    }

    public enum Opers {
        CREATE (R.string.create, R.string.created, R.string.creating),
        ADD (R.string.add, R.string.added, R.string.adding),
        CHANGE (R.string.change, R.string.changed, R.string.changing),
        RENAME (R.string.rename, R.string.renamed, R.string.renaming),
        DELETE (R.string.delete, R.string.deleted, R.string.deleting),
        COPY (R.string.copy, R.string.copied, R.string.copying),
        CUT (R.string.cut, R.string.cutted, R.string.cutting),
        INSERT (R.string.insert, R.string.inserted, R.string.inserting),
        MOVE (R.string.move, R.string.moved, R.string.moving),
        SAVE (R.string.save, R.string.saved, R.string.saving),
        ATTACH (R.string.attach, R.string.attached, R.string.attaching),
        ENCRYPT (R.string.encrypt, R.string.encrypted, R.string.encrypting),
        DECRYPT (R.string.decrypt, R.string.decrypted, R.string.decrypting);

        int[] mRes = new int[3];

        Opers(int simple, int perfect, int continuous) {
            this.mRes[0] = simple;
            this.mRes[1] = perfect;
            this.mRes[2] = continuous;
        }

        int getResId(int tense) {
            return (tense >= 0 && tense < 3) ? mRes[tense] : 0;
        }
    }

    public static String addOperStartLog(Objs obj, Opers oper) {
        String first = (App.isRusLanguage()) ? getString(oper.getResId(PRESENT_CONTINUOUS)) : getString(obj.getResId(PRESENT_CONTINUOUS));
        String second = (App.isRusLanguage()) ? getString(obj.getResId(PRESENT_CONTINUOUS)) : getString(oper.getResId(PRESENT_CONTINUOUS));
        String mes = String.format(getString(R.string.log_oper_start_mask), first, second);
        LogManager.addLog(mes, Types.ERROR);
        return mes;
    }

    public static String addOperCancelLog(Objs obj, Opers oper) {
        String mes = String.format(getString(R.string.log_oper_cancel_mask),
                getString(obj.getResId(PRESENT_CONTINUOUS)), getString(oper.getResId(PRESENT_CONTINUOUS)));
        LogManager.addLog(mes, Types.DEBUG);
        return mes;
    }

    public static String addOperResLog(Objs obj, Opers oper) {
        return addOperResLog(obj, oper, Toast.LENGTH_SHORT);
    }

    public static String addOperResLog(Objs obj, Opers oper, int length) {
        String mes = getString(obj.getResId(PAST_PERFECT)) + getString(oper.getResId(PAST_PERFECT));
        LogManager.addLog(mes, Types.INFO, length);
        return mes;
    }

    public static String addOperErrorLog(Objs obj, Opers oper) {
        return addOperErrorLog(obj, oper, Toast.LENGTH_LONG);
    }

    public static String addOperErrorLog(Objs obj, Opers oper, int length) {
        String mes = String.format(getString(R.string.log_oper_error_mask),
                getString(obj.getResId(PRESENT_SIMPLE)), getString(oper.getResId(PRESENT_SIMPLE)));
        LogManager.addLog(mes, Types.ERROR, length);
        return mes;
    }

    private static String getString(int resId) {
        return context.getString(resId);
    }
}

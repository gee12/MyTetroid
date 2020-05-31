package com.gee12.mytetroid;

import android.widget.Toast;

public class TetroidLog extends LogManager {

    public static final int PRESENT_SIMPLE = 0;
    public static final int PAST_PERFECT = 1;
    public static final int PRESENT_CONTINUOUS = 2;

    public enum Objs {
        STORAGE(R.array.obj_storage),//STORAGE (R.string.oper_simple_node, R.string.oper_past_node, R.string.oper_continuous_node),
        NODE(R.array.obj_node),//NODE (R.string.oper_simple_node, R.string.oper_past_node, R.string.oper_continuous_node),
        NODE_FIELDS(R.array.obj_node_fields),//NODE_FIELDS (R.string.oper_simple_node_fields, R.string.oper_past_node_fields, R.string.oper_continuous_node_fields),
        RECORD(R.array.obj_record),//RECORD (R.string.oper_simple_record, R.string.oper_past_record, R.string.oper_continuous_record),
        RECORD_FIELDS(R.array.obj_record_fields),//RECORD_FIELDS (R.string.oper_simple_record_fields, R.string.oper_past_record_fields, R.string.oper_continuous_record_fields),
        RECORD_DIR(R.array.obj_record_dir),//RECORD_DIR (R.string.oper_simple_record_dir, R.string.oper_past_record_dir, R.string.oper_continuous_record_dir),
        FILE(R.array.obj_file),//FILE (R.string.oper_simple_file, R.string.oper_past_file, R.string.oper_continuous_file),
        FILE_FIELDS(R.array.obj_file_fields);//FILE_FIELDS (R.string.oper_simple_file_fields, R.string.oper_past_file_fields, R.string.oper_continuous_file_fields);

//        int[] mRes = new int[3];

//        Objs(int simple, int perfect, int continuous) {
//            this.mRes[0] = simple;
//            this.mRes[1] = perfect;
//            this.mRes[2] = continuous;
//        }

//        int getResId(int tense) {
//            return (tense >= 0 && tense < 3) ? mRes[tense] : 0;
//        }

        int maRes;

        Objs(int arrayRes) {
            this.maRes = arrayRes;
        }

        String getString(int tense) {
            return (tense >= 0 && tense < 3) ? context.getResources().getStringArray(maRes)[tense] : null;
        }
    }

    public enum Opers {
        LOAD(R.array.oper_load),
        CREATE(R.array.oper_create),//CREATE (R.string.create, R.string.created, R.string.creating),
        ADD(R.array.oper_add),//ADD (R.string.add, R.string.added, R.string.adding),
        CHANGE(R.array.oper_change),//CHANGE (R.string.change, R.string.changed, R.string.changing),
        RENAME(R.array.oper_rename),//RENAME (R.string.rename, R.string.renamed, R.string.renaming),
        DELETE(R.array.oper_delete),//DELETE (R.string.delete, R.string.deleted, R.string.deleting),
        COPY(R.array.oper_copy),//COPY (R.string.copy, R.string.copied, R.string.copying),
        CUT(R.array.oper_cut),//CUT (R.string.cut, R.string.cutted, R.string.cutting),
        INSERT(R.array.oper_insert),//INSERT (R.string.insert, R.string.inserted, R.string.inserting),
        MOVE(R.array.oper_move),//MOVE (R.string.move, R.string.moved, R.string.moving),
        SAVE(R.array.oper_save),//SAVE (R.string.save, R.string.saved, R.string.saving),
        ATTACH(R.array.oper_attach),//ATTACH (R.string.attach, R.string.attached, R.string.attaching),
        ENCRYPT(R.array.oper_encrypt),//ENCRYPT (R.string.encrypt, R.string.encrypted, R.string.encrypting),
        DECRYPT(R.array.oper_decrypt),//DECRYPT (R.string.decrypt, R.string.decrypted, R.string.decrypting);
        REENCRYPT(R.array.oper_reencrypt);

//        int[] mRes = new int[3];
//
//        Opers(int simple, int perfect, int continuous) {
//            this.mRes[0] = simple;
//            this.mRes[1] = perfect;
//            this.mRes[2] = continuous;
//        }
//
//        int getResId(int tense) {
//            return (tense >= 0 && tense < 3) ? mRes[tense] : 0;
//        }

        int maRes;

        Opers(int arrayRes) {
            this.maRes = arrayRes;
        }

        String getString(int tense) {
            return (tense >= 0 && tense < 3) ? context.getResources().getStringArray(maRes)[tense] : null;
        }
    }

    public static String logOperStart(Objs obj, Opers oper) {
        // меняем местами существительное и глагол в зависимости от языка
        String first = ((App.isRusLanguage()) ? oper.getString(PRESENT_CONTINUOUS) : obj.getString(PRESENT_CONTINUOUS));
        String second = ((App.isRusLanguage()) ? obj.getString(PRESENT_CONTINUOUS) : oper.getString(PRESENT_CONTINUOUS));
        String mes = String.format(getString(R.string.log_oper_start_mask), first, second);
        LogManager.log(mes, Types.ERROR);
        return mes;
    }

    public static String logOperCancel(Objs obj, Opers oper) {
        String mes = String.format(getString(R.string.log_oper_cancel_mask),
                (obj.getString(PRESENT_CONTINUOUS)), (oper.getString(PRESENT_CONTINUOUS)));
        LogManager.log(mes, Types.DEBUG);
        return mes;
    }

    public static String logOperRes(Objs obj, Opers oper) {
        return logOperRes(obj, oper, Toast.LENGTH_SHORT);
    }

    public static String logOperRes(Objs obj, Opers oper, int length) {
        String mes = (obj.getString(PAST_PERFECT)) + (oper.getString(PAST_PERFECT));
        LogManager.log(mes, Types.INFO, length);
        return mes;
    }

    public static String logOperError(Objs obj, Opers oper) {
        return logOperError(obj, oper, Toast.LENGTH_LONG);
    }

    public static String logOperError(Objs obj, Opers oper, int length) {
        String mes = String.format(getString(R.string.log_oper_error_mask),
                (oper.getString(PRESENT_SIMPLE)), (obj.getString(PRESENT_SIMPLE)));
        LogManager.log(mes, Types.ERROR, length);
        return mes;
    }

    public static String logDuringOperErrors(Objs obj, Opers oper, int length) {
        String mes = String.format(getString(R.string.log_during_oper_errors_mask),
                (oper.getString(PRESENT_CONTINUOUS)), (obj.getString(PRESENT_CONTINUOUS)));
        LogManager.log(mes, Types.ERROR, length);
        return mes;
    }

    private static String getString(int resId) {
        return context.getString(resId);
    }
}

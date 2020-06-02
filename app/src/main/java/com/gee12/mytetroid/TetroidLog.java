package com.gee12.mytetroid;

import android.widget.Toast;

import androidx.annotation.StringRes;

import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.utils.Utils;

import org.jetbrains.annotations.NotNull;

public class TetroidLog extends LogManager {

    public static final int PRESENT_SIMPLE = 0;
    public static final int PAST_PERFECT = 1;
    public static final int PRESENT_CONTINUOUS = 2;

    public enum Objs {
        STORAGE(R.array.obj_storage),
        NODE(R.array.obj_node),
        NODE_FIELDS(R.array.obj_node_fields),
        RECORD(R.array.obj_record),
        RECORD_FIELDS(R.array.obj_record_fields),
        RECORD_DIR(R.array.obj_record_dir),
        FILE(R.array.obj_file),
        FILE_FIELDS(R.array.obj_file_fields);

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
        CREATE(R.array.oper_create),
        ADD(R.array.oper_add),
        CHANGE(R.array.oper_change),
        RENAME(R.array.oper_rename),
        DELETE(R.array.oper_delete),
        COPY(R.array.oper_copy),
        CUT(R.array.oper_cut),
        INSERT(R.array.oper_insert),
        MOVE(R.array.oper_move),
        SAVE(R.array.oper_save),
        ATTACH(R.array.oper_attach),
        ENCRYPT(R.array.oper_encrypt),
        DECRYPT(R.array.oper_decrypt),
        DROPCRYPT(R.array.oper_dropcrypt),
        REENCRYPT(R.array.oper_reencrypt);

        int maRes;

        Opers(int arrayRes) {
            this.maRes = arrayRes;
        }

        String getString(int tense) {
            return (tense >= 0 && tense < 3) ? context.getResources().getStringArray(maRes)[tense] : null;
        }
    }

    public static String logOperStart(Objs obj, Opers oper) {
        return logOperStart(obj, oper, null);
    }

    public static String logOperStart(Objs obj, Opers oper, TetroidObject o) {
        // меняем местами существительное и глагол в зависимости от языка
        String first = ((App.isRusLanguage()) ? oper.getString(PRESENT_CONTINUOUS) : obj.getString(PRESENT_CONTINUOUS));
        String second = ((App.isRusLanguage()) ? obj.getString(PRESENT_CONTINUOUS) : oper.getString(PRESENT_CONTINUOUS));
        String mes = String.format(getString(R.string.log_oper_start_mask), first, second) + addIdName(o);
        LogManager.log(mes, Types.INFO);
        return mes;
    }

    public static String logOperCancel(Objs obj, Opers oper) {
        String mes = String.format(getString(R.string.log_oper_cancel_mask),
                (obj.getString(PRESENT_CONTINUOUS)), (oper.getString(PRESENT_CONTINUOUS)));
        LogManager.log(mes, Types.DEBUG);
        return mes;
    }

    public static String logOperRes(Objs obj, Opers oper) {
        return logOperRes(obj, oper, Toast.LENGTH_SHORT, null);
    }

    public static String logOperRes(Objs obj, Opers oper, TetroidObject o) {
        return logOperRes(obj, oper, Toast.LENGTH_SHORT, o);
    }

    public static String logOperRes(Objs obj, Opers oper, int length, TetroidObject o) {
        String mes = (obj.getString(PAST_PERFECT)) + (oper.getString(PAST_PERFECT)) + addIdName(o);
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

    private static String addIdName(TetroidObject o) {
        return (o != null) ? ": " + getIdNameString(o) : "";
    }

    /**
     * Формирование строки с именем и id объекта хранилища.
     * @param obj
     * @return
     */
    public static String getIdNameString(@NotNull TetroidObject obj) {
        return getStringFormat(R.string.log_obj_id_name_mask, obj.getId(), obj.getName());
    }

    public static String getStringFormat(@StringRes int formatRes, String... args) {
        return getStringFormat(context.getString(formatRes), args);
    }

    public static String getStringFormat(String formatRes, String... args) {
        return Utils.getStringFormat(formatRes, args);
    }
}

package com.gee12.mytetroid;

import android.widget.Toast;

import androidx.annotation.StringRes;

import com.gee12.mytetroid.activities.MainActivity;
import com.gee12.mytetroid.activities.SettingsActivity;
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
        FILE_FIELDS(R.array.obj_file_fields),
        CUR_PASS(),
        NEW_PASS();

        int maRes;

        Objs() {
            this.maRes = 0;
        }
        
        Objs(int arrayRes) {
            this.maRes = arrayRes;
        }

        String getString(int tense) {
            return (maRes > 0 && tense >= 0 && tense < 3) ? context.getResources().getStringArray(maRes)[tense] : "";
        }
    }

    public enum Opers {
        SET(R.array.oper_set),
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
        REENCRYPT(R.array.oper_reencrypt),
        CHECK();

        int maRes;

        Opers() {
            this.maRes = 0;
        }

        Opers(int arrayRes) {
            this.maRes = arrayRes;
        }

        String getString(int tense) {
            return (maRes > 0 && tense >= 0 && tense < 3) ? context.getResources().getStringArray(maRes)[tense] : "";
        }
    }

    public static String logOperStart(Objs obj, Opers oper) {
        return logOperStart(obj, oper, "");
    }

    public static String logOperStart(Objs obj, Opers oper, TetroidObject o) {
        return logOperStart(obj, oper, addIdName(o));
    }

    public static String logOperStart(Objs obj, Opers oper, String add) {
        // меняем местами существительное и глагол в зависимости от языка
        String first = ((App.isRusLanguage()) ? oper.getString(PRESENT_CONTINUOUS) : obj.getString(PRESENT_CONTINUOUS));
        String second = ((App.isRusLanguage()) ? obj.getString(PRESENT_CONTINUOUS) : oper.getString(PRESENT_CONTINUOUS));
        String mes = String.format(getString(R.string.log_oper_start_mask), first, second) + add;
        log(mes, Types.INFO);
        return mes;
    }

    public static String logOperCancel(Objs obj, Opers oper) {
        String mes = String.format(getString(R.string.log_oper_cancel_mask),
                (obj.getString(PRESENT_CONTINUOUS)), (oper.getString(PRESENT_CONTINUOUS)));
        log(mes, Types.DEBUG);
        return mes;
    }

    public static String logOperRes(Objs obj, Opers oper) {
        return logOperRes(obj, oper, "", Toast.LENGTH_SHORT);
    }

    public static String logOperRes(Objs obj, Opers oper, TetroidObject o) {
        return logOperRes(obj, oper, o, Toast.LENGTH_SHORT);
    }

    public static String logOperRes(Objs obj, Opers oper, TetroidObject o, int length) {
        return logOperRes(obj, oper, addIdName(o), length);
    }

    public static String logOperRes(Objs obj, Opers oper, String add, int length) {
        String mes = (obj.getString(PAST_PERFECT)) + (oper.getString(PAST_PERFECT)) + add;
        log(mes, Types.INFO, length);
        return mes;
    }

    public static String logOperErrorMore(Objs obj, Opers oper) {
        return logOperError(obj, oper, Toast.LENGTH_LONG);
    }

    public static String logOperErrorMore(Objs obj, Opers oper, int length) {
        return logOperError(obj, oper, null, true, length);
    }

    public static String logOperError(Objs obj, Opers oper, int length) {
        return logOperError(obj, oper, null, length >= 0, length);
    }

    public static String logOperError(Objs obj, Opers oper, String add, boolean more, int length) {
        String mes = String.format(getString(R.string.log_oper_error_mask),
                (oper.getString(PRESENT_SIMPLE)), (obj.getString(PRESENT_SIMPLE)),
                (add != null) ? add : "",
                (more) ? getString(R.string.log_more_in_logs) : "");
        log(mes, Types.ERROR, length);
        return mes;
    }

    public static String logDuringOperErrors(Objs obj, Opers oper, int length) {
        String mes = String.format(getString(R.string.log_during_oper_errors_mask),
                (oper.getString(PRESENT_CONTINUOUS)), (obj.getString(PRESENT_CONTINUOUS)));
        log(mes, Types.ERROR, length);
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
        return Utils.getStringFormat(context, formatRes, args);
    }

    /**
     *
     * @param stage
     * @return
     */
//    public static String logTaskStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage) {
    public static String logTaskStage(TaskStage stage) {
        switch (stage.stage) {
            case START:
                if (stage.clazz == SettingsActivity.ChangePassTask.class) {
                    switch (stage.oper) {
                        case CHECK:
                            return logTaskStage(stage, R.string.stage_pass_checking, Types.INFO);
                        case SET:
                            return logTaskStage(stage, (stage.obj == Objs.CUR_PASS)
                                    ? R.string.log_set_cur_pass : R.string.log_set_new_pass, Types.INFO);
                        case DECRYPT:
                            return logTaskStage(stage, R.string.stage_old_pass_decrypting, Types.INFO);
                        case REENCRYPT:
                            return logTaskStage(stage, R.string.stage_new_pass_reencrypting, Types.INFO);
                        case SAVE:
                            return logTaskStage(stage, (stage.obj == Objs.STORAGE)
                                    ? R.string.stage_storage_saving : R.string.log_save_pass, Types.INFO);
                        default:
                            return logOperStart(stage.obj, stage.oper);
                    }
                } else if (stage.clazz == MainActivity.CryptNodeTask.class) {
                    switch (stage.oper) {
                        case DECRYPT:
                            return logTaskStage(stage, R.string.stage_storage_decrypting, Types.INFO);
                        case ENCRYPT:
                            return logTaskStage(stage, R.string.task_node_encrypting, Types.INFO);
                        case DROPCRYPT:
                            return logTaskStage(stage, R.string.task_node_drop_crypting, Types.INFO);
                        default:
                            return logOperStart(stage.obj, stage.oper);
                    }
                }
                return logOperStart(stage.obj, stage.oper);
            case SUCCESS:
                return logOperRes(stage.obj, stage.oper, "", DURATION_NONE);
            case FAILED:
                return logDuringOperErrors(stage.obj, stage.oper, DURATION_NONE);
        }
        return null;
    }

    public static String logTaskStage(TaskStage taskStage, int resId, Types type) {
        String mes = getString(resId);
        if (taskStage.writeLog) {
            log(mes, type);
        }
        return mes;
    }
}

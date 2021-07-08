package com.gee12.mytetroid.logs;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.views.activities.MainActivity;
import com.gee12.mytetroid.views.fragments.settings.SettingsEncryptionFragment;
import com.gee12.mytetroid.model.TetroidObject;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.views.Message;
import com.gee12.mytetroid.views.fragments.settings.storage.StorageEncryptionSettingsFragment;

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
        TEMP_RECORD(R.array.obj_temp_record),
        RECORD_FIELDS(R.array.obj_record_fields),
        RECORD_DIR(R.array.obj_record_dir),
        TAG(R.array.obj_tag),
        FILE(R.array.obj_file),
        IMAGE(R.array.obj_image),
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

        String getString(Context context, int tense) {
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

        String getString(Context context, int tense) {
            return (maRes > 0 && tense >= 0 && tense < 3) ? context.getResources().getStringArray(maRes)[tense] : "";
        }
    }

    public static String logOperStart(Context context, Objs obj, Opers oper) {
        return logOperStart(context, obj, oper, "");
    }

    public static String logOperStart(Context context, Objs obj, Opers oper, TetroidObject o) {
        return logOperStart(context, obj, oper, addIdName(context, o));
    }

    public static String logOperStart(Context context, Objs obj, Opers oper, String add) {
        // меняем местами существительное и глагол в зависимости от языка
        String first = ((App.isRusLanguage()) ? oper.getString(context, PRESENT_CONTINUOUS)
                : obj.getString(context, PRESENT_CONTINUOUS));
        String second = ((App.isRusLanguage()) ? obj.getString(context, PRESENT_CONTINUOUS)
                : oper.getString(context, PRESENT_CONTINUOUS));
        String mes = String.format(context.getString(R.string.log_oper_start_mask), first, second) + add;
        log(context, mes, ILogger.Types.INFO);
        return mes;
    }

    public static String logOperCancel(Context context, Objs obj, Opers oper) {
        String mes = String.format(context.getString(R.string.log_oper_cancel_mask),
                (obj.getString(context, PRESENT_CONTINUOUS)), (oper.getString(context, PRESENT_CONTINUOUS)));
        log(context, mes, ILogger.Types.DEBUG);
        return mes;
    }

    public static String logOperRes(Context context, Objs obj, Opers oper) {
        return logOperRes(context, obj, oper, "", Toast.LENGTH_SHORT);
    }

    public static String logOperRes(Context context, Objs obj, Opers oper, TetroidObject o, boolean showAdd) {
        return logOperRes(context, obj, oper, addIdName(context, o), Toast.LENGTH_SHORT, showAdd);
    }

    public static String logOperRes(Context context, Objs obj, Opers oper, TetroidObject o, int duration) {
        return logOperRes(context, obj, oper, addIdName(context, o), duration);
    }

    public static String logOperRes(Context context, Objs obj, Opers oper, String add, int duration) {
        String mes = (obj.getString(context, PAST_PERFECT)) + (oper.getString(context, PAST_PERFECT)) + add;
        log(context, mes, ILogger.Types.INFO, duration);
        return mes;
    }

    public static String logOperRes(Context context, Objs obj, Opers oper, String add, int duration, boolean showAdd) {
        String mes = (obj.getString(context, PAST_PERFECT)) + (oper.getString(context, PAST_PERFECT));
        if (!showAdd) {
            Message.show(context, mes, duration);
            duration = -1;
        }
        mes += add;
        log(context, mes, ILogger.Types.INFO, duration);
        return mes;
    }

    public static String logOperErrorMore(Context context, Objs obj, Opers oper) {
        return logOperError(context, obj, oper, Toast.LENGTH_LONG);
    }

    public static String logOperErrorMore(Context context, Objs obj, Opers oper, int duration) {
        return logOperError(context, obj, oper, null, true, duration);
    }

    public static String logOperError(Context context, Objs obj, Opers oper, int duration) {
        return logOperError(context, obj, oper, null, duration >= 0, duration);
    }

    public static String logOperError(Context context, Objs obj, Opers oper, String add, boolean more, int duration) {
        String mes = String.format(context.getString(R.string.log_oper_error_mask),
                (oper.getString(context, PRESENT_SIMPLE)), (obj.getString(context, PRESENT_SIMPLE)),
                (add != null) ? add : "");
//                (more) ? context.getString(R.string.log_more_in_logs) : "");
        log(context, mes, ILogger.Types.ERROR, duration);
        if (more) {
            showSnackMoreInLogs(context);
        }
        return mes;
    }

    public static String logDuringOperErrors(Context context, Objs obj, Opers oper, int duration) {
        String mes = String.format(context.getString(R.string.log_during_oper_errors_mask),
                (oper.getString(context, PRESENT_CONTINUOUS)), (obj.getString(context, PRESENT_CONTINUOUS)));
        log(context, mes, ILogger.Types.ERROR, duration);
        showSnackMoreInLogs(context);
        return mes;
    }

//    private static String getString(Context context, int resId) {
//        return context.getString(resId);
//    }

    private static String addIdName(Context context, TetroidObject o) {
//        return (o != null) ? ": " + getIdNameString(o) : "";
        return (o != null) ? ": " + getIdString(context, o) : "";
    }

    /**
     * Формирование строки с именем и id объекта хранилища.
     * @param obj
     * @return
     */
    public static String getIdNameString(Context context, @NotNull TetroidObject obj) {
        return getStringFormat(context, R.string.log_obj_id_name_mask, obj.getId(), obj.getName());
    }

    /**
     * Формирование строки с id объекта хранилища.
     * @param obj
     * @return
     */
    public static String getIdString(Context context, @NotNull TetroidObject obj) {
        return getStringFormat(context, R.string.log_obj_id_mask, obj.getId());
    }

    public static String getStringFormat(Context context, @StringRes int formatRes, String... args) {
        return Utils.getStringFormat(context, formatRes, (args != null && args.length > 1) ? (Object[]) args : (Object)args);
    }

    /**
     *
     * @param stage
     * @return
     */
    public static String logTaskStage(Context context, TaskStage stage) {
        switch (stage.stage) {
            case START:
                if (stage.clazz == StorageEncryptionSettingsFragment.ChangePassTask.class) {
                    switch (stage.oper) {
                        case CHECK:
                            return logTaskStage(context, stage, R.string.stage_pass_checking, ILogger.Types.INFO);
                        case SET:
                            return logTaskStage(context, stage, (stage.obj == Objs.CUR_PASS)
                                    ? R.string.log_set_cur_pass : R.string.log_set_new_pass, ILogger.Types.INFO);
                        case DECRYPT:
                            return logTaskStage(context, stage, R.string.stage_old_pass_decrypting, ILogger.Types.INFO);
                        case REENCRYPT:
                            return logTaskStage(context, stage, R.string.stage_new_pass_reencrypting, ILogger.Types.INFO);
                        case SAVE:
                            return logTaskStage(context, stage, (stage.obj == Objs.STORAGE)
                                    ? R.string.stage_storage_saving : R.string.log_save_pass, ILogger.Types.INFO);
                        default:
                            return logOperStart(context, stage.obj, stage.oper);
                    }
                } else if (stage.clazz == MainActivity.CryptNodeTask.class) {
                    switch (stage.oper) {
                        case DECRYPT:
                            return logTaskStage(context, stage, R.string.stage_storage_decrypting, ILogger.Types.INFO);
                        case ENCRYPT:
                            return logTaskStage(context, stage, R.string.task_node_encrypting, ILogger.Types.INFO);
                        case DROPCRYPT:
                            return logTaskStage(context, stage, R.string.task_node_drop_crypting, ILogger.Types.INFO);
                        default:
                            return logOperStart(context, stage.obj, stage.oper);
                    }
                }
                return logOperStart(context, stage.obj, stage.oper);
            case SUCCESS:
                return logOperRes(context, stage.obj, stage.oper, "", DURATION_NONE);
            case FAILED:
                return logDuringOperErrors(context, stage.obj, stage.oper, DURATION_NONE);
        }
        return null;
    }

    public static String logTaskStage(Context context, TaskStage taskStage, int resId, ILogger.Types type) {
        String mes = context.getString(resId);
        if (taskStage.writeLog) {
            log(context, mes, type);
        }
        return mes;
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    protected static void showSnackMoreInLogs(Context context) {
        Message.showSnackMoreInLogs(context, R.id.layout_coordinator);
    }
}

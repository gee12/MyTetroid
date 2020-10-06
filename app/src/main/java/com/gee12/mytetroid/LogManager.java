package com.gee12.mytetroid;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.gee12.mytetroid.views.Message;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class LogManager {

    /**
     * Типы логов.
     */
    public enum Types {
        INFO,
        DEBUG,
        WARNING,
        ERROR
    }

    private static final String LOG_TAG = "MYTETROID";
    private static final int CALLER_STACK_INDEX = 5;
    public static final int DURATION_NONE = -1;

    // FIXME: Переписать, чтобы использовать Singleton
    //  и не хранить context в static (получать всегда параметром)
//    private static LogManager instance;

//    protected static Context context;
    private static String dirPath;
    private static String fullFileName;
    private static boolean isWriteToFile;
    private static StringBuilder buffer = new StringBuilder();

    /**
     * Инициализация менеджера.
//     * @param ctx
     * @param path
     * @param isWriteToFile
     */
    public static void init(Context context, String path, boolean isWriteToFile) {
//        instance = new LogManager();
//        LogManager.context = ctx;
        setLogPath(context, path);
        LogManager.isWriteToFile = isWriteToFile;
//        LogManager.buffer = new StringBuilder();
    }

    public static void setLogPath(Context context, String path) {
        LogManager.dirPath = path;
        LogManager.fullFileName = String.format("%s%s%s.log", path, File.separator, context.getString(R.string.app_name));
    }

    public static void log(Context context, String mes, Types type, boolean isWriteToFile, int duration) {
        if (type == Types.DEBUG && !BuildConfig.DEBUG)
            return;
        String typeTag;
        switch(type) {
            case INFO:
                typeTag = "INFO: ";
                Log.i(LOG_TAG, mes);
                break;
            case WARNING:
                typeTag = "WARN: ";
                Log.w(LOG_TAG, mes);
                break;
            case ERROR:
                typeTag = "ERROR: ";
                Log.e(LOG_TAG, mes);
                break;
            default:
                typeTag = "DEBUG: ";
                Log.d(LOG_TAG, mes);
                break;
        }
        String fullMes = addTime(typeTag + mes);
        writeToBuffer(fullMes);
        if (isWriteToFile) {
            writeToFile(context, fullMes);
        }
        if (duration >= 0) {
            showMessage(context, mes, duration);
        }
    }

    public static void log(Context context, String s, Types type, int duration) {
        log(context, s, type, isWriteToFile, duration);
    }

    public static void log(Context context, String s, Types type) {
        log(context, s, type, isWriteToFile, DURATION_NONE);
    }

    public static void log(Context context, String s) {
        log(context, s, Types.INFO);
    }

    public static void log(Context context, int sId) {
        log(context, context.getString(sId), Types.INFO);
    }

    public static void log(Context context, String s, int duration) {
        log(context, s, Types.INFO, duration);
    }

    public static void log(Context context, int sId, int duration) {
        log(context, context.getString(sId), duration);
    }

    public static void log(Context context, int sId, Types type) {
        log(context, context.getString(sId), type, DURATION_NONE);
    }

    public static void log(Context context, int sId, Types type, int duration) {
        log(context, context.getString(sId), type, duration);
    }

    public static void log(Context context, Exception ex) {
        log(context, getExceptionInfo(ex), Types.ERROR);
    }

    public static void log(Context context, String s, Exception ex) {
        log(context, s + ": " + getExceptionInfo(ex), Types.ERROR);
    }

    public static void log(Context context, Exception ex, int duration) {
        log(context, getExceptionInfo(ex), Types.ERROR, duration);
    }

    public static void log(Context context, String s, Exception ex, int duration) {
        log(context, s + ": " + getExceptionInfo(ex), Types.ERROR, duration);
    }

    public static void showMessage(Context context, String s, int duration) {
        Message.show(context, s, duration);
    }

    /*public static void showMessage(Context context, String s, int duration) {
        Message.show(context, s, duration);
    }*/

    private static void addLogWithoutFile(Context context, Exception ex, int duration) {
        addLogWithoutFile(context, getExceptionInfo(ex), duration);
    }

    private static void addLogWithoutFile(Context context, String s, int duration) {
        log(context, s, Types.ERROR, false, duration);
    }

    private static String addTime(String s) {
        return String.format("%s - %s", DateFormat.format("yyyy.MM.dd HH:mm:ss",
                Calendar.getInstance().getTime()), s);
    }

    /**
     * Сообщение о параметре=null
     * @param methodName
     */
    public static void emptyParams(Context context, String methodName) {
        String start = (!TextUtils.isEmpty(methodName)) ? methodName + ": " : "";
        log(context, start + "Some required parameter(s) is null", LogManager.Types.WARNING);
    }

    /**
     * Сообщение о параметре context=null
     */
    public static void emptyContextLog(Context context) {
        log(context, "Parameter <context> is null", LogManager.Types.WARNING);
    }

    /**
     *
     * @param ex
     * @return
     */
    public static String getExceptionInfo(Exception ex) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[CALLER_STACK_INDEX];

        String fullClassName = caller.getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        String methodName = caller.getMethodName();
        int lineNumber = caller.getLineNumber();

        return String.format(Locale.getDefault(), "%s.%s():%d\n%s", className, methodName, lineNumber, ex.getMessage());
    }

    /**
     * Запись логов в буфер.
     * @param s
     */
    private static void writeToBuffer(String s) {
        buffer.append(s);
        buffer.append("\n");
    }

    /**
     * Запись логов в файл.
     * @param mes
     */
    private static void writeToFile(Context context, String mes) {
        File logFile = new File(fullFileName);
        if (!logFile.exists()) {
            try {
                // проверка существования каталога
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
//                        log(context.getString(R.string.log_dir_creating_error) + dirPath,
//                                Types.ERROR, false, Toast.LENGTH_LONG);
                        addLogWithoutFile(context, context.getString(R.string.log_dir_creating_error) + dirPath, Toast.LENGTH_LONG);
                        //
//                        writeToBuffer(mes);
                        return;
                    } else {
                        log(context, context.getString(R.string.log_created_log_dir) + dirPath, Types.DEBUG);
                    }
                }
                // попытка создания лог-файла
                logFile.createNewFile();
            }
            catch (IOException e) {
                addLogWithoutFile(context, e, Toast.LENGTH_LONG);
                //
//                writeToBuffer(mes);
                return;
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(mes);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            addLogWithoutFile(context, e, Toast.LENGTH_LONG);
            //
//            writeToBuffer(mes);
        }
    }

    /**
     * Получение полного пути к лог-файлу.
     * @return
     */
    public static String getFullFileName() {
        return fullFileName;
    }

    /**
     * Получение списка логов, которые не удалось записать в файл.
     * @return
     */
    public static String getBufferString() {
        return buffer.toString();
    }
}

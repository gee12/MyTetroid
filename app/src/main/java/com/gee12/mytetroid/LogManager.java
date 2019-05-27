package com.gee12.mytetroid;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class LogManager {

    public enum Types {
        INFO,
        DEBUG,
        WARNING,
        ERROR
    }

    private static final String LOG_TAG = "MYTETROID";
    private static final int CALLER_STACK_INDEX = 5;

    private static Context context;
    private static  String fullFileName;
    private static boolean isWriteToFile;

    public static void init(Context ctx, String path, boolean isWriteToFile) {
        LogManager.context = ctx;
        setLogPath(path);
        LogManager.isWriteToFile = isWriteToFile;
    }

    public static void setLogPath(String path) {
        LogManager.fullFileName = String.format("%s%s%s.log", path, File.separator, context.getString(R.string.app_name));
    }

    public static void addLog(String s, Types type, boolean isWriteToFile, int duration) {
        if (type == Types.DEBUG && !BuildConfig.DEBUG)
            return;
        String typeTag;
        switch(type) {
            case INFO:
                typeTag = "INFO: ";
                Log.i(LOG_TAG, s);
                break;
            case WARNING:
                typeTag = "WARN: ";
                Log.w(LOG_TAG, s);
                break;
            case ERROR:
                typeTag = "ERROR: ";
                Log.e(LOG_TAG, s);
                break;
            default:
                typeTag = "DEBUG: ";
                Log.d(LOG_TAG, s);
                break;
        }
        if (isWriteToFile)
            writeToFile(typeTag + s);
        if (duration >= 0)
            showToast(s, duration);
    }

    public static void addLog(String s, Types type, int duration) {
        addLog(s, type, isWriteToFile, duration);
    }

    public static void addLog(String s, Types type) {
        addLog(s, type, isWriteToFile, -1);
    }

    public static void addLog(String s) {
        addLog(s, Types.INFO);
    }

    public static void addLog(int sId) {
        addLog(context.getString(sId), Types.INFO);
    }

    public static void addLog(String s, int duration) {
        addLog(s, Types.INFO, duration);
    }

    public static void addLog(int sId, int duration) {
        addLog(context.getString(sId), duration);
    }

    public static void addLog(Exception ex) {
        addLog(getExceptionInfo(ex), Types.ERROR);
    }

    public static void addLog(String s, Exception ex) {
        addLog(s + getExceptionInfo(ex), Types.ERROR);
    }

    public static void addLog(Exception ex, int duration) {
        addLog(getExceptionInfo(ex), Types.ERROR, duration);
    }

    public static void addLog(String s, Exception ex, int duration) {
        addLog(s + getExceptionInfo(ex), Types.ERROR, duration);
    }

    public static void showToast(String s, int duration) {
        Toast.makeText(context, s, duration).show();
    }

    private static void addLogWithoutFile(Exception ex, int duration) {
        addLog(getExceptionInfo(ex), Types.ERROR, false, duration);
    }

    private static String createMessage(String s) {
        return String.format("%s - %s", DateFormat.format("yyyy.MM.dd HH:mm:ss",
                Calendar.getInstance().getTime()), s);
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

        return String.format("%s.%s():%d\n%s", className, methodName, lineNumber, ex.getMessage());
    }

    private static void writeToFile(String s) {
        File logFile = new File(fullFileName);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            }
            catch (IOException e) {
                addLogWithoutFile(e, Toast.LENGTH_LONG);
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(createMessage(s));
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            addLogWithoutFile(e, Toast.LENGTH_LONG);
        }
    }
}

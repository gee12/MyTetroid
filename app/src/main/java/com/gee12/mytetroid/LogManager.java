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

    public static final String LOG_TAG = "MYTETROID";

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

    public static void setIsWriteToFile(boolean isWriteToFile) {
        LogManager.isWriteToFile = isWriteToFile;
    }

    public static void addLog(String s) {
        Log.i(LOG_TAG, s);
        if (isWriteToFile)
            writeToFile(s);
    }

    public static void addLog(int sId) {
        String mes = context.getString(sId);
        Log.i(LOG_TAG, mes);
        if (isWriteToFile)
            writeToFile(mes);
    }

    public static void addLog(String s, int duration) {
        addLog(s);
        showToast(s, duration);
    }

    public static void addLog(int sId, int duration) {
        String mes = context.getString(sId);
        addLog(mes);
        showToast(mes, duration);
    }

    public static void addLog(Exception ex) {
        String mes = ex.getMessage();
        Log.e(LOG_TAG, mes);
        if (isWriteToFile)
            writeToFile(mes);
    }

    public static void addLog(String s, Exception ex) {
        String mes = s + ex.getMessage();
        Log.e(LOG_TAG, mes);
        if (isWriteToFile)
            writeToFile(mes);
    }

    public static void addLog(Exception ex, int duration) {
        addLog(ex);
        showToast(ex.getMessage(), duration);
    }

    public static void addLog(String s, Exception ex, int duration) {
        addLog(s, ex);
        showToast(s + ex.getMessage(), duration);
    }

    public static void showToast(String s, int duration) {
        Toast.makeText(context, s, duration).show();
    }

    private static void addLogWithoutFile(Exception ex, int duration) {
        String mes = ex.getMessage();
        Log.e(LOG_TAG, mes);
        showToast(mes, duration);
    }

    private static String createMessage(String s) {
        return String.format("%s - %s", DateFormat.format("yyyy.MM.dd hh:mm:ss",
                Calendar.getInstance().getTime()), s);
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

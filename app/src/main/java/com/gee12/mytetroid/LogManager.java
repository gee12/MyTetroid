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

    public static void init(Context ctx, String path) {
        context = ctx;
        fullFileName = String.format("%s%s%s.log", path, File.separator, context.getString(R.string.app_name));
    }

    public static void addLog(String s) {
        Log.i(LOG_TAG, s);
        writeToFile(s);
    }

    public static void addLog(String s, int duration) {
        addLog(s);
        showToast(s, duration);
    }

    public static void addLog(Exception ex) {
        String s = ex.getMessage();
        Log.e(LOG_TAG, s);
        writeToFile(s);
    }

    public static void addLog(Exception ex, int duration) {
        addLog(ex);
        showToast(ex.getMessage(), duration);
    }

    public static void showToast(String s, int duration) {
        Toast.makeText(context, s, duration).show();
    }

    private static String createMessage(String s) {
        return String.format("%s - %s", DateFormat.format("yyyy.MM.dd hh:mm:ss",
                Calendar.getInstance().getTime()), s);
    }

    public static void writeToFile(String s) {
        File logFile = new File(fullFileName);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(createMessage(s));
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

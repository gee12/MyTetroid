package com.gee12.mytetroid;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class LogManager {

    public static final String LOG_TAG = "MYTETROID";

    private static Context context;

    public static void init(Context ctx) {
        context = ctx;
    }

    public static void addLog(String s) {
        Log.i(LOG_TAG, s);
    }

    public static void addLog(String s, int duration) {
        addLog(s);
        showToast(s, duration);
    }

    public static void addLog(Exception ex) {
        Log.e(LOG_TAG, ex.getMessage());
    }

    public static void addLog(Exception ex, int duration) {
        addLog(ex);
        showToast(ex.getMessage(), duration);
    }

    public static void showToast(String s, int duration) {
        Toast.makeText(context, s, duration).show();
    }
}

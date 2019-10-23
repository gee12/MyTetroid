package com.gee12.mytetroid;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class Message {

    public static void show(Context context, String text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

    public static void show(Context context, String text) {
        show(context, text, Toast.LENGTH_LONG);
    }

    public static void show(View view, String text, int duration) {
        Snackbar.make(view, text, duration)
                .setAction("Action", null).show();
    }

    public static void show(View view, String text) {
        show(view, text, Snackbar.LENGTH_LONG);
    }
}

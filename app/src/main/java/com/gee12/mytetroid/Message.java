package com.gee12.mytetroid;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class Message {

    public static void create(Context context, String text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

    public static void create(Context context, String text) {
        create(context, text, Toast.LENGTH_LONG);
    }

    public static void create(View view, String text, int duration) {
        Snackbar.make(view, text, duration)
                .setAction("Action", null).show();
    }

    public static void create(View view, String text) {
        create(view, text, Snackbar.LENGTH_LONG);
    }
}

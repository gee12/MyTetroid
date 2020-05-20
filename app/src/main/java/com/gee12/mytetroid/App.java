package com.gee12.mytetroid;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;

import com.gee12.mytetroid.utils.ViewUtils;

import java.util.Locale;

public class App {

    /**
     * Проверка - полная ли версия
     * @return
     */
    public static boolean isFullVersion() {
        return (BuildConfig.FLAVOR.equals("pro"));
    }

    /**
     * Переключатель полноэкранного режима.
     * @param activity
     * @return текущий режим
     */
    public static boolean toggleFullscreen(AppCompatActivity activity) {
        boolean newValue = !SettingsManager.IsFullScreen;
        SettingsManager.IsFullScreen = newValue;
        ViewUtils.setFullscreen(activity, newValue);
        return newValue;
    }

    /**
     * Переключатель блокировки выключения экрана.
     * @param activity
     */
    public static void setKeepScreenOn(Activity activity) {
        ViewUtils.setKeepScreenOn(activity, SettingsManager.isKeepScreenOn());
    }

    public static boolean isRusLanguage() {
        return (Locale.getDefault().getLanguage().equals("ru"));
    }
}

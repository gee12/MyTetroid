package com.gee12.mytetroid;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.ColorInt;

import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.utils.Utils;
import com.gee12.mytetroid.utils.ViewUtils;

import java.util.Locale;

public class App {

    public static boolean IsInited;
    public static boolean IsHighlightAttach;
    public static boolean IsHighlightCryptedNodes;
    @ColorInt public static int HighlightAttachColor;
    public static String DateFormatString;
//    public static boolean IsFullScreen;
    public static boolean IsLoadedFavoritesOnly;

    /**
     * Проверка - полная ли версия.
     * @return
     */
    public static boolean isFullVersion() {
        return (BuildConfig.FLAVOR.equals("pro"));
    }

    /**
     * Проверка - обычная ли версия.
     * @return
     */
    public static boolean isFreeVersion() {
        return (BuildConfig.FLAVOR.equals("free"));
    }

//    /**
//     * Переключатель полноэкранного режима.
//     * @param activity
//     * @return текущий режим
//     */
//    public static int toggleFullscreen(AppCompatActivity activity, boolean fromDoubleTap) {
//        if (!fromDoubleTap || SettingsManager.isDoubleTapFullscreen(activity)) {
//            boolean newValue = !IsFullScreen;
//            ViewUtils.setFullscreen(activity, newValue);
//            IsFullScreen = newValue;
//            return (newValue) ? 1 : 0;
//        } else {
//            return -1;
//        }
//    }

    /**
     * Переключатель блокировки выключения экрана.
     * @param activity
     */
    public static void checkKeepScreenOn(Activity activity) {
        ViewUtils.setKeepScreenOn(activity, SettingsManager.isKeepScreenOn(activity));
    }

    public static boolean isRusLanguage() {
        return (Locale.getDefault().getLanguage().equals("ru"));
    }

    /**
     * Первоначальная инициализация служб приложения.
     */
    public static void init(Context context) {
        if (IsInited)
            return;
        SettingsManager.init(context);
        LogManager.init(context, SettingsManager.getLogPath(context), SettingsManager.isWriteLogToFile(context));
        LogManager.log(context, String.format(context.getString(R.string.log_app_start_mask), Utils.getVersionName(context)));
        if (SettingsManager.isCopiedFromFree()) {
            LogManager.log(context, R.string.log_settings_copied_from_free, ILogger.Types.INFO);
        }

        // отключаем с версии 3.5
        SettingsManager.setIsSyncStorage(context, false);

        IsInited = true;
    }
}

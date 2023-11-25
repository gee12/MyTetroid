package com.gee12.mytetroid.domain

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.gee12.mytetroid.model.enums.AppTheme

object AppThemeHelper {

    fun setTheme(theme: AppTheme) {
        getNightModeFromTheme(theme).also { mode ->
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    fun getNightModeFromTheme(theme: AppTheme): Int {
        return when (theme) {
            AppTheme.SYSTEM -> {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppTheme.LIGHT -> {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppTheme.DARK -> {
                AppCompatDelegate.MODE_NIGHT_YES
            }
        }
    }

    fun setNightMode(context: Context, isNightMode: Boolean) {
        if (Build.VERSION.SDK_INT >= 31) {
            val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            val mode = if (isNightMode) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            uiManager?.setApplicationNightMode(mode)
        } else {
            val mode = if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

}
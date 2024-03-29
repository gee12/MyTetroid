package com.gee12.mytetroid.domain

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.gee12.mytetroid.model.enums.AppTheme
import com.gee12.mytetroid.model.enums.EditorTheme

object AppThemeHelper {

    fun setAppTheme(theme: AppTheme) {
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

    fun WebView.setNightMode(appTheme: AppTheme, editorTheme: EditorTheme) {
        when (editorTheme) {
            EditorTheme.AS_APP_THEME -> {
                when (appTheme) {
                    AppTheme.SYSTEM -> {
                        val isNightModeEnabled = context.isNightModeEnabled() ?: false
                        setNightMode(isNightModeEnabled)
                    }
                    AppTheme.LIGHT -> {
                        setNightMode(false)
                    }
                    AppTheme.DARK -> {
                        setNightMode(true)
                    }
                }
            }
            EditorTheme.LIGHT -> {
                setNightMode(false)
            }
            EditorTheme.DARK -> {
                setNightMode(true)
            }
        }
    }

    fun WebView.setNightMode(isNightMode: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isNightMode)

        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            val mode = if (isNightMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            WebSettingsCompat.setForceDark(settings, mode)
        }
    }

    fun Context.isNightModeEnabled(): Boolean? {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                false
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                true
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                null
            }
            else -> {
                null
            }
        }
    }

}
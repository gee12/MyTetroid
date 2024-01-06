package com.gee12.mytetroid.ui.settings

import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.base.TetroidSettingsActivity

/**
 * Активность для управления настройками приложения.
 */
class SettingsActivity : TetroidSettingsActivity<CommonSettingsViewModel>() {

    override fun getLayoutResourceId() =  R.layout.activity_settings

    override fun getViewModelClazz() = CommonSettingsViewModel::class.java

    override fun isSingleTitle() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun startDefaultFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, SettingsSectionsFragment())
            .commit()
    }

    override fun onBackPressed() {
        val fragment = currentFragment
        if (fragment !is SettingsSectionsFragment) {
            setTitle(R.string.action_common_settings)
        }
        super.onBackPressed()
    }

}
package com.gee12.mytetroid.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.fragments.settings.SettingsOtherFragment
import com.gee12.mytetroid.ui.fragments.settings.SettingsSectionsFragment
import com.gee12.mytetroid.ui.fragments.settings.SettingsStorageFragment

/**
 * Активность для управления настройками приложения.
 */
class SettingsActivity : TetroidSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.activity_settings
    }

    override fun startDefaultFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, SettingsSectionsFragment())
            .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val permGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        val fragment = getCurrentFragment()
        if (fragment is SettingsStorageFragment) {
            fragment.onRequestPermissionsResult(permGranted, requestCode)
        } else if (fragment is SettingsOtherFragment) {
            fragment.onRequestPermissionsResult(permGranted, requestCode)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val fragment = getCurrentFragment()
        if (fragment is SettingsStorageFragment) {
            fragment.onResult(requestCode, resultCode, data!!)
        } else if (fragment is SettingsOtherFragment) {
            fragment.onResult(requestCode, resultCode, data!!)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
//        boolean isBackPressed = false;
        val fragment = getCurrentFragment()
        // обрабатываем нажатие Back во фрагменте SettingsEncryptionFragment отдельно
//        if (fragment instanceof SettingsEncryptionFragment) {
//            if (!((SettingsEncryptionFragment)fragment).onBackPressed()) {
//                isBackPressed = true;
//            }
//        } else {
//            isBackPressed = true;
//        }
//        if (isBackPressed) {
        if (fragment !is SettingsSectionsFragment) {
            setTitle(R.string.title_settings)
        }
        super.onBackPressed()
//        }
    }
}
package com.gee12.mytetroid.views.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.views.fragments.settings.SettingsSectionsFragment;
import com.gee12.mytetroid.views.fragments.settings.SettingsOtherFragment;
import com.gee12.mytetroid.views.fragments.settings.SettingsStorageFragment;

import org.jetbrains.annotations.NotNull;

/**
 * Активность для управления настройками приложения.
 */
public class SettingsActivity extends TetroidSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_settings;
    }

    @Override
    protected void startDefaultFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsSectionsFragment())
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof SettingsStorageFragment) {
            ((SettingsStorageFragment)fragment).onRequestPermissionsResult(permGranted, requestCode);
        } else if (fragment instanceof SettingsOtherFragment) {
            ((SettingsOtherFragment)fragment).onRequestPermissionsResult(permGranted, requestCode);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof SettingsStorageFragment) {
            ((SettingsStorageFragment)fragment).onResult(requestCode, resultCode, data);
        } else if (fragment instanceof SettingsOtherFragment) {
            ((SettingsOtherFragment)fragment).onResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
//        boolean isBackPressed = false;
        Fragment fragment = getCurrentFragment();
//        // обрабатываем нажатие Back во фрагменте SettingsEncryptionFragment отдельно
//        if (fragment instanceof SettingsEncryptionFragment) {
//            if (!((SettingsEncryptionFragment)fragment).onBackPressed()) {
//                isBackPressed = true;
//            }
//        } else {
//            isBackPressed = true;
//        }
//        if (isBackPressed) {
            if (!(fragment instanceof SettingsSectionsFragment)) {
                setTitle(R.string.title_settings);
            }
            super.onBackPressed();
//        }
    }
}
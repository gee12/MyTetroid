package com.gee12.mytetroid.views.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;

import androidx.fragment.app.Fragment;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.model.TetroidStorage;
import com.gee12.mytetroid.views.fragments.settings.storage.StorageEncryptionSettingsFragment;
import com.gee12.mytetroid.views.fragments.settings.storage.StorageMainSettingsFragment;
import com.gee12.mytetroid.views.fragments.settings.storage.StorageSectionsSettingsFragment;
import com.gee12.mytetroid.views.fragments.settings.storage.TetroidStorageSettingsFragment;

import org.jetbrains.annotations.NotNull;

/**
 * Активность для управления настройками хранилища.
 * (замена SettingsManager для параметров хранилища)
 */
public class StorageSettingsActivity extends TetroidSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public int getStorageId() {
        return (getIntent() != null && getIntent().hasExtra(Constants.EXTRA_STORAGE_ID))
                ? getIntent().getIntExtra(Constants.EXTRA_STORAGE_ID, 0) : 0;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_settings;
    }

    @Override
    protected void startDefaultFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new StorageSectionsSettingsFragment())
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof StorageMainSettingsFragment) {
            ((StorageMainSettingsFragment)fragment).onRequestPermissionsResult(permGranted, requestCode);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof StorageMainSettingsFragment) {
            ((StorageMainSettingsFragment)fragment).onResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.storage_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof TetroidStorageSettingsFragment) {
            if (!((TetroidStorageSettingsFragment)fragment).onBackPressed()) {
                super.onBackPressed();
            }
        }
        else {
            super.onBackPressed();
        }
    }

    public static Intent newIntent(Context context, TetroidStorage storage) {
        Intent intent = new Intent(context, StorageSettingsActivity.class);
        intent.putExtra(Constants.EXTRA_STORAGE_ID, storage.getId());
        return intent;
    }
}
package com.gee12.mytetroid.views.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.model.TetroidStorage;
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel;
import com.gee12.mytetroid.viewmodels.StorageViewModelFactory;
import com.gee12.mytetroid.views.fragments.settings.SettingsFragment;
import com.gee12.mytetroid.views.fragments.settings.storage.StorageEncryptionSettingsFragment;
import com.gee12.mytetroid.views.fragments.settings.storage.StorageMainSettingsFragment;
import com.gee12.mytetroid.views.fragments.settings.storage.StorageSettingsFragment;

import org.jetbrains.annotations.NotNull;

/**
 * Активность для управления настройками хранилища.
 * (замена SettingsManager для параметров хранилища)
 */
public class StorageSettingsActivity extends TetroidSettingsActivity {

    private StorageSettingsViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mViewModel = new ViewModelProvider(this, new StorageViewModelFactory(getApplication()))
                .get(StorageSettingsViewModel.class);

        int storageId = (getIntent() != null && getIntent().hasExtra(Constants.EXTRA_STORAGE_ID))
                ? getIntent().getIntExtra(Constants.EXTRA_STORAGE_ID, 0) : 0;
        mViewModel.setStorageFromBase(storageId);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_settings;
    }

    @Override
    protected void startDefaultFragment() {
//        int storageId = (getIntent() != null && getIntent().hasExtra(EXTRA_STORAGE_ID))
//                ? getIntent().getIntExtra(EXTRA_STORAGE_ID, 0) : 0;

        getSupportFragmentManager()
                .beginTransaction()
//                .replace(R.id.container, StorageSettingsFragment.Companion.newInstance(storageId))
                .replace(R.id.container, new StorageSettingsFragment())
                .commit();
    }

    /**
     * TODO
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        Fragment fragment = getCurFragment();
        if (fragment instanceof StorageMainSettingsFragment) {
            ((StorageMainSettingsFragment)fragment).onRequestPermissionsResult(permGranted, requestCode);
        } /*else if (fragment instanceof SettingsOtherFragment) {
            ((SettingsOtherFragment)fragment).onRequestPermissionsResult(permGranted, requestCode);
        }*/
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * TODO
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getCurFragment();
        /*if (fragment instanceof StorageMainSettingsFragment) {
            ((StorageMainSettingsFragment)fragment).onResult(requestCode, resultCode, data);
        }*/ /*else if (fragment instanceof SettingsOtherFragment) {
            ((SettingsOtherFragment)fragment).onResult(requestCode, resultCode, data);
        }*/
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
    TODO
     */
    @Override
    public void onBackPressed() {
        boolean isBackPressed = false;
        Fragment fragment = getCurFragment();
        // обрабатываем нажатие Back во фрагменте StorageEncryptionSettingsFragment отдельно
        if (fragment instanceof StorageEncryptionSettingsFragment) {
            if (!((StorageEncryptionSettingsFragment)fragment).onBackPressed()) {
                isBackPressed = true;
            }
        } else {
            isBackPressed = true;
        }
        if (isBackPressed) {
            if (!(fragment instanceof SettingsFragment)) {
                setTitle(R.string.title_storage_settings);
            }
            super.onBackPressed();
        }
    }

    public static Intent newIntent(Context context, TetroidStorage storage) {
        Intent intent = new Intent(context, StorageSettingsActivity.class);
        intent.putExtra(Constants.EXTRA_STORAGE_ID, storage.getId());
        return intent;
    }
}
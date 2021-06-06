package com.gee12.mytetroid.views.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.views.fragments.settings.SettingsEncryptionFragment;
import com.gee12.mytetroid.views.fragments.settings.SettingsFragment;
import com.gee12.mytetroid.views.fragments.settings.SettingsOtherFragment;
import com.gee12.mytetroid.views.fragments.settings.SettingsStorageFragment;

import org.jetbrains.annotations.NotNull;

/**
 * Активность для управления настройками приложения.
 */
public class SettingsActivity extends AppCompatActivity {

    private AppCompatDelegate mDelegate;
    private LinearLayout mLayoutProgress;
    private TextView mTextViewProgress;
//    private SettingsFragment mSettingsFragment;
//    private SettingsEncryptionFragment mEncFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setVisibilityActionHome(true);
//        addPreferencesFromResource(R.xml.prefs);
//        this.mSettingsFragment = new SettingsFragment();
//        this.mEncFragment = new SettingsEncryptionFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_settings, new SettingsFragment())//mSettingsFragment)
                .commit();

        this.mLayoutProgress = findViewById(R.id.layout_progress_bar);
        this.mTextViewProgress = findViewById(R.id.progress_text);
    }

    public void setProgressVisibility(boolean vis, String text) {
        if (vis) {
            mTextViewProgress.setText(text);
            mLayoutProgress.setVisibility(View.VISIBLE);
        } else {
            mLayoutProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        boolean permGranted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
//        mSettingsFragment.onRequestPermissionsResult(permGranted, requestCode);
        Fragment fragment = getCurFragment();
        if (fragment instanceof SettingsStorageFragment) {
            ((SettingsStorageFragment)fragment).onRequestPermissionsResult(permGranted, requestCode);
        } else if (fragment instanceof SettingsOtherFragment) {
            ((SettingsOtherFragment)fragment).onRequestPermissionsResult(permGranted, requestCode);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        mSettingsFragment.onResult(requestCode, resultCode, data);
        Fragment fragment = getCurFragment();
        if (fragment instanceof SettingsStorageFragment) {
            ((SettingsStorageFragment)fragment).onResult(requestCode, resultCode, data);
        } else if (fragment instanceof SettingsOtherFragment) {
            ((SettingsOtherFragment)fragment).onResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
//            finish();
//            return true;
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Этот метод и методы ниже для реализации ToolBar в PreferenceActivity
     * @param savedInstanceState
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Nullable
    public ActionBar getSupportActionBar() {
        return this.getDelegate().getSupportActionBar();
    }

    public AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    @Override
    public void onBackPressed() {
        boolean isBackPressed = false;
        Fragment fragment = getCurFragment();
        // обрабатываем нажатие Back во фрагменте SettingsEncryptionFragment отдельно
        if (fragment instanceof SettingsEncryptionFragment) {
            if (!((SettingsEncryptionFragment)fragment).onBackPressed()) {
                isBackPressed = true;
            }
        } else {
            isBackPressed = true;
        }
        if (isBackPressed) {
            if (!(fragment instanceof SettingsFragment)) {
                setTitle(R.string.title_settings);
            }
            super.onBackPressed();
        }
    }

    private Fragment getCurFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.layout_settings);
    }

    protected void setVisibilityActionHome(boolean isVis) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(isVis);
        }
    }
}
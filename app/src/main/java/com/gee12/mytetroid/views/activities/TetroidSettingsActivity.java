package com.gee12.mytetroid.views.activities;

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

/**
 * Активность для управления настройками.
 */
public abstract class TetroidSettingsActivity extends AppCompatActivity {

    private AppCompatDelegate mDelegate;
    private LinearLayout mLayoutProgress;
    private TextView mTextViewProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        setSupportActionBar(findViewById(R.id.toolbar));
        setVisibilityActionHome(true);

        this.mLayoutProgress = findViewById(R.id.layout_progress_bar);
        this.mTextViewProgress = findViewById(R.id.progress_text);

        startDefaultFragment();
    }

    /**
     *
     * @return
     */
    protected abstract int getLayoutResourceId();

    /**
     *
     * @return
     */
    protected abstract void startDefaultFragment();

    public void setProgressVisibility(boolean vis, String text) {
        if (vis) {
            mTextViewProgress.setText(text);
            mLayoutProgress.setVisibility(View.VISIBLE);
        } else {
            mLayoutProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected Fragment getCurFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.container);
    }

    protected void setVisibilityActionHome(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(isVisible);
        }
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

    public void setSubTitle(String subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
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
}
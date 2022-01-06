package com.gee12.mytetroid.views.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.gee12.mytetroid.R
import com.gee12.mytetroid.views.IViewEventListener
import com.gee12.mytetroid.views.TetroidMessage

/**
 * Активность для управления настройками.
 */
abstract class TetroidSettingsActivity : AppCompatActivity(), IViewEventListener {

    private var newDelegate: AppCompatDelegate? = null
    private lateinit var lProgress: LinearLayout
    private lateinit var tvProgress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.installViewFactory()
        delegate.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResourceId())
        setSupportActionBar(findViewById(R.id.toolbar))
        setVisibilityActionHome(true)

        lProgress = findViewById(R.id.layout_progress_bar)
        tvProgress = findViewById(R.id.progress_text)

        startDefaultFragment()

        App.current.logger?.logDebug(getString(R.string.log_activity_opened_mask, javaClass.simpleName))
    }

    protected abstract fun getLayoutResourceId(): Int

    protected abstract fun startDefaultFragment()

    protected fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.container)
    }

    //region IViewEventListener

    override fun setProgressVisibility(isVisible: Boolean, text: String?) {
        if (isVisible) {
            tvProgress.text = text
            lProgress.visibility = View.VISIBLE
        } else {
            lProgress.visibility = View.GONE
        }
    }

    override fun setProgressText(progressTextResId: Int) {
        tvProgress.setText(progressTextResId)
    }

    override fun setProgressText(progressText: String?) {
        tvProgress.text = progressText
    }

    //endregion IViewEventListener

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun setVisibilityActionHome(isVisible: Boolean) {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(isVisible)
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    override fun showSnackMoreInLogs() {
        TetroidMessage.showSnackMoreInLogs(this, R.id.layout_coordinator)
    }

    //region ToolBar in PreferenceActivity

    /**
     * Этот метод и методы ниже для реализации ToolBar в PreferenceActivity
     * @param savedInstanceState
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delegate.onPostCreate(savedInstanceState)
    }

    override fun setContentView(@LayoutRes layoutResID: Int) {
        delegate.setContentView(layoutResID)
    }

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    override fun onStop() {
        super.onStop()
        delegate.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        delegate.onDestroy()
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        delegate.setSupportActionBar(toolbar)
    }

    fun setSubTitle(subTitle: String?) {
        supportActionBar!!.subtitle = subTitle
    }

    override fun getSupportActionBar(): ActionBar? {
        return delegate.supportActionBar
    }

    override fun getDelegate(): AppCompatDelegate {
        if (newDelegate == null) {
            newDelegate = AppCompatDelegate.create(this, null)
        }
        return newDelegate!!
    }

    //endregion ToolBar in PreferenceActivity

}
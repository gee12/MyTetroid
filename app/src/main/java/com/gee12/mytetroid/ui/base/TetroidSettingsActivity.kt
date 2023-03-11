package com.gee12.mytetroid.ui.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.gee12.mytetroid.R

/**
 * Активность для управления настройками.
 */
abstract class TetroidSettingsActivity<VM : BaseViewModel> : TetroidActivity<VM>() {

    private var newDelegate: AppCompatDelegate? = null

    protected val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.container)

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.installViewFactory()
        delegate.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        startDefaultFragment()
    }

    protected abstract fun startDefaultFragment()

    // region File

    override fun isUseFileStorage() = true

    override fun onStorageAccessGranted(requestCode: Int, root: DocumentFile) {
        (currentFragment as? ITetroidFileStorage)?.onStorageAccessGranted(requestCode, root)
    }

    override fun onFolderSelected(requestCode: Int, folder: DocumentFile) {
        (currentFragment as? ITetroidFileStorage)?.onFolderSelected(requestCode, folder)
    }

    override fun onFileSelected(requestCode: Int, files: List<DocumentFile>) {
        (currentFragment as? ITetroidFileStorage)?.onFileSelected(requestCode, files)
    }

    // endregion File

    //region IAndroidComponent

    //endregion IAndroidComponent

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.optionsMenu = menu
        menuInflater.inflate(R.menu.storages, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
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
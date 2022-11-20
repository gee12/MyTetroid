package com.gee12.mytetroid.views.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.viewmodels.ViewEvent
import com.gee12.mytetroid.views.fragments.settings.storage.StorageSectionsSettingsFragment
import com.gee12.mytetroid.views.fragments.settings.storage.StorageMainSettingsFragment
import com.gee12.mytetroid.views.fragments.settings.storage.TetroidStorageSettingsFragment
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModel.StorageEvent
import com.gee12.mytetroid.views.TetroidMessage
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Активность для управления настройками хранилища.
 * (замена SettingsManager для параметров хранилища)
 */
class StorageSettingsActivity : TetroidSettingsActivity() {

    val viewModel: StorageSettingsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
    }

    fun initViewModel() {

        lifecycleScope.launch {
            viewModel.messageEventFlow.collect { message -> TetroidMessage.show(this@StorageSettingsActivity, message) }
        }
        lifecycleScope.launch {
            viewModel.viewEventFlow.collect { event -> onViewEvent(event) }
        }
        lifecycleScope.launch {
            viewModel.storageEventFlow.collect { event -> onStorageEvent(event) }
        }
        viewModel.updateStorageField.observe(this) { pair -> onUpdateStorageFieldEvent(pair.first, pair.second.toString()) }

    }

    protected fun onViewEvent(event: ViewEvent) {
//        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onViewEvent(event, data)

        when (event) {
            is ViewEvent.ShowProgress -> setProgressVisibility(event.isVisible)
            is ViewEvent.ShowProgressText -> setProgressText(event.message)
            is ViewEvent.TaskStarted -> {
                setProgressVisibility(true, event.titleResId?.let { getString(it) })
            }
            ViewEvent.TaskFinished -> setProgressVisibility(false)
            ViewEvent.ShowMoreInLogs -> showSnackMoreInLogs()
            ViewEvent.PermissionCheck -> viewModel.checkWriteExtStoragePermission(this)
            is ViewEvent.PermissionGranted -> viewModel.initStorage()
            else -> Unit
        }
    }

    protected fun onStorageEvent(event: StorageEvent) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageEvent(event)

        when (event) {
            is StorageEvent.GetEntity -> onStorageFoundInBase(event.storage)
            is StorageEvent.Inited -> onStorageInited(event.storage)
            is StorageEvent.InitFailed -> onStorageInitFailed()
            else -> Unit
        }
    }

    protected fun onStorageFoundInBase(storage: TetroidStorage) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageFoundInBase(storage)
    }

    protected fun onStorageInited(storage: TetroidStorage) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageInited(storage)

        val storageFilesError = viewModel.checkStorageFilesExistingError()
        setWarningMenuItem(
            isVisible = (storageFilesError != null)
        ) {
            viewModel.showMessage(
                storageFilesError ?: getString(R.string.mes_storage_init_error)
            )
        }
    }

    protected fun onStorageInitFailed() {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageInitFailed()

        setWarningMenuItem(
            isVisible = true
        ) {
            viewModel.showMessage(
                viewModel.checkStorageFilesExistingError() ?: getString(R.string.mes_storage_init_error)
            )
        }
    }

    protected fun onUpdateStorageFieldEvent(key: String, value: String) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onUpdateStorageFieldEvent(key, value)
    }

    protected fun setWarningMenuItem(isVisible: Boolean, onClick: (() -> Unit)? = null) {
        optionsMenu.findItem(R.id.action_error)?.let {
            it.isVisible = isVisible
            it.setOnMenuItemClickListener {
                onClick?.invoke()
                true
            }
        }
        updateOptionsMenu()
    }

    fun getStorageId(): Int {
        return if (intent?.hasExtra(Constants.EXTRA_STORAGE_ID) == true)
            intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
        else 0
    }

    override fun getLayoutResourceId() = R.layout.activity_settings

    override fun startDefaultFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, StorageSectionsSettingsFragment())
            .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val permGranted = grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED
        val fragment = getCurrentFragment()
        if (fragment is StorageMainSettingsFragment) {
            fragment.onRequestPermissionsResult(permGranted, requestCode)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val fragment = getCurrentFragment()
        if (fragment is StorageMainSettingsFragment) {
            fragment.onResult(requestCode, resultCode, data!!)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.storage_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onBackPressed() {
        val fragment = getCurrentFragment()
        if (fragment is TetroidStorageSettingsFragment) {
            if (!fragment.onBackPressed()) {
                if (fragment is StorageSectionsSettingsFragment) {
                    checkSettingsChanges()
                }
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun checkSettingsChanges() {
        // если настройки хранилища были изменены, добавляем пометку в результат активити
        if (viewModel.isFieldsChanged) {
            val intent = Intent().apply {
                if (viewModel.isStoragePathChanged) {
                    putExtra(Constants.EXTRA_IS_LOAD_STORAGE, true)
                    putExtra(Constants.EXTRA_STORAGE_ID, getStorageId())
                } else {
                    putExtra(Constants.EXTRA_IS_RELOAD_STORAGE_ENTITY, true)
                }
            }
            setResult(Activity.RESULT_OK, intent)
        }
    }

    companion object {

        @JvmStatic
        fun newIntent(context: Context, storage: TetroidStorage): Intent {
            return Intent(context, StorageSettingsActivity::class.java).apply {
                putExtra(Constants.EXTRA_STORAGE_ID, storage.id)
            }
        }
    }
}
package com.gee12.mytetroid.ui.settings.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.enums.TetroidPermission
import com.gee12.mytetroid.ui.base.TetroidSettingsActivity
import com.gee12.mytetroid.ui.storage.StorageEvent
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidStorageSettingsFragment
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope

/**
 * Активность для управления настройками хранилища.
 * (замена SettingsManager для параметров хранилища)
 */
class StorageSettingsActivity : TetroidSettingsActivity() {

    private lateinit var scopeSource: ScopeSource
    private val koinScope: Scope
        get() = scopeSource.scope

    lateinit var viewModel: StorageSettingsViewModel


    fun getStorageId(): Int {
        return intent.getIntExtra(Constants.EXTRA_STORAGE_ID, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createDependencyScope()
        createViewModel()
        initViewModel()
    }

    private fun createDependencyScope() {
        val currentStorageProvider = ScopeSource.current.scope.get<IStorageProvider>()
        val currentStorageId = currentStorageProvider.storage?.id
        // создавать новый koin scope или использовать существующий current.scope
        scopeSource = if (currentStorageId?.let { it == getStorageId() } == true) {
            ScopeSource.current
        } else {
            ScopeSource.createNew()
        }
    }

    private fun createViewModel() {
        viewModel = koinScope.get()
    }

    private fun initViewModel() {
        lifecycleScope.launch {
            viewModel.eventFlow.collect { event -> onBaseEvent(event) }
        }
        lifecycleScope.launch {
            viewModel.messageEventFlow.collect { message -> showMessage(message) }
        }

        viewModel.updateStorageField.observe(this) { pair -> onUpdateStorageFieldEvent(pair.first, pair.second.toString()) }

    }

    private fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is StorageEvent -> {
                onStorageEvent(event)
            }
            is BaseEvent.ShowProgress -> setProgressVisibility(event.isVisible)
            is BaseEvent.ShowProgressText -> showProgress(event.message)
            is BaseEvent.TaskStarted -> {
                setProgressVisibility(true, event.titleResId?.let { getString(it) })
            }
            BaseEvent.TaskFinished -> setProgressVisibility(false)
            BaseEvent.ShowMoreInLogs -> showSnackMoreInLogs()
            is BaseEvent.Permission.Check -> {
                if (event.permission == TetroidPermission.WriteStorage) {
                    viewModel.checkWriteExtStoragePermission(activity = this)
                }
            }
            is BaseEvent.Permission.Granted -> {
                if (event.permission == TetroidPermission.WriteStorage) {
                    viewModel.initStorage()
                }
            }
            else -> Unit
        }
    }

    private fun onStorageEvent(event: StorageEvent) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageEvent(event)

        when (event) {
            is StorageEvent.FoundInBase -> onStorageFoundInBase(event.storage)
            is StorageEvent.Inited -> onStorageInited(event.storage)
            is StorageEvent.InitFailed -> onStorageInitFailed()
            else -> Unit
        }
    }

    private fun onStorageFoundInBase(storage: TetroidStorage) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageFoundInBase(storage)
    }

    private fun onStorageInited(storage: TetroidStorage) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageInited(storage)

        val storageFilesError = viewModel.checkStorageFilesExistingError()
        setWarningMenuItem(
            isVisible = (storageFilesError != null),
            onClick = {
                viewModel.showMessage(
                    storageFilesError ?: getString(R.string.mes_storage_init_error)
                )
            }
        )
    }

    private fun onStorageInitFailed() {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onStorageInitFailed()

        setWarningMenuItem(
            isVisible = true,
            onClick = {
                viewModel.showMessage(
                    viewModel.checkStorageFilesExistingError() ?: getString(R.string.mes_storage_init_error)
                )
            }
        )
    }

    private fun onUpdateStorageFieldEvent(key: String, value: String) {
        (getCurrentFragment() as? TetroidStorageSettingsFragment)?.onUpdateStorageFieldEvent(key, value)
    }

    private fun setWarningMenuItem(isVisible: Boolean, onClick: (() -> Unit)? = null) {
        optionsMenu.findItem(R.id.action_error)?.let {
            it.isVisible = isVisible
            it.setOnMenuItemClickListener {
                onClick?.invoke()
                true
            }
        }
        updateOptionsMenu()
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
            val intent = buildIntent {
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
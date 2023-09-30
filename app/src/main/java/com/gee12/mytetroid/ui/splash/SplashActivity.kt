package com.gee12.mytetroid.ui.splash

import android.content.Intent
import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.main.MainActivity

/**
 * Главная активность приложения со списком веток, записей и меток.
 */
class SplashActivity : TetroidActivity<SplashViewModel>() {

    // region Create

    override fun getLayoutResourceId() = R.layout.activity_splash

    override fun getViewModelClazz() = SplashViewModel::class.java

    override fun isSingleTitle() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setVisibilityActionHome(false)

        viewModel.initApp()
    }

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    // endregion Create

    // region Events

    /**
     * Обработчик событий UI.
     */
    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            SplashEvent.AppInited -> {
                viewModel.checkAndStartMigration()
            }
            SplashEvent.Migration.NoNeeded -> {
                viewModel.checkPermissions()
            }
            SplashEvent.Migration.Finished -> {
                showMigrationDialog(
                    onCommit = {
                        viewModel.checkPermissions()
                    }
                )
            }
            SplashEvent.Migration.Failed -> {
                showSnackMoreInLogs()
            }
            SplashEvent.CheckPermissions.NoNeeded -> {
                startMainActivity()
            }
            SplashEvent.CheckPermissions.Granted -> {
                startMainActivity()
            }
            SplashEvent.CheckPermissions.Request -> {
                showFullFileStoragePermissionRequest()
            }
            else -> {
                super.onBaseEvent(event)
            }
        }
    }

    // endregion Events

    private fun showMigrationDialog(
        onCommit: () -> Unit,
    ) {
        AskDialogs.showOkDialog(
            context = this,
            titleResId = R.string.title_app_updated,
            messageResId = R.string.mes_migration_53,
            applyResId = R.string.answer_ok,
            isCancelable = false,
            onApply = onCommit,
        )
    }

    private fun showFullFileStoragePermissionRequest() {
        viewModel.permissionManager.requestWriteExtStoragePermissions(
            activity = this,
            requestCode = PermissionRequestCode.FULL_FILE_ACCESS,
            onManualPermissionRequest = { callback ->
                AskDialogs.showOkCancelDialog(
                    context = this,
                    title = getString(R.string.ask_permission_on_all_files_on_device_title),
                    message = getString(R.string.ask_permission_on_all_files_on_device_description),
                    isCancelable = false,
                    onYes = {
                        callback()
                    },
                    onCancel = {
                        finishAffinity()
                    }
                )
            },
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PermissionRequestCode.FULL_FILE_ACCESS.code) {
            // перепроверяем разрешения
            viewModel.checkPermissions()
        }
    }

    private fun startMainActivity() {
        finish()
        MainActivity.start(this, action = null, bundle = null)
    }

}
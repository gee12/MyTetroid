package com.gee12.mytetroid.ui.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.main.MainActivity
import com.gee12.mytetroid.ui.record.RecordActivity

/**
 * Главная активность приложения со списком веток, записей и меток.
 */
class SplashActivity : TetroidActivity<SplashViewModel>() {

    private var startRecordActivity = false

    // region Create

    override fun getLayoutResourceId() = R.layout.activity_splash

    override fun getViewModelClazz() = SplashViewModel::class.java

    override fun isSingleTitle() = true

    override fun isAppearInLogs() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleExtras()

        setVisibilityActionHome(false)

        viewModel.initApp()
    }

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    private fun handleExtras() {
        startRecordActivity = intent.getBooleanExtra(Constants.EXTRA_START_RECORD_ACTIVITY, false)
    }

    // endregion Create

    // region Events

    /**
     * Обработчик событий UI.
     */
    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            SplashEvent.AppInitialized -> {
                App.isInitialized = true
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
                startNextActivity()
            }
            SplashEvent.CheckPermissions.Granted -> {
                startNextActivity()
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

    private fun startNextActivity() {
        if (startRecordActivity) {
            RecordActivity.start(this)
        } else {
            MainActivity.start(this)
        }
        finish()
    }

    companion object {

        fun start(activity: Activity, startRecordActivity: Boolean = false) {
            val intent = Intent(activity, SplashActivity::class.java)
            intent.putExtra(Constants.EXTRA_START_RECORD_ACTIVITY, startRecordActivity)
            activity.startActivity(intent)
        }
    }

}
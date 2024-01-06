package com.gee12.mytetroid.ui.splash

import android.app.Application
import android.os.Build
import android.os.Environment
import com.gee12.mytetroid.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.*
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.InitAppUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.ui.base.BaseViewModel

class SplashViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,

    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,

    private val migrationInteractor: MigrationInteractor,

    private val initAppUseCase: InitAppUseCase,
): BaseViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,

    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
) {

    fun initApp() {
        launchOnMain {
            showProgressWithText(R.string.state_app_initializing)
            withIo {
                initAppUseCase.run(InitAppUseCase.Params)
            }.onComplete {
                hideProgress()
            } .onFailure {
                logFailure(it)
            }.onSuccess {
                sendEvent(SplashEvent.AppInitialized)
            }
        }
    }

    // region Migration

    /**
     * Проверка необходимости миграции и ее запуск.
     * Возвращает true, если миграция была запущена.
     */
    fun checkAndStartMigration() {
        launchOnIo {
            val fromVersion = CommonSettings.getSettingsVersion(getContext())
            var result: Boolean? = null

            if (fromVersion == 0 && settingsManager.getStoragePath().isEmpty()) {
                // новая установка, миграция не нужна
                CommonSettings.setSettingsCurrentVersion(getContext())
                withMain {
                    sendEvent(SplashEvent.Migration.NoNeeded)
                }
            } else {

                // 53
                if (fromVersion < Constants.SETTINGS_VERSION_CURRENT) {
                    result = migrateFrom46To53()
                }
                // ..

                when (result) {
                    true -> {
                        CommonSettings.setSettingsCurrentVersion(getContext())
                        withMain {
                            sendEvent(SplashEvent.Migration.Finished)
                        }
                    }
                    false -> {
                        logger.logError(R.string.log_error_migration, show = true)
                        withMain {
                            sendEvent(SplashEvent.Migration.Failed)
                        }
                    }
                    else -> {
                        withMain {
                            sendEvent(SplashEvent.Migration.NoNeeded)
                        }
                    }
                }
            }
        }
    }

    private suspend fun migrateFrom46To53(): Boolean {
        logger.log(getString(R.string.log_start_migrate_to_version_mask, "5.3"), false)

        // параметры хранилища из SharedPreferences в бд
        if (migrationInteractor.isNeedMigrateStorageFromPrefs()) {
            if (migrationInteractor.addDefaultStorageFromPrefs()) {
                logger.log(R.string.log_migration_finished_successfully)
            } else {
                return false
            }
        }

        return true
    }

    // endregion Migration

    // region Permissions

    fun checkPermissions() {
        val hasAllFilesAccess = buildInfoProvider.hasAllFilesAccessVersion()
        if (hasAllFilesAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                launchOnMain {
                    sendEvent(SplashEvent.CheckPermissions.Granted)
                }
            } else {
                launchOnMain {
                    sendEvent(SplashEvent.CheckPermissions.Request)
                }
            }
        } else {
            launchOnMain {
                sendEvent(SplashEvent.CheckPermissions.NoNeeded)
            }
        }
    }

    // endregion Permissions

}
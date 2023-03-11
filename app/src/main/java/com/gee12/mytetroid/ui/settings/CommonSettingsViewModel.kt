package com.gee12.mytetroid.ui.settings

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.usecase.storage.ClearAllStoragesTrashFolderUseCase
import com.gee12.mytetroid.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class CommonSettingsViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    val buildInfoProvider: BuildInfoProvider,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    private val clearAllStoragesTrashFolderUseCase: ClearAllStoragesTrashFolderUseCase,
) : BaseViewModel(
    application = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    val crypter = Crypter(this.logger)


    //region Pin

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    fun isRequestPINCode(): Boolean {
        return buildInfoProvider.isFullVersion()
                && CommonSettings.isRequestPINCode(getContext())
                && CommonSettings.getPINCodeHash(getContext()) != null
    }

    fun setupPinCodeLength(length: Int) {
        CommonSettings.setPINCodeLength(getContext(), length)
        logger.log(getString(R.string.log_pin_code_length_setup) + length, false)
    }

    fun setupPinCode(pin: String) {
        // сохраняем хеш
        val crypter = Crypter(this.logger)
        val pinHash: String = crypter.passToHash(pin)
        CommonSettings.setPINCodeHash(getContext(), pinHash)
        logger.log(R.string.log_pin_code_setup, true)
    }

    fun checkAndDropPinCode(pin: String): Boolean {
        return checkPinCode(pin).also {
            if (it) dropPinCode()
        }
    }

    fun checkPinCode(pin: String): Boolean {
        // сравниваем хеши
        val pinHash = crypter.passToHash(pin)
        return (pinHash == CommonSettings.getPINCodeHash(getContext()))
    }

    protected fun dropPinCode() {
        CommonSettings.setPINCodeHash(getContext(), null)
        logger.log(R.string.log_pin_code_dropped, true)
    }

    fun clearTrashFolders() {
        launchOnMain {
            withIo {
                clearAllStoragesTrashFolderUseCase.run(
                    ClearAllStoragesTrashFolderUseCase.Params
                )
            }.onFailure {
                logFailure(it)
            }.onSuccess {
                log(R.string.title_trash_cleared, true)
            }
        }
    }

    //endregion Pin

}
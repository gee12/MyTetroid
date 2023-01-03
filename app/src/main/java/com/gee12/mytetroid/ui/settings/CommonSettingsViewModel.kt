package com.gee12.mytetroid.ui.settings

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.interactor.TrashInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.repo.StoragesRepo
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
) : BaseViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    settingsManager,
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    val trashInteractor = TrashInteractor(this.logger, StoragesRepo(app))
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
            when (trashInteractor.clearTrashFoldersIfNeeded(/*false*/)) {
                TrashInteractor.TrashClearResult.SUCCESS -> {
                    log(R.string.title_trash_cleared, true)
                }
                TrashInteractor.TrashClearResult.FAILURE -> {
                    logError(R.string.title_trash_clear_error, true)
                }
                else -> {}
            }
        }
    }

    //endregion Pin

}
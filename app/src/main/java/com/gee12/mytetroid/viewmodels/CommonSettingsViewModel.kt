package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.App
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.interactors.TrashInteractor
import com.gee12.mytetroid.repo.StoragesRepo
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class CommonSettingsViewModel(
    app: Application,
) : BaseViewModel(app), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    val trashInteractor = TrashInteractor(this.logger, StoragesRepo(app))
    val crypter = Crypter(this.logger)


    //region Pin

    /**
     * Проверка использования ПИН-кода с учетом версии приложения.
     * @return
     */
    fun isRequestPINCode(): Boolean {
        return App.isFullVersion()
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
        launch {
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
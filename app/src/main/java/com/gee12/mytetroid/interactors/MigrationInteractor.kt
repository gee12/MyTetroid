package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.CommonSettingsRepo
import com.gee12.mytetroid.utils.Utils
import java.nio.charset.Charset

class MigrationInteractor(
    private val logger: ITetroidLogger,
    private val commonSettingsRepo: CommonSettingsRepo,
    private val storagesInteractor: StoragesInteractor
) {

    /**
     * TODO: проверить на старой версии, можно ли иметь сохраненный в SP хеш пароля,
     *  но не иметь загружаемого (пути) хранилища
     */
    fun isNeedMigratePinCode(context: Context): Boolean {
        return BuildConfig.VERSION_CODE >= Constants.VERSION_50
                && CommonSettings.isPinCodeMigratedFromRC5ToMD5(context)
                && CommonSettings.isSaveMiddlePassHashLocalDef(context)
                && CommonSettings.getMiddlePassHash(context) != null
    }

    /**
     * Изменение способа шифрования ПИН-кода.
     * Начиная с версии 5.0, ПИН-код шифруется с помощью MD5, а не RC5, и не зависит от пароля на хранилище.
     */
    fun migratePinCodeFromRC5ToMD5(context: Context): Boolean {
        logger.log(R.string.log_start_pin_crypt_migration, false)
        return try {
            val rc5PinCodeHash = CommonSettings.getPINCodeHash(context) ?: return true
            val crypter = createRC5Crypter(rc5PinCodeHash)

            val pinCode = crypter.decryptText(rc5PinCodeHash.toByteArray(Charset.forName("UTF-8")))

            val md5PinCodeHash = Utils.toMD5Hex(pinCode)
            CommonSettings.setPINCodeHash(context, md5PinCodeHash)

            logger.log(R.string.log_pin_crypt_was_migrated, false)
            true
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.log_error_pin_crypt_migrate), ex, false)
            false
        }
    }

    private fun createRC5Crypter(rc5PinCodeHash: String): Crypter {
        val crypter = Crypter(logger)
        val key = crypter.middlePassHashToKey(rc5PinCodeHash)
        crypter.setCryptKey(key)
        return crypter
    }

    suspend fun isNeedMigrateStorageFromPrefs(context: Context): Boolean {
        return BuildConfig.VERSION_CODE >= Constants.VERSION_50
            && storagesInteractor.getStoragesCount() == 0
            && CommonSettings.getStoragePath(context)?.isNotEmpty() == true
    }

    /**
     * Миграция с версии < 5.0, когда не было многобазовости.
     */
    suspend fun addDefaultStorageFromPrefs(context: Context): Boolean {
        return storagesInteractor.addStorage(
            storagesInteractor.initStorage(
                context,
                TetroidStorage(
                    path = CommonSettings.getStoragePath(context)
                )
            ).apply {
                isDefault = true
                middlePassHash = CommonSettings.getMiddlePassHash(context)
                quickNodeId = CommonSettings.getQuicklyNodeId(context)
                lastNodeId = CommonSettings.getLastNodeId(context)
                // TODO: создать миграцию Избранного
//                favorites = CommonSettings.getFavorites(context)
            })
    }

}
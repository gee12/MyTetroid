package com.gee12.mytetroid.interactors

import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.providers.ISensitiveDataProvider
import com.gee12.mytetroid.providers.IStorageProvider

/**
 * Создается для конкретного хранилища.
 */
class PasswordInteractor(
    private val storageProvider: IStorageProvider,
    private val storageCrypter: IStorageCrypter,
    private val sensitiveDataProvider: ISensitiveDataProvider,
) {

    private val databaseConfig: DatabaseConfig
        get() = storageProvider.databaseConfig

    /**
     * Проверка введенного пароля с сохраненным проверочным хэшем.
     * @throws DatabaseConfig.EmptyFieldException
     */
    @Throws(DatabaseConfig.EmptyFieldException::class)
    fun checkPass(pass: String?): Boolean {
        val salt = databaseConfig.cryptCheckSalt
        val checkHash = databaseConfig.cryptCheckHash
        return storageCrypter.checkPass(pass, salt, checkHash)
    }

    /**
     * Проверка сохраненного хэша пароля с помощью сохраненных зашифрованных данных.
     * @throws DatabaseConfig.EmptyFieldException
     */
    @Throws(DatabaseConfig.EmptyFieldException::class)
    fun checkMiddlePassHash(passHash: String?): Boolean {
        val checkData = databaseConfig.middleHashCheckData
        return storageCrypter.checkMiddlePassHash(passHash, checkData)
    }

    /**
     * Сброс сохраненного хэша пароля и его проверочных данных.
     */
    fun clearSavedPass(storage: TetroidStorage) {
        sensitiveDataProvider.resetMiddlePassHash()
        clearPassCheckData(storage)
        clearMiddlePassCheckData()
    }

    /**
     * Очистка сохраненного проверочнго хэша пароля.
     */
    fun clearPassCheckData(storage: TetroidStorage): Boolean {
        storage.middlePassHash = null
        return databaseConfig.savePass(null, null, false)
    }

    /**
     * Очистка сохраненной проверочной строки промежуточного хэша пароля.
     */
    fun clearMiddlePassCheckData(): Boolean {
        return databaseConfig.saveCheckData(null)
    }

}
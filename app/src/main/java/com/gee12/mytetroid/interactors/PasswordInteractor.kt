package com.gee12.mytetroid.interactors

import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.crypt.Base64
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.helpers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

/**
 * Создается для конкретного хранилища.
 */
class PasswordInteractor(
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val cryptInteractor: EncryptionInteractor,
    private val nodesInteractor: NodesInteractor,
    private val sensitiveDataProvider: ISensitiveDataProvider,
) {

    private val databaseConfig: DatabaseConfig
        get() = storageProvider.databaseConfig

    fun isCrypted() = nodesInteractor.isExistCryptedNodes(false)

    /**
     * Проверка введенного пароля с сохраненным проверочным хэшем.
     * @param pass
     * @return
     * @throws DatabaseConfig.EmptyFieldException
     */
    @Throws(DatabaseConfig.EmptyFieldException::class)
    fun checkPass(pass: String?): Boolean {
        val salt = databaseConfig.cryptCheckSalt
        val checkHash = databaseConfig.cryptCheckHash
        return cryptInteractor.crypter.checkPass(pass, salt, checkHash)
    }

    /**
     * Проверка сохраненного хэша пароля с помощью сохраненных зашифрованных данных.
     * @param passHash
     * @return
     * @throws DatabaseConfig.EmptyFieldException
     */
    @Throws(DatabaseConfig.EmptyFieldException::class)
    fun checkMiddlePassHash(passHash: String?): Boolean {
        val checkData = databaseConfig.middleHashCheckData
        return cryptInteractor.crypter.checkMiddlePassHash(passHash, checkData)
    }

    /**
     * Сохранение пароля в настройках и его установка для шифрования.
     * @param pass
     */
    suspend fun initPass(storage: TetroidStorage, pass: String) {
        val passHash = cryptInteractor.crypter.passToHash(pass)
        if (storage.isSavePassLocal) {
            // сохраняем хэш пароля
            storage.middlePassHash = passHash
            // записываем проверочную строку
            saveMiddlePassCheckData(passHash)
        } else {
            // сохраняем хэш пароля в оперативную память, на вермя "сеанса" работы приложения
            sensitiveDataProvider.saveMiddlePassHash(passHash)
        }
        // здесь, по идее, можно сохранять сразу passHash (с параметром isMiddleHash=true),
        // но сделал так
        cryptInteractor.initCryptPass(pass, false)
    }

    /**
     * Установка пароля хранилища впервые.
     * @param pass
     */
    suspend fun setupPass(storage: TetroidStorage, pass: String): Boolean {
        // сохраняем в database.ini
        return if (savePassCheckData(pass)) {
            logger.log(R.string.log_pass_setted, true)
            initPass(storage, pass)
            true
        } else {
            logger.log(R.string.log_pass_set_error, true)
            false
        }
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
     * Сохранение проверочного хэша пароля и сопутствующих данных в database.ini.
     * @param newPass
     * @return
     */
    fun savePassCheckData(newPass: String?): Boolean {
        val salt = Utils.createRandomBytes(32)
        val passHash = try {
            Crypter.calculatePBKDF2Hash(newPass, salt)
        } catch (ex: Exception) {
            logger.logError(ex)
            return false
        }
        return databaseConfig.savePass(
            Base64.encodeToString(passHash, false),
            Base64.encodeToString(salt, false), true
        )
    }

    /**
     * Сохранение проверочной строки промежуточного хэша пароля в database.ini.
     * @param passHash
     * @return
     */
    suspend fun saveMiddlePassCheckData(passHash: String?): Boolean {
        val checkData = cryptInteractor.crypter.createMiddlePassHashCheckData(passHash)
        return withContext(Dispatchers.IO) { databaseConfig.saveCheckData(checkData) }
    }

    /**
     * Очистка сохраненного проверочнго хэша пароля.
     * @return
     */
    fun clearPassCheckData(storage: TetroidStorage): Boolean {
        storage.middlePassHash = null
        return databaseConfig.savePass(null, null, false)
    }

    /**
     * Очистка сохраненной проверочной строки промежуточного хэша пароля.
     * @return
     */
    fun clearMiddlePassCheckData(): Boolean {
        return databaseConfig.saveCheckData(null)
    }

}
package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.crypt.Base64
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.ini.DatabaseConfig
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class PasswordInteractor(
    val logger: ITetroidLogger,
    val storage: TetroidStorage?,
    val databaseConfig: DatabaseConfig,
    val cryptInteractor: EncryptionInteractor,
    val storageInteractor: StorageInteractor,
    val nodesInteractor: NodesInteractor
) {

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
    suspend fun initPass(context: Context, pass: String) {
        val passHash = cryptInteractor.crypter.passToHash(pass)
//        if (SettingsManager.isSaveMiddlePassHashLocal(context)) {
        if (storage?.isSavePassLocal == true) {
            // сохраняем хэш пароля
//            SettingsManager.setMiddlePassHash(context, passHash)
            storage.middlePassHash = passHash
            // записываем проверочную строку
            saveMiddlePassCheckData(passHash)
        } else {
            // сохраняем хэш пароля в оперативную память, может еще понадобится
            cryptInteractor.crypter.middlePassHash = passHash
        }
        // здесь, по идее, можно сохранять сразу passHash (с параметром isMiddleHash=true),
        // но сделал так
        cryptInteractor.initCryptPass(pass, false)
    }

    /**
     * Установка пароля хранилища впервые.
     * @param pass
     */
    suspend fun setupPass(context: Context, pass: String) {
        // сохраняем в database.ini
        if (savePassCheckData(context, pass)) {
            logger.log(R.string.log_pass_setted, true)
            initPass(context, pass)
        } else {
            logger.log(R.string.log_pass_set_error, true)
        }
    }

//    suspend fun changePass(context: Context, curPass: String?, newPass: String?, taskProgress: ITaskProgress): Boolean {
//        // сначала устанавливаем текущий пароль
//        taskProgress.nextStage(TetroidLog.Objs.CUR_PASS, TetroidLog.Opers.SET, TaskStage.Stages.START)
//        initPass(context, curPass)
//        // и расшифровываем хранилище
//        if (!taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.DECRYPT) {
//                cryptInteractor.decryptStorage(context, true)
//        }) return false
//        // теперь устанавливаем новый пароль
//        taskProgress.nextStage(TetroidLog.Objs.NEW_PASS, TetroidLog.Opers.SET, TaskStage.Stages.START)
//        initPass(context, newPass)
//        // и перешифровываем зашифрованные ветки
//        if (!taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.REENCRYPT) {
//                cryptInteractor.reencryptStorage(context)
//        }) return false
//        // сохраняем mytetra.xml
//        taskProgress.nextStage(TetroidLog.Objs.STORAGE, TetroidLog.Opers.SAVE) {
//            storageInteractor.saveStorage(context)
//        }
//        // сохраняем данные в database.ini
//        taskProgress.nextStage(TetroidLog.Objs.NEW_PASS, TetroidLog.Opers.SAVE, TaskStage.Stages.START)
//        savePassCheckData(context, newPass)
//        return true
//    }

    /**
     * Сброс сохраненного хэша пароля и его проверочных данных.
     */
    fun clearSavedPass() {
//        SettingsManager.setMiddlePassHash(context, null)
//        storage.middlePassHash = null
        cryptInteractor.crypter.middlePassHash = null
        clearPassCheckData()
        clearMiddlePassCheckData()
    }

    /**
     * Сохранение проверочного хэша пароля и сопутствующих данных в database.ini.
     * @param newPass
     * @return
     */
    fun savePassCheckData(context: Context?, newPass: String?): Boolean {
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
    fun clearPassCheckData(): Boolean {
//        SettingsManager.setMiddlePassHash(context, null)
        storage?.middlePassHash = null
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
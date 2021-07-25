package com.gee12.mytetroid.interactors

import android.content.Context
import android.widget.Toast
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.crypt.IRecordFileCrypter
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.viewmodels.IStorageCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class EncryptionInteractor(
    val xmlHelper: TetroidXml,
    val logger: ILogger,
    val callback: IStorageCallback
) : IRecordFileCrypter {

    var crypter = TetroidCrypter(logger, xmlHelper.loadHelper, this)
        protected set

    /**
     * Инициализация ключа шифрования с помощью пароля или его хэша.
     * @param pass
     * @param isMiddleHash
     */
    fun initCryptPass(pass: String, isMiddleHash: Boolean) {
        if (isMiddleHash) {
            crypter.initFromMiddleHash(pass)
        } else {
            crypter.initFromPass(pass)
        }
    }

    /**
     * Перешифровка хранилища (перед этим ветки должны быть расшифрованы).
     * @return
     */
    suspend fun reencryptStorage(context: Context): Boolean {
//        LogManager.log(R.string.log_start_storage_reencrypt);
        return crypter.encryptNodes(context, xmlHelper.mRootNodesList, true)
    }

    /**
     * Расшифровка хранилища (временная).
     * @return
     */
    suspend fun decryptStorage(context: Context, decryptFiles: Boolean): Boolean = withContext(Dispatchers.IO) {
//        LogManager.log(R.string.log_start_storage_decrypt);
        crypter.decryptNodes(context, xmlHelper.mRootNodesList, true, true,
            xmlHelper.loadHelper, false, decryptFiles
        )
    }

    /**
     * Расшифровка ветки с подветками (постоянная).
     * @param node
     * @return
     */
    suspend fun dropCryptNode(context: Context, node: TetroidNode): Boolean {
//        TetroidLog.logOperStart(TetroidLog.Objs.NODE, TetroidLog.Opers.DROPCRYPT, node);
        val res: Boolean = crypter.decryptNode(context, node, true, true, xmlHelper.loadHelper, true, false)
        return if (res) {
            callback.saveStorage(context)
        } else false
    }

    fun decryptField(obj: TetroidObject?, field: String?): String? {
        return if (obj != null && obj.isCrypted) crypter.decryptBase64(field) else field
    }

    fun decryptField(isCrypted: Boolean, field: String?): String? {
        return if (isCrypted) crypter.decryptBase64(field) else field
    }

    fun encryptField(obj: TetroidObject?, field: String?): String? {
        return encryptField(obj != null && obj.isCrypted && obj.isDecrypted, field) // последняя проверка не обязательна
    }

    fun encryptField(isCrypted: Boolean, field: String?): String? {
        return if (isCrypted) crypter.encryptTextBase64(field) else field
    }

    /**
     * Зашифровка (незашифрованной) ветки с подветками.
     * @param node
     * @return
     */
    suspend fun encryptNode(context: Context, node: TetroidNode): Boolean {
//        TetroidLog.logOperStart(TetroidLog.Objs.NODE, TetroidLog.Opers.ENCRYPT, node);
        val res = crypter.encryptNode(context, node, false)
        return if (res) {
            callback.saveStorage(context)
        } else false
    }

    /**
     * Зашифровка или расшифровка файла при необходимости.
     * @param file
     * @param isCrypted
     * @param isEncrypt
     * @return
     */
    suspend private fun cryptOrDecryptFile(context: Context, file: File, isCrypted: Boolean, isEncrypt: Boolean): Int {
        if (isCrypted && !isEncrypt) {
            return try {
                // расшифровуем файл записи
                if (encryptDecryptFile(file, file, false)) 1 else -1
            } catch (ex: Exception) {
                LogManager.log(context, context.getString(R.string.log_error_file_decrypt) + file.absolutePath, ex)
                -1
            }
        } else if (!isCrypted && isEncrypt) {
            return try {
                // зашифровуем файл записи
                if (encryptDecryptFile(file, file, true)) 1 else -1
            } catch (ex: Exception) {
                LogManager.log(context, context.getString(R.string.log_error_file_encrypt) + file.absolutePath, ex)
                -1
            }
        }
        return 0
    }

    /**
     * Зашифровка или расшифровка файла записи и прикрепленных файлов при необходимости.
     * @param record
     * @param isEncrypt
     */
    override suspend fun cryptRecordFiles(context: Context, record: TetroidRecord, isCrypted: Boolean, isEncrypt: Boolean): Boolean {
        // файл записи
        val recordFolderPath = callback.getPathToRecordFolder(record)
        var file = File(recordFolderPath, record.fileName)
        if (cryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
            return false
        }
        // прикрепленные файлы
        if (record.attachedFilesCount > 0) {
            for (attach in record.attachedFiles) {
                file = File(recordFolderPath, attach.idName)
                if (!file.exists()) {
                    LogManager.log(context, context.getString(R.string.log_file_is_missing) + TetroidLog.getIdString(context, attach),
                        ILogger.Types.WARNING, Toast.LENGTH_LONG)
                    continue
                }
                if (cryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
                    return false
                }
            }
        }
        return true
    }

    suspend fun encryptDecryptFile(srcFile: File, destFile: File, isEncrypt: Boolean): Boolean {
        return withContext(Dispatchers.IO) { crypter.encryptDecryptFile(srcFile, destFile, isEncrypt) }
    }
}
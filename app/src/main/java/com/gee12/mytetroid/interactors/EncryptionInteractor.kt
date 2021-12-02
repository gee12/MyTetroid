package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.data.xml.IStorageLoadHelper
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.TetroidRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Создается для конкретного хранилища.
 */
class EncryptionInteractor(
    private val logger: ITetroidLogger,
    val crypter: TetroidCrypter,
    private val xmlHelper: TetroidXml,
    private val storageHelper: IStorageLoadHelper // чтобы прокинуть в decryptNode и decryptNodes
) {

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
            storageHelper, false, decryptFiles
        )
    }

    /**
     * Расшифровка ветки с подветками (постоянная).
     * @param node
     * @return
     */
    suspend fun dropCryptNode(context: Context, node: TetroidNode): Boolean {
//        TetroidLog.logOperStart(TetroidLog.Objs.NODE, TetroidLog.Opers.DROPCRYPT, node);
        return crypter.decryptNode(context, node, true, true, storageHelper, true, false)
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
        return crypter.encryptNode(context, node, false)
    }

    /**
     * Зашифровка или расшифровка файла при необходимости.
     * @param file
     * @param isCrypted
     * @param isEncrypt
     * @return
     */
    suspend fun encryptOrDecryptFile(context: Context, file: File, isCrypted: Boolean, isEncrypt: Boolean): Int {
        if (isCrypted && !isEncrypt) {
            return try {
                // расшифровуем файл записи
                if (encryptDecryptFile(file, file, false)) 1 else -1
            } catch (ex: Exception) {
                logger.logError(context.getString(R.string.log_error_file_decrypt) + file.absolutePath, ex)
                -1
            }
        } else if (!isCrypted && isEncrypt) {
            return try {
                // зашифровуем файл записи
                if (encryptDecryptFile(file, file, true)) 1 else -1
            } catch (ex: Exception) {
                logger.logError(context.getString(R.string.log_error_file_encrypt) + file.absolutePath, ex)
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
    suspend fun cryptRecordFiles(context: Context, record: TetroidRecord, isCrypted: Boolean, isEncrypt: Boolean): Boolean {
//        // файл записи
//        val recordFolderPath = callback.getPathToRecordFolder(record)
//        var file = File(recordFolderPath, record.fileName)
//        if (encryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
//            return false
//        }
//        // прикрепленные файлы
//        if (record.attachedFilesCount > 0) {
//            for (attach in record.attachedFiles) {
//                file = File(recordFolderPath, attach.idName)
//                if (!file.exists()) {
//                    logger.logWarning(context.getString(R.string.log_file_is_missing) + StringUtils.getIdString(context, attach))
//                    continue
//                }
//                if (encryptOrDecryptFile(context, file, isCrypted, isEncrypt) < 0) {
//                    return false
//                }
//            }
//        }
//        return true
        return crypter.recordFileCrypter.cryptRecordFiles(context, record, isCrypted, isEncrypt)
    }

    suspend fun encryptDecryptFile(srcFile: File, destFile: File, isEncrypt: Boolean): Boolean {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) { crypter.encryptDecryptFile(srcFile, destFile, isEncrypt) }
    }
}
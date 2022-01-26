package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.crypt.ITetroidCrypter
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.INodeIconLoader
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.TetroidRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Создается для конкретного хранилища.
 */
class EncryptionInteractor(
    private val logger: ITetroidLogger,
    val crypter: ITetroidCrypter,
    private val storageDataProcessor: IStorageDataProcessor,
    private val nodeIconLoader: INodeIconLoader
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
        return crypter.encryptNodes(
            context = context,
            nodes = storageDataProcessor.getRootNodes(),
            isReencrypt = true
        )
    }

    /**
     * Расшифровка хранилища (временная).
     * @return
     */
    suspend fun decryptStorage(context: Context, decryptFiles: Boolean): Boolean = withContext(Dispatchers.IO) {
        crypter.decryptNodes(
            context = context,
            nodes = storageDataProcessor.getRootNodes(),
            isDecryptSubNodes = true,
            decryptRecords = true,
            iconLoader = nodeIconLoader,
            dropCrypt = false,
            decryptFiles = decryptFiles
        )
    }

    /**
     * Расшифровка ветки с подветками (постоянная).
     * @param node
     * @return
     */
    suspend fun dropCryptNode(context: Context, node: TetroidNode): Boolean {
        return crypter.decryptNode(
            context = context,
            node = node,
            decryptSubNodes = true,
            decryptRecords = true,
            iconLoader = nodeIconLoader,
            dropCrypt = true,
            decryptFiles = false
        )
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
        return crypter.recordFileCrypter.cryptRecordFiles(context, record, isCrypted, isEncrypt)
    }

    suspend fun encryptDecryptFile(srcFile: File, destFile: File, isEncrypt: Boolean): Boolean {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) { crypter.encryptDecryptFile(srcFile, destFile, isEncrypt) }
    }
}
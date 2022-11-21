package com.gee12.mytetroid.interactors

import android.content.Context
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.usecase.crypt.CryptRecordFilesUseCase
import com.gee12.mytetroid.usecase.LoadNodeIconUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Создается для конкретного хранилища.
 */
class EncryptionInteractor(
    private val logger: ITetroidLogger,
    val crypter: IEncryptHelper,
    private val storageDataProcessor: IStorageDataProcessor,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
    private val cryptRecordFilesUseCase: CryptRecordFilesUseCase,
) {

    /**
     * Инициализация ключа шифрования с помощью пароля или его хэша.
     */
    fun initCryptPass(pass: String, isMiddleHash: Boolean) {
        // FIXME: koin: исправить циклическую зависимость
        crypter.storageDataProcessor = storageDataProcessor

        if (isMiddleHash) {
            crypter.initFromMiddleHash(pass)
        } else {
            crypter.initFromPass(pass)
        }
    }

    /**
     * Перешифровка хранилища (перед этим ветки должны быть расшифрованы).
     */
    suspend fun reencryptStorage(): Boolean {
        return crypter.encryptNodes(
            nodes = storageDataProcessor.getRootNodes(),
            isReencrypt = true
        )
    }

    /**
     * Расшифровка ветки с подветками (постоянная).
     */
    suspend fun dropCryptNode(node: TetroidNode): Boolean {
        return crypter.decryptNode(
            node = node,
            isDecryptSubNodes = true,
            isDecryptRecords = true,
            loadIconCallback = {
                loadNodeIconUseCase.execute(
                    LoadNodeIconUseCase.Params(node)
                ).onFailure {
                    logger.logFailure(it, show = false)
                }
            },
            isDropCrypt = true,
            isDecryptFiles = false
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
     */
    suspend fun encryptNode(node: TetroidNode): Boolean {
        //TetroidLog.logOperStart(TetroidLog.Objs.NODE, TetroidLog.Opers.ENCRYPT, node);
        return crypter.encryptNode(node, false)
    }

    /**
     * Зашифровка или расшифровка файла записи и прикрепленных файлов при необходимости.
     */
    suspend fun cryptRecordFiles(record: TetroidRecord, isCrypted: Boolean, isEncrypt: Boolean): Boolean {
        return cryptRecordFilesUseCase.run(
            CryptRecordFilesUseCase.Params(
                record = record,
                isCrypted = isCrypted,
                isEncrypt = isEncrypt
            )
        ).foldResult(
            onLeft = {
                logger.logFailure(it, show = false)
                false
            },
            onRight = { it }
        )
    }

    suspend fun encryptDecryptFile(srcFile: File, destFile: File, isEncrypt: Boolean): Boolean {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) { crypter.encryptDecryptFile(srcFile, destFile, isEncrypt) }
    }

    fun decryptText(bytes: ByteArray): String {
        return crypter.decryptText(bytes)
    }

    fun encryptTextBytes(text: String): ByteArray {
        return crypter.encryptTextBytes(text)
    }

}
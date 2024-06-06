package com.gee12.mytetroid.domain.manager

import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.domain.usecase.crypt.CryptRecordFilesIfNeedUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.model.obj.TetroidNode
import com.gee12.mytetroid.model.obj.TetroidRecord
import com.gee12.mytetroid.model.obj.TetroidFile
import com.gee12.mytetroid.logs.ITetroidLogger
import java.io.InputStream
import java.io.OutputStream

interface IStorageCryptManager {

    fun init(
        cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase,
        parseRecordTagsUseCase: ParseRecordTagsUseCase,
    )

    fun reset()

    fun setKeyFromPassword(pass: String)
    fun setKeyFromMiddleHash(passHash: String)

    /**
     * Зашифровка веток.
     * @param isReencrypt Если true, то повторное шифрование зашифрованного объекта (должно быть расшифрованно перед этим)
     */
    suspend fun encryptNodes(nodes: List<TetroidNode>, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка ветки.
     */
    suspend fun encryptNode(node: TetroidNode, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей ветки.
     */
    fun encryptNodeFields(node: TetroidNode, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param isReencrypt Флаг, заставляющий шифровать файлы записи даже тогда, когда запись
     * уже зашифрована.
     */
    suspend fun encryptRecordsAndFiles(records: List<TetroidRecord>, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей записи.
     */
    fun encryptRecordFields(record: TetroidRecord, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей прикрепленного файла.
     */
    fun encryptAttach(file: TetroidFile, isReencrypt: Boolean): Boolean

    /**
     * Расшифровка веток.
     * @param isDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    suspend fun decryptNodes(
        nodes: List<TetroidNode>,
        isDecryptSubNodes: Boolean,
        isDecryptRecords: Boolean,
        loadIconCallback: suspend (TetroidNode) -> Unit,
        isDropCrypt: Boolean,
        isDecryptFiles: Boolean
    ): Boolean

    /**
     * Расшифровка ветки.
     * @param isDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    suspend fun decryptNode(
        node: TetroidNode,
        isDecryptSubNodes: Boolean,
        isDecryptRecords: Boolean,
        loadIconCallback: suspend (TetroidNode) -> Unit,
        isDropCrypt: Boolean,
        isDecryptFiles: Boolean
    ): Boolean

    /**
     * Расшифровка полей ветки.
     * @param idDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    fun decryptNodeFields(node: TetroidNode, idDropCrypt: Boolean): Boolean

    /**
     * Расшифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param idDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    suspend fun decryptRecordsAndFiles(
        records: List<TetroidRecord>,
        idDropCrypt: Boolean,
        decryptFiles: Boolean
    ): Boolean

    suspend fun decryptRecordAndFiles(
        record: TetroidRecord,
        dropCrypt: Boolean,
        decryptFiles: Boolean
    ): Boolean

    /**
     * Расшифровка полей записи.
     * @param idDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    suspend fun decryptRecordFields(
        record: TetroidRecord,
        idDropCrypt: Boolean
    ): Boolean

    /**
     * Расшифровка полей прикрепленного файла.
     * @param isDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    fun decryptAttach(file: TetroidFile, isDropCrypt: Boolean): Boolean

    fun decryptTextBase64(field: String): String?

    fun encryptTextBase64(field: String): String?

    fun decryptText(bytes: ByteArray): String?

    fun encryptTextBytes(text: String): ByteArray

    fun encryptOrDecryptFile(srcFileStream: InputStream, destFileStream: OutputStream, encrypt: Boolean): Boolean

    fun passToHash(pass: String): String

    fun checkPass(pass: String?, salt: String?, checkHash: String?): Boolean

    fun checkMiddlePassHash(passHash: String?, checkData: String?): Boolean

    fun createMiddlePassHashCheckData(passHash: String?): String?

    fun getErrorCode(): Int
}

class StorageCryptManager(
    private val logger: ITetroidLogger,
    private var crypter: Crypter = Crypter(logger),
) : IStorageCryptManager {


    private lateinit var cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase
    private lateinit var parseRecordTagsUseCase: ParseRecordTagsUseCase

    override fun init(
        cryptRecordFilesIfNeedUseCase: CryptRecordFilesIfNeedUseCase,
        parseRecordTagsUseCase: ParseRecordTagsUseCase,
    ) {
        this.cryptRecordFilesIfNeedUseCase = cryptRecordFilesIfNeedUseCase
        this.parseRecordTagsUseCase = parseRecordTagsUseCase
    }

    override fun reset() {
        crypter = Crypter(logger)
    }

    override fun setKeyFromPassword(pass: String) {
        val key = crypter.passToKey(pass)
        setCryptKey(key)
    }

    override fun setKeyFromMiddleHash(passHash: String) {
        val key = crypter.middlePassHashToKey(passHash)
        setCryptKey(key)
    }

    private fun setCryptKey(key: IntArray) {
        crypter.setCryptKey(key)
    }

    /**
     * Зашифровка веток.
     * @param isReencrypt Если true, то повторное шифрование зашифрованного объекта (должно быть расшифрованно перед этим)
     */
    override suspend fun encryptNodes(nodes: List<TetroidNode>, isReencrypt: Boolean): Boolean {
        var res = true
        for (node in nodes) {
            res = res and encryptNode(node, isReencrypt)
        }
        return res
    }

    /**
     * Зашифровка ветки.
     */
    override suspend fun encryptNode(node: TetroidNode, isReencrypt: Boolean): Boolean {
        var res = true
        if (!isReencrypt && !node.isEncrypted || isReencrypt && node.isEncrypted && node.isDecrypted) {
            // зашифровываем поля
            res = encryptNodeFields(node, isReencrypt)
            if (node.recordsCount > 0) {
                res = res and encryptRecordsAndFiles(node.records, isReencrypt)
            }
        }
        // зашифровываем подветки
        if (node.subNodesCount > 0) {
            res = res and encryptNodes(node.subNodes, isReencrypt)
        }
        return res
    }

    /**
     * Зашифровка полей ветки.
     */
    override fun encryptNodeFields(node: TetroidNode, isReencrypt: Boolean): Boolean {
        var res: Boolean
        // name
        var temp = encryptTextBase64(node.name)
        res = temp != null
        if (temp != null) {
            if (!isReencrypt && !node.isEncrypted) {
                node.decryptedName = node.name
            }
            node.sourceName = temp
        }
        // icon
        val iconName = node.iconName.orEmpty()
        if (iconName.isNotEmpty()) {
            temp = encryptTextBase64(iconName)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !node.isEncrypted) {
                    node.decryptedIconName = iconName
                }
                node.sourceIconName = temp
            }
        }
        // encryption result
        if (!isReencrypt && !node.isEncrypted) {
            node.isEncrypted = res
            node.isDecrypted = res
        }
        return res
    }

    /**
     * Зашифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param isReencrypt Флаг, заставляющий шифровать файлы записи даже тогда, когда запись
     * уже зашифрована.
     */
    override suspend fun encryptRecordsAndFiles(records: List<TetroidRecord>, isReencrypt: Boolean): Boolean {
        var res = true
        for (record in records) {
            // зашифровываем файлы записи
            res = res and cryptRecordFiles(
                record = record,
                isEncrypted = record.isEncrypted && !isReencrypt,
                isEncrypt = true
            )
            res = res and encryptRecordFields(record, isReencrypt)
            if (record.attachedFilesCount > 0) {
                for (file in record.attachedFiles) {
                    res = res and encryptAttach(file, isReencrypt)
                }
            }
        }
        return res
    }

    /**
     * Зашифровка полей записи.
     */
    override fun encryptRecordFields(record: TetroidRecord, isReencrypt: Boolean): Boolean {
        var res: Boolean
        var temp = encryptTextBase64(record.name)
        res = temp != null
        if (temp != null) {
            if (!isReencrypt && !record.isEncrypted) {
                record.decryptedName = record.name
            }
            record.sourceName = temp
        }
        val tagsString = record.tagsString
        if (!tagsString.isNullOrEmpty()) {
            temp = encryptTextBase64(tagsString)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !record.isEncrypted) {
                    record.decryptedTagsString = tagsString
                }
                record.tagsString = temp
            }
        }
        val author = record.author
        if (!author.isNullOrEmpty()) {
            temp = encryptTextBase64(author)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !record.isEncrypted) {
                    record.decryptedAuthor = author
                }
                record.author = temp
            }
        }
        val url = record.url
        if (!url.isNullOrEmpty()) {
            temp = encryptTextBase64(url)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !record.isEncrypted) {
                    record.decryptedUrl = url
                }
                record.url = temp
            }
        }
        if (!isReencrypt && !record.isEncrypted) {
            record.isEncrypted = res
            record.isDecrypted = res
        }
        return res
    }

    /**
     * Зашифровка полей прикрепленного файла.
     */
    override fun encryptAttach(file: TetroidFile, isReencrypt: Boolean): Boolean {
        val temp = encryptTextBase64(file.name)
        val res = temp != null
        if (temp != null) {
            if (!isReencrypt && !file.isEncrypted) {
                file.decryptedName = file.name
            }
            file.sourceName = temp
        }
        if (!isReencrypt && !file.isEncrypted) {
            file.isEncrypted = res
            file.isDecrypted = res
        }
        return res
    }

    /**
     * Расшифровка веток.
     * @param isDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override suspend fun decryptNodes(
        nodes: List<TetroidNode>,
        isDecryptSubNodes: Boolean,
        isDecryptRecords: Boolean,
        loadIconCallback: suspend (TetroidNode) -> Unit,
        isDropCrypt: Boolean,
        isDecryptFiles: Boolean
    ): Boolean {
        var res = true
        for (node in nodes) {
            res = res and decryptNode(
                node = node,
                isDecryptSubNodes = isDecryptSubNodes,
                isDecryptRecords = isDecryptRecords,
                loadIconCallback = loadIconCallback,
                isDropCrypt = isDropCrypt,
                isDecryptFiles = isDecryptFiles
            )
        }
        return res
    }

    /**
     * Расшифровка ветки.
     * @param isDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override suspend fun decryptNode(
        node: TetroidNode,
        isDecryptSubNodes: Boolean,
        isDecryptRecords: Boolean,
        loadIconCallback: suspend (TetroidNode) -> Unit,
        isDropCrypt: Boolean,
        isDecryptFiles: Boolean
    ): Boolean {
        var res = true
        if (node.isEncrypted && (!node.isDecrypted || isDropCrypt || isDecryptFiles)) {
            // расшифровываем поля
            res = decryptNodeFields(node, isDropCrypt)
            // загружаем иконку
            loadIconCallback(node)
            // TODO: расшифровывать список записей сразу или при выделении ?
            //  (пока сразу)
            if (isDecryptRecords && node.recordsCount > 0) {
                res = res and decryptRecordsAndFiles(
                    records = node.records,
                    idDropCrypt = isDropCrypt,
                    decryptFiles = isDecryptFiles
                )
            }
        }
        // расшифровываем подветки
        if (isDecryptSubNodes && node.subNodesCount > 0) {
            res = res and decryptNodes(
                nodes = node.subNodes,
                isDecryptSubNodes = true,
                isDecryptRecords = isDecryptRecords,
                loadIconCallback = loadIconCallback,
                isDropCrypt = isDropCrypt,
                isDecryptFiles = isDecryptFiles
            )
        }
        return res
    }

    /**
     * Расшифровка полей ветки.
     * @param idDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override fun decryptNodeFields(node: TetroidNode, idDropCrypt: Boolean): Boolean {
        var res: Boolean
        // name
        var temp = decryptTextBase64(node.sourceName)
        res = temp != null
        if (temp != null) {
            if (idDropCrypt) {
                node.sourceName = temp
                node.decryptedName = null
            } else {
                node.decryptedName = temp
            }
        }
        // icon
        node.sourceIconName?.also { iconName ->
            temp = decryptTextBase64(iconName)
            res = res and (temp != null)
        }
        if (temp != null) {
            if (idDropCrypt) {
                node.sourceIconName = temp
                node.decryptedIconName = null
            } else {
                node.decryptedIconName = temp
            }
        }
        // decryption result
        if (idDropCrypt) {
            node.isEncrypted = !res
            node.isDecrypted = !res
        } else {
            node.isDecrypted = res
        }
        return res
    }

    /**
     * Расшифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param idDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override suspend fun decryptRecordsAndFiles(records: List<TetroidRecord>, idDropCrypt: Boolean, decryptFiles: Boolean): Boolean {
        var res = true
        for (record in records) {
            res = res and decryptRecordAndFiles(record, idDropCrypt, decryptFiles)
        }
        return res
    }

    override suspend fun decryptRecordAndFiles(record: TetroidRecord, dropCrypt: Boolean, decryptFiles: Boolean): Boolean {
        var res = decryptRecordFields(record, dropCrypt)
        if (record.attachedFilesCount > 0) {
            for (file in record.attachedFiles) {
                res = res and decryptAttach(file, dropCrypt)
            }
        }
        // расшифровываем файлы записи
        if ((dropCrypt || decryptFiles)) {
            res = res and cryptRecordFiles(
                record = record,
                isEncrypted = true,
                isEncrypt = false
            )
        }
        return res
    }

    /**
     * Расшифровка полей записи.
     * @param idDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override suspend fun decryptRecordFields(record: TetroidRecord, idDropCrypt: Boolean): Boolean {
        var res: Boolean
        var temp = decryptTextBase64(record.sourceName)
        res = temp != null
        if (temp != null) {
            if (idDropCrypt) {
                record.sourceName = temp
                record.decryptedName = null
            } else {
                record.decryptedName = temp
            }
        }
        temp = record.sourceTagsString?.let { decryptTextBase64(it) }
        res = res and (temp != null)
        if (temp != null) {
            if (idDropCrypt) {
                record.tagsString = temp
                record.decryptedTagsString = null
            } else {
                record.decryptedTagsString = temp
            }
            parseRecordTagsUseCase.run(
                ParseRecordTagsUseCase.Params(
                    record = record,
                    tagsString = temp,
                )
            ).onFailure {
                logger.logFailure(it, show = false)
            }
        }
        temp = record.sourceAuthor?.let { decryptTextBase64(it) }
        res = res and (temp != null)
        if (temp != null) {
            if (idDropCrypt) {
                record.author = temp
                record.decryptedAuthor = null
            } else {
                record.decryptedAuthor = temp
            }
        }
        temp = record.sourceUrl?.let { decryptTextBase64(it) }
        res = res and (temp != null)
        if (temp != null) {
            if (idDropCrypt) {
                record.url = temp
                record.decryptedUrl = null
            } else {
                record.decryptedUrl = temp
            }
        }
        if (idDropCrypt) {
            record.isEncrypted = !res
            record.isDecrypted = !res
        } else {
            record.isDecrypted = res
        }
        return res
    }

    /**
     * Расшифровка полей прикрепленного файла.
     * @param isDropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override fun decryptAttach(file: TetroidFile, isDropCrypt: Boolean): Boolean {
        val temp = decryptTextBase64(file.sourceName)
        val res = temp != null
        if (temp != null) {
            if (isDropCrypt) {
                file.sourceName = temp
                file.decryptedName = null
            } else {
                file.decryptedName = temp
            }
        }
        if (isDropCrypt) {
            file.isEncrypted = !res
            file.isDecrypted = !res
        } else {
            file.isDecrypted = res
        }
        return res
    }

    override fun decryptTextBase64(field: String): String? {
        return crypter.decryptBase64(field)
    }

    override fun encryptTextBase64(field: String): String? {
        return crypter.encryptTextBase64(field)
    }

    override fun decryptText(bytes: ByteArray): String? {
        return crypter.decryptText(bytes)
    }

    override fun encryptTextBytes(text: String): ByteArray {
        return crypter.encryptTextBytes(text)
    }

    override fun encryptOrDecryptFile(srcFileStream: InputStream, destFileStream: OutputStream, encrypt: Boolean): Boolean {
        return crypter.encryptDecryptFile(srcFileStream, destFileStream, encrypt)
    }

    private suspend fun cryptRecordFiles(record: TetroidRecord, isEncrypted: Boolean, isEncrypt: Boolean): Boolean {
        return cryptRecordFilesIfNeedUseCase.run(
            CryptRecordFilesIfNeedUseCase.Params(
                record = record,
                isEncrypted = isEncrypted,
                isEncrypt = isEncrypt,
            )
        ).foldResult(
            onLeft = {
                logger.logFailure(it, show = false)
                false
            },
            onRight = { it }
        )
    }

    override fun passToHash(pass: String): String {
        return crypter.passToHash(pass)
    }

    override fun checkPass(pass: String?, salt: String?, checkHash: String?): Boolean {
        return crypter.checkPass(pass, salt, checkHash)
    }

    override fun checkMiddlePassHash(passHash: String?, checkData: String?): Boolean {
        return crypter.checkMiddlePassHash(passHash, checkData)
    }

    override fun createMiddlePassHashCheckData(passHash: String?): String? {
        return crypter.createMiddlePassHashCheckData(passHash)
    }

    override fun getErrorCode(): Int {
        return crypter.errorCode
    }

}
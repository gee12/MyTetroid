package com.gee12.mytetroid.domain.manager

import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.domain.usecase.crypt.CryptRecordFilesUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.logs.ITetroidLogger
import java.io.File

interface IStorageCryptManager {

    fun init(
        cryptRecordFilesUseCase: CryptRecordFilesUseCase,
        parseRecordTagsUseCase: ParseRecordTagsUseCase,
    )

    fun setKeyFromPassword(pass: String)
    fun setKeyFromMiddleHash(passHash: String)

    /**
     * Зашифровка веток.
     * @param nodes
     * @param isReencrypt Если true, то повторное шифрование зашифрованного объекта (должно быть расшифрованно перед этим)
     * @return
     */
    suspend fun encryptNodes(nodes: List<TetroidNode>, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    suspend fun encryptNode(node: TetroidNode, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    fun encryptNodeFields(node: TetroidNode, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param records
     * @param isReencrypt Флаг, заставляющий шифровать файлы записи даже тогда, когда запись
     * уже зашифрована.
     * @return
     */
    suspend fun encryptRecordsAndFiles(records: List<TetroidRecord>, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей записи.
     * @param record
     * @param isReencrypt
     * @return
     */
    fun encryptRecordFields(record: TetroidRecord, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка полей прикрепленного файла.
     * @param file
     * @param isReencrypt
     * @return
     */
    fun encryptAttach(file: TetroidFile, isReencrypt: Boolean): Boolean

    /**
     * Расшифровка веток.
     * @param nodes
     * @param isDecryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
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
     * @param node
     * @param isDecryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
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
     * @param node
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    fun decryptNodeFields(node: TetroidNode, dropCrypt: Boolean): Boolean

    /**
     * Расшифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param records
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    suspend fun decryptRecordsAndFiles(
        records: List<TetroidRecord>,
        dropCrypt: Boolean,
        decryptFiles: Boolean
    ): Boolean

    suspend fun decryptRecordAndFiles(
        record: TetroidRecord,
        dropCrypt: Boolean,
        decryptFiles: Boolean
    ): Boolean

    /**
     * Расшифровка полей записи.
     * @param record
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    suspend fun decryptRecordFields(
        record: TetroidRecord,
        dropCrypt: Boolean
    ): Boolean

    /**
     * Расшифровка полей прикрепленного файла.
     * @param file
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    fun decryptAttach(file: TetroidFile, dropCrypt: Boolean): Boolean

    fun decryptTextBase64(field: String): String?

    fun encryptTextBase64(field: String): String?

    fun decryptText(bytes: ByteArray): String

    fun encryptTextBytes(text: String): ByteArray

    fun encryptDecryptFile(srcFile: File, destFile: File, encrypt: Boolean): Boolean

    fun passToHash(pass: String): String

    fun checkPass(pass: String?, salt: String?, checkHash: String?): Boolean

    fun checkMiddlePassHash(passHash: String?, checkData: String?): Boolean

    fun createMiddlePassHashCheckData(passHash: String?): String?

    fun getErrorCode(): Int
}

class StorageCryptManager(
    private val logger: ITetroidLogger,
    private val crypter: Crypter,
) : IStorageCryptManager {


    private lateinit var cryptRecordFilesUseCase: CryptRecordFilesUseCase
    private lateinit var parseRecordTagsUseCase: ParseRecordTagsUseCase

    override fun init(
        cryptRecordFilesUseCase: CryptRecordFilesUseCase,
        parseRecordTagsUseCase: ParseRecordTagsUseCase,
    ) {
        this.cryptRecordFilesUseCase = cryptRecordFilesUseCase
        this.parseRecordTagsUseCase = parseRecordTagsUseCase
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
        if (!isReencrypt && !node.isCrypted || isReencrypt && node.isCrypted && node.isDecrypted) {
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
        if (res) {
            if (!isReencrypt && !node.isCrypted) {
                node.setDecryptedName(node.name)
            }
            node.name = temp
        }
        // icon
        val iconName = node.iconName.orEmpty()
        if (iconName.isNotEmpty()) {
            temp = encryptTextBase64(iconName)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !node.isCrypted) {
                    node.setDecryptedIconName(iconName)
                }
                node.iconName = temp
            }
        }
        // encryption result
        if (!isReencrypt && !node.isCrypted) {
            node.setIsCrypted(res)
            node.setIsDecrypted(res)
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
                isCrypted = record.isCrypted && !isReencrypt,
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
        if (res) {
            if (!isReencrypt && !record.isCrypted) {
                record.setDecryptedName(record.name)
            }
            record.name = temp
        }
        val tagsString = record.tagsString
        if (tagsString.isNotEmpty()) {
            temp = encryptTextBase64(tagsString)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !record.isCrypted) {
                    record.setDecryptedTagsString(tagsString)
                }
                record.tagsString = temp
            }
        }
        val author = record.author
        if (author.isNotEmpty()) {
            temp = encryptTextBase64(author)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !record.isCrypted) {
                    record.setDecryptedAuthor(author)
                }
                record.author = temp
            }
        }
        val url = record.url
        if (url.isNotEmpty()) {
            temp = encryptTextBase64(url)
            res = res and (temp != null)
            if (temp != null) {
                if (!isReencrypt && !record.isCrypted) {
                    record.setDecryptedUrl(url)
                }
                record.url = temp
            }
        }
        if (!isReencrypt && !record.isCrypted) {
            record.setIsCrypted(res)
            record.setIsDecrypted(res)
        }
        return res
    }

    /**
     * Зашифровка полей прикрепленного файла.
     */
    override fun encryptAttach(file: TetroidFile, isReencrypt: Boolean): Boolean {
        val temp = encryptTextBase64(file.name)
        val res = temp != null
        if (res) {
            if (!isReencrypt && !file.isCrypted) {
                file.setDecryptedName(file.name)
            }
            file.name = temp
        }
        if (!isReencrypt && !file.isCrypted) {
            file.setIsCrypted(res)
            file.setIsDecrypted(res)
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
        if (node.isCrypted && (!node.isDecrypted || isDropCrypt || isDecryptFiles)) {
            // расшифровываем поля
            res = decryptNodeFields(node, isDropCrypt)
            // загружаем иконку
            loadIconCallback(node)
            // TODO: расшифровывать список записей сразу или при выделении ?
            //  (пока сразу)
            if (isDecryptRecords && node.recordsCount > 0) {
                res = res and decryptRecordsAndFiles(
                    records = node.records,
                    dropCrypt = isDropCrypt,
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
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override fun decryptNodeFields(node: TetroidNode, dropCrypt: Boolean): Boolean {
        var res: Boolean
        // name
        var temp = decryptTextBase64(node.getName(true))
        res = temp != null
        if (res) {
            if (dropCrypt) {
                node.name = temp
                node.setDecryptedName(null)
            } else {
                node.setDecryptedName(temp)
            }
        }
        // icon
        node.getIconName(true)?.also { iconName ->
            temp = decryptTextBase64(iconName)
            res = res and (temp != null)
        }
        if (temp != null) {
            if (dropCrypt) {
                node.iconName = temp
                node.setDecryptedIconName(null)
            } else {
                node.setDecryptedIconName(temp)
            }
        }
        // decryption result
        if (dropCrypt) {
            node.setIsCrypted(!res)
            node.setIsDecrypted(!res)
        } else {
            node.setIsDecrypted(res)
        }
        return res
    }

    /**
     * Расшифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override suspend fun decryptRecordsAndFiles(records: List<TetroidRecord>, dropCrypt: Boolean, decryptFiles: Boolean): Boolean {
        var res = true
        for (record in records) {
            res = res and decryptRecordAndFiles(record, dropCrypt, decryptFiles)
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
                isCrypted = true,
                isEncrypt = false
            )
        }
        return res
    }

    /**
     * Расшифровка полей записи.
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override suspend fun decryptRecordFields(record: TetroidRecord, dropCrypt: Boolean): Boolean {
        var res: Boolean
        var temp = decryptTextBase64(record.getName(true))
        res = temp != null
        if (res) {
            if (dropCrypt) {
                record.name = temp
                record.setDecryptedName(null)
            } else {
                record.setDecryptedName(temp)
            }
        }
        temp = decryptTextBase64(record.getTagsString(true))
        res = res and (temp != null)
        if (temp != null) {
            if (dropCrypt) {
                record.tagsString = temp
                record.setDecryptedTagsString(null)
            } else {
                record.setDecryptedTagsString(temp)
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
        temp = decryptTextBase64(record.getAuthor(true))
        res = res and (temp != null)
        if (temp != null) {
            if (dropCrypt) {
                record.author = temp
                record.setDecryptedAuthor(null)
            } else {
                record.setDecryptedAuthor(temp)
            }
        }
        temp = decryptTextBase64(record.getUrl(true))
        res = res and (temp != null)
        if (temp != null) {
            if (dropCrypt) {
                record.url = temp
                record.setDecryptedUrl(null)
            } else {
                record.setDecryptedUrl(temp)
            }
        }
        if (dropCrypt) {
            record.setIsCrypted(!res)
            record.setIsDecrypted(!res)
        } else {
            record.setIsDecrypted(res)
        }
        return res
    }

    /**
     * Расшифровка полей прикрепленного файла.
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     */
    override fun decryptAttach(file: TetroidFile, dropCrypt: Boolean): Boolean {
        val temp = decryptTextBase64(file.getName(true))
        val res = temp != null
        if (res) {
            if (dropCrypt) {
                file.name = temp
                file.setDecryptedName(null)
            } else {
                file.setDecryptedName(temp)
            }
        }
        if (dropCrypt) {
            file.setIsCrypted(!res)
            file.setIsDecrypted(!res)
        } else {
            file.setIsDecrypted(res)
        }
        return res
    }

    override fun decryptTextBase64(field: String): String? {
        return crypter.decryptBase64(field)
    }

    override fun encryptTextBase64(field: String): String? {
        return crypter.encryptTextBase64(field)
    }

    override fun decryptText(bytes: ByteArray): String {
        return crypter.decryptText(bytes)
    }

    override fun encryptTextBytes(text: String): ByteArray {
        return crypter.encryptTextBytes(text)
    }

    override fun encryptDecryptFile(srcFile: File, destFile: File, encrypt: Boolean): Boolean {
        return crypter.encryptDecryptFile(srcFile, destFile, encrypt)
    }

    private suspend fun cryptRecordFiles(record: TetroidRecord, isCrypted: Boolean, isEncrypt: Boolean): Boolean {
        return cryptRecordFilesUseCase.run(
            CryptRecordFilesUseCase.Params(
                record = record,
                isCrypted = isCrypted,
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
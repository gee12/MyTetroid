package com.gee12.mytetroid.data.crypt

import android.content.Context
import com.gee12.mytetroid.data.ITagsParser
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.helpers.INodeIconLoader
import com.gee12.mytetroid.logs.ITetroidLogger
import org.jsoup.internal.StringUtil
import java.io.File

interface ITetroidCrypter {

    var middlePassHashOrNull: String?
    val tagsParser: ITagsParser
    val recordFileCrypter: IRecordFileCrypter

    fun initFromPass(pass: String)
    fun initFromMiddleHash(passHash: String)
    fun init(key: IntArray)

    /**
     * Зашифровка веток.
     * @param nodes
     * @param isReencrypt Если true, то повторное шифрование зашифрованного объекта (должно быть расшифрованно перед этим)
     * @return
     */
    suspend fun encryptNodes(context: Context, nodes: List<TetroidNode>, isReencrypt: Boolean): Boolean

    /**
     * Зашифровка ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    suspend fun encryptNode(context: Context, node: TetroidNode?, isReencrypt: Boolean): Boolean

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
    suspend fun encryptRecordsAndFiles(context: Context, records: List<TetroidRecord>, isReencrypt: Boolean): Boolean

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
    suspend fun decryptNodes(context: Context, nodes: List<TetroidNode>, isDecryptSubNodes: Boolean, decryptRecords: Boolean,
                             iconLoader: INodeIconLoader?, dropCrypt: Boolean, decryptFiles: Boolean
    ): Boolean

    /**
     * Расшифровка ветки.
     * @param node
     * @param decryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    suspend fun decryptNode(context: Context, node: TetroidNode?, decryptSubNodes: Boolean, decryptRecords: Boolean,
                            iconLoader: INodeIconLoader?, dropCrypt: Boolean, decryptFiles: Boolean
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
    suspend fun decryptRecordsAndFiles(context: Context, records: List<TetroidRecord>, dropCrypt: Boolean, decryptFiles: Boolean): Boolean

    suspend fun decryptRecordAndFiles(context: Context, record: TetroidRecord?, dropCrypt: Boolean, decryptFiles: Boolean): Boolean

    /**
     * Расшифровка полей записи.
     * @param record
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    fun decryptRecordFields(record: TetroidRecord, dropCrypt: Boolean): Boolean

    /**
     * Расшифровка полей прикрепленного файла.
     * @param file
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    fun decryptAttach(file: TetroidFile, dropCrypt: Boolean): Boolean

    fun decryptBase64(field: String?): String?

    fun encryptTextBase64(field: String?): String?

    fun decryptText(bytes: ByteArray): String

    fun encryptTextBytes(text: String): ByteArray

    fun encryptDecryptFile(srcFile: File, destFile: File, encrypt: Boolean): Boolean

    fun passToHash(pass: String): String

    fun checkPass(pass: String?, salt: String, checkHash: String): Boolean

    fun checkMiddlePassHash(passHash: String?, checkData: String): Boolean

    fun createMiddlePassHashCheckData(passHash: String?): String?

    fun getErrorCode(): Int
}

class TetroidCrypter(
    logger: ITetroidLogger?,
    override val tagsParser: ITagsParser,
    override val recordFileCrypter: IRecordFileCrypter
) : Crypter(logger), ITetroidCrypter {

    override var middlePassHashOrNull: String?
        get() = this.middlePassHash
        set(value) { this.middlePassHash = value }


    override fun initFromPass(pass: String) {
        val key = passToKey(pass)
        // записываем в память
        setCryptKey(key)
        init(key)
    }

    override fun initFromMiddleHash(passHash: String) {
        val key = middlePassHashToKey(passHash)
        init(key)
    }

    override fun init(key: IntArray) {
        // записываем в память
        setCryptKey(key)
    }

    /**
     * Зашифровка веток.
     * @param nodes
     * @param isReencrypt Если true, то повторное шифрование зашифрованного объекта (должно быть расшифрованно перед этим)
     * @return
     */
    override suspend fun encryptNodes(context: Context, nodes: List<TetroidNode>, isReencrypt: Boolean): Boolean {
        var res = true
        for (node in nodes) {
//            if (!isReencrypt && !node.isCrypted() || isReencrypt && node.isCrypted() && node.isDecrypted()) {
            res = res and encryptNode(context, node, isReencrypt)
            //            }
        }
        return res
    }

    /**
     * Зашифровка ветки.
     * @param node
     * @param isReencrypt
     * @return
     */
    override suspend fun encryptNode(context: Context, node: TetroidNode?, isReencrypt: Boolean): Boolean {
        if (node == null) return false
        var res = true
        if (!isReencrypt && !node.isCrypted || isReencrypt && node.isCrypted && node.isDecrypted) {
            // зашифровываем поля
            res = encryptNodeFields(node, isReencrypt)
            if (node.recordsCount > 0) {
                res = res and encryptRecordsAndFiles(context, node.records, isReencrypt)
            }
        }
        // зашифровываем подветки
        if (node.subNodesCount > 0) {
            res = res and encryptNodes(context, node.subNodes, isReencrypt)
        }
        return res
    }

    /**
     * Зашифровка полей ветки.
     * @param node
     * @param isReencrypt
     * @return
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
        val iconName = node.iconName
        if (!StringUtil.isBlank(iconName)) {
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
     * @param records
     * @param isReencrypt Флаг, заставляющий шифровать файлы записи даже тогда, когда запись
     * уже зашифрована.
     * @return
     */
    override suspend fun encryptRecordsAndFiles(context: Context, records: List<TetroidRecord>, isReencrypt: Boolean): Boolean {
        var res = true
        for (record in records) {
            // зашифровываем файлы записи
            res = res and recordFileCrypter.cryptRecordFiles(context, record, record.isCrypted && !isReencrypt, true)
            res = res and encryptRecordFields(record, isReencrypt)
            if (record.attachedFilesCount > 0) for (file in record.attachedFiles) {
                res = res and encryptAttach(file, isReencrypt)
            }
        }
        return res
    }

    /**
     * Зашифровка полей записи.
     * @param record
     * @param isReencrypt
     * @return
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
        if (!StringUtil.isBlank(tagsString)) {
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
        if (!StringUtil.isBlank(author)) {
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
        if (!StringUtil.isBlank(url)) {
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
     * @param file
     * @param isReencrypt
     * @return
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
     * @param nodes
     * @param isDecryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    override suspend fun decryptNodes(context: Context, nodes: List<TetroidNode>, isDecryptSubNodes: Boolean, decryptRecords: Boolean,
                                      iconLoader: INodeIconLoader?, dropCrypt: Boolean, decryptFiles: Boolean
    ): Boolean {
        var res = true
        for (node in nodes) {
            res = res and decryptNode(context, node, isDecryptSubNodes, decryptRecords, iconLoader, dropCrypt, decryptFiles)
        }
        return res
    }

    /**
     * Расшифровка ветки.
     * @param node
     * @param decryptSubNodes
     * @param iconLoader
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    override suspend fun decryptNode(context: Context, node: TetroidNode?, decryptSubNodes: Boolean, decryptRecords: Boolean,
                                     iconLoader: INodeIconLoader?, dropCrypt: Boolean, decryptFiles: Boolean
    ): Boolean {
        if (node == null) return false
        var res = true
        if (node.isCrypted && (!node.isDecrypted || dropCrypt || decryptFiles)) {
            // расшифровываем поля
            res = decryptNodeFields(node, dropCrypt)
            // загружаем иконку
            iconLoader?.loadIcon(context, node)
            // TODO: расшифровывать список записей сразу или при выделении ?
            //  (пока сразу)
            if (decryptRecords && node.recordsCount > 0) {
                res = res and decryptRecordsAndFiles(context, node.records, dropCrypt, decryptFiles)
            }
        }
        // расшифровываем подветки
        if (decryptSubNodes && node.subNodesCount > 0) {
            res = res and decryptNodes(context, node.subNodes, true, decryptRecords, iconLoader, dropCrypt, decryptFiles)
        }
        return res
    }

    /**
     * Расшифровка полей ветки.
     * @param node
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    override fun decryptNodeFields(node: TetroidNode, dropCrypt: Boolean): Boolean {
        var res: Boolean
        // name
        var temp = decryptBase64(node.getName(true))
        res = temp != null
        if (res) {
            if (dropCrypt) {
                node.name = temp
                node.setDecryptedName(null)
            } else node.setDecryptedName(temp)
        }
        // icon
        temp = decryptBase64(node.getIconName(true))
        res = res and (temp != null)
        if (temp != null) {
            if (dropCrypt) {
                node.iconName = temp
                node.setDecryptedIconName(null)
            } else node.setDecryptedIconName(temp)
        }
        // decryption result
        if (dropCrypt) {
            node.setIsCrypted(!res)
            node.setIsDecrypted(!res)
        } else node.setIsDecrypted(res)
        return res
    }

    /**
     * Расшифровка полей списка записей и полей их прикрепленных прифайлов.
     * @param records
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    override suspend fun decryptRecordsAndFiles(context: Context, records: List<TetroidRecord>, dropCrypt: Boolean, decryptFiles: Boolean): Boolean {
        var res = true
        for (record in records) {
            res = res and decryptRecordAndFiles(context, record, dropCrypt, decryptFiles)
        }
        return res
    }

    override suspend fun decryptRecordAndFiles(context: Context, record: TetroidRecord?, dropCrypt: Boolean, decryptFiles: Boolean): Boolean {
        if (record == null) return false

        var res = decryptRecordFields(record, dropCrypt)
        if (record.attachedFilesCount > 0) for (file in record.attachedFiles) {
            res = res and decryptAttach(file, dropCrypt)
        }
        // расшифровываем файлы записи
        if ((dropCrypt || decryptFiles)) {
            res = res and recordFileCrypter.cryptRecordFiles(context, record, true, false)
        }
        return res
    }

    /**
     * Расшифровка полей записи.
     * @param record
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    override fun decryptRecordFields(record: TetroidRecord, dropCrypt: Boolean): Boolean {
        var res: Boolean
        var temp = decryptBase64(record.getName(true))
        res = temp != null
        if (res) {
            if (dropCrypt) {
                record.name = temp
                record.setDecryptedName(null)
            } else record.setDecryptedName(temp)
        }
        temp = decryptBase64(record.getTagsString(true))
        res = res and (temp != null)
        if (temp != null) {
            if (dropCrypt) {
                record.tagsString = temp
                record.setDecryptedTagsString(null)
            } else record.setDecryptedTagsString(temp)
            tagsParser.parseRecordTags(record, temp)
        }
        temp = decryptBase64(record.getAuthor(true))
        res = res and (temp != null)
        if (temp != null) {
            if (dropCrypt) {
                record.author = temp
                record.setDecryptedAuthor(null)
            } else record.setDecryptedAuthor(temp)
        }
        temp = decryptBase64(record.getUrl(true))
        res = res and (temp != null)
        if (temp != null) {
            if (dropCrypt) {
                record.url = temp
                record.setDecryptedUrl(null)
            } else record.setDecryptedUrl(temp)
        }
        if (dropCrypt) {
            record.setIsCrypted(!res)
            record.setIsDecrypted(!res)
        } else record.setIsDecrypted(res)
        return res
    }

    /**
     * Расшифровка полей прикрепленного файла.
     * @param file
     * @param dropCrypt Если true - сбросить шифрование объекта, false - временная расшифровка.
     * @return
     */
    override fun decryptAttach(file: TetroidFile, dropCrypt: Boolean): Boolean {
        val temp = decryptBase64(file.getName(true))
        val res = temp != null
        if (res) {
            if (dropCrypt) {
                file.name = temp
                file.setDecryptedName(null)
            } else file.setDecryptedName(temp)
        }
        if (dropCrypt) {
            file.setIsCrypted(!res)
            file.setIsDecrypted(!res)
        } else file.setIsDecrypted(res)
        return res
    }
}
package com.gee12.mytetroid.model.obj

import com.gee12.mytetroid.model.enums.TetroidObjectType
import java.util.Date

class TetroidRecord(
    isEncrypted: Boolean,
    id: String,
    sourceName: String,
    var sourceTagsString: String?,
    var sourceAuthor: String?,
    var sourceUrl: String?,
    var node: TetroidNode,
    var created: Date? = null,
    var isNew: Boolean = false,
    var isTemporary: Boolean = false,
    var folderName: String,
    var fileName: String = DEF_FILE_NAME,
    var attachedFiles: MutableList<TetroidFile> = mutableListOf(),
    var tags: MutableList<TetroidTag> = mutableListOf(),
    var decryptedTagsString: String? = null,
    var decryptedAuthor: String? = null,
    var decryptedUrl: String? = null,
) : TetroidObject(
    id = id,
    type = TetroidObjectType.RECORD,
    isEncrypted = isEncrypted,
    sourceName = sourceName,
) {
    companion object {
        const val DEF_FILE_NAME = "text.html"
    }

    fun setDecryptedValues(name: String?, tagsString: String?, author: String?, url: String?) {
        decryptedName = name
        decryptedTagsString = tagsString
        decryptedAuthor = author
        decryptedUrl = url
    }

    var tagsString: String?
        get() = if (isEncrypted && isDecrypted) decryptedTagsString else sourceTagsString
        set(tagsString) {
            sourceTagsString = tagsString
        }

    var author: String?
        get() = if (isEncrypted && isDecrypted) decryptedAuthor else sourceAuthor
        set(author) {
            sourceAuthor = author
        }

    var url: String?
        get() = if (isEncrypted && isDecrypted) decryptedUrl else sourceUrl
        set(url) {
            sourceUrl = url
        }

    val attachedFilesCount: Int
        get() = attachedFiles.size

    fun addTag(tag: TetroidTag) {
        tags.add(tag)
    }

}

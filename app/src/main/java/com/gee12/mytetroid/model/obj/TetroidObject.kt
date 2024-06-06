package com.gee12.mytetroid.model.obj

import android.os.Parcelable
import com.gee12.mytetroid.model.enums.TetroidObjectType
import kotlinx.parcelize.Parcelize

/**
 * Объект хранилища.
 */
@Parcelize
open class TetroidObject(
    override val id: String,
    override val type: TetroidObjectType,
    override var sourceName: String,
    override var isEncrypted: Boolean = false,
    override var isDecrypted: Boolean = false,
    var decryptedName: String? = null,
    var isFavorite: Boolean = false,
) : ITetroidObject, Parcelable {

    override val name: String
        get() = if (isEncrypted && isDecrypted) decryptedName.orEmpty() else sourceName

    /**
     * Получение признака, что объект не зашифрован или уже расшифрован.
     */
    val isNonEncryptedOrDecrypted: Boolean
        get() = !isEncrypted || isDecrypted

}

package com.gee12.mytetroid.model.obj

import android.graphics.drawable.Drawable
import com.gee12.mytetroid.model.enums.TetroidObjectType

class TetroidNode(
    id: String,
    sourceName: String,
    isEncrypted: Boolean = false,
    var level: Int,
    var subNodes: MutableList<TetroidNode> = mutableListOf(),
    var records: MutableList<TetroidRecord> = mutableListOf(),
    var icon: Drawable? = null,
    var sourceIconName: String? = null,
    var decryptedIconName: String? = null,
    var parentNode: TetroidNode? = null,
) : TetroidObject(
    id = id,
    type = TetroidObjectType.NODE,
    sourceName = sourceName,
    isEncrypted = isEncrypted,
) {

    companion object {
        val Empty = TetroidNode(
            id = "EMPTY",
            sourceName = "",
            level = 0,
        )
    }

    val isExpandable: Boolean
        get() = subNodes.isNotEmpty()

    val iconName: String?
        get() = if (isEncrypted && isDecrypted) decryptedIconName else sourceIconName

    val subNodesCount: Int
        get() = subNodes.size

    val recordsCount: Int
        get() = records.size

    fun addSubNode(subNode: TetroidNode) {
        subNodes.add(subNode)
    }

    fun addRecord(record: TetroidRecord): Boolean {
        record.node = this
        return records.add(record)
    }

    fun deleteRecord(record: TetroidRecord): Boolean {
        record.node = Empty
        return records.remove(record)
    }
}

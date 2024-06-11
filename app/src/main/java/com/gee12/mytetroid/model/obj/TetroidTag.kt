package com.gee12.mytetroid.model.obj

import com.gee12.mytetroid.model.enums.TetroidObjectType

class TetroidTag(
    sourceName: String,
    val records: MutableList<TetroidRecord> = mutableListOf(),
    val isEmpty: Boolean = false,
) : TetroidObject(
    id = sourceName,
    type = TetroidObjectType.TAG,
    sourceName = sourceName,
) {

    fun addRecord(record: TetroidRecord) {
        records.add(record)
    }

    fun getRecord(index: Int): TetroidRecord? {
        return records.getOrNull(index)
    }
}

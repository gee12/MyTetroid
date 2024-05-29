package com.gee12.mytetroid.model

import com.gee12.mytetroid.model.enums.TetroidObjectType
import java.util.Date

data class HistoryEntity(
    val id: Int? = null,
    val storageId: Int,
    val obj: TetroidObject,
    val type: TetroidObjectType,
    val createdDate: Date?,
)

package com.gee12.mytetroid.ui.main

import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidObject

class ClipboardParams(
    val operation: LogOper,
    val obj: TetroidObject,
    val isCutted: Boolean = false
)
package com.gee12.mytetroid.ui.main

import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.obj.TetroidObject

class ClipboardParams(
    val operation: LogOper,
    val obj: TetroidObject,
    val isCutting: Boolean = false
)
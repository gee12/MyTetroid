package com.gee12.mytetroid.ui.record

import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord

class ResultObj {
    var type = 0
    var id: String? = null
    var obj: Any? = null
    var needReloadText = false

    internal constructor(type: Int) {
        init(type, null, null)
    }

    internal constructor(type: Int, id: String?) {
        // если есть id, значит obj использоваться не будет
        init(type, id, null)
    }

    internal constructor(obj: Any?) {
        this.obj = obj
        when (obj) {
            is TetroidRecord -> {
                type = OPEN_RECORD
                id = obj.id
            }
            is TetroidNode -> {
                type = OPEN_NODE
                id = obj.id
            }
            is TetroidFile -> {
                type = OPEN_FILE
                id = obj.id
            }
            is String -> {
                type = OPEN_TAG
            }
            else -> {
                type = NONE
            }
        }
    }

    private fun init(type: Int, id: String?, obj: Any?) {
        this.type = type
        this.id = id
        this.obj = obj
    }

    companion object {
        const val NONE = 0
        const val EXIT = 1
        const val START_MAIN_ACTIVITY = 2
        const val OPEN_RECORD = 3
        const val OPEN_NODE = 4
        const val OPEN_FILE = 5
        const val OPEN_TAG = 6
    }
}
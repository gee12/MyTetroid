package com.gee12.mytetroid.logs

import com.gee12.mytetroid.common.Constants.TetroidView
import com.gee12.mytetroid.logs.TetroidLog.Objs
import com.gee12.mytetroid.logs.TetroidLog.Opers

class TaskStage {

    interface ITaskStageExecutor {
        suspend fun execute(): Boolean
    }

    enum class Stages {
        START,
        SUCCESS,
        FAILED
    }

    @JvmField
    var viewType: TetroidView? = null
    @JvmField
    var obj: Objs? = null
    @JvmField
    var oper: Opers? = null
    @JvmField
    var stage: Stages? = null
    @JvmField
    var writeLog = false

    constructor(viewType: TetroidView?) {
        this.viewType = viewType
    }

    constructor(viewType: TetroidView, obj: Objs, oper: Opers, stage: Stages) {
        setValues(viewType, obj, oper, stage, true)
    }

    constructor(viewType: TetroidView, obj: Objs, oper: Opers, stage: Stages, writeLog: Boolean) {
        setValues(viewType, obj, oper, stage, writeLog)
    }

    private fun setValues(task: TetroidView, obj: Objs, oper: Opers, stage: Stages, writeLog: Boolean) {
        viewType = task
        this.obj = obj
        this.oper = oper
        this.stage = stage
        this.writeLog = writeLog
    }
}
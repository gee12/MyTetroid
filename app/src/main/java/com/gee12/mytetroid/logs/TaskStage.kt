package com.gee12.mytetroid.logs

import com.gee12.mytetroid.common.Constants.TetroidView

class TaskStage(
    var viewType: TetroidView,
    var obj: LogObj = LogObj.NONE,
    var oper: LogOper = LogOper.NONE,
    var stage: Stages = Stages.NONE,
    var writeLog: Boolean = false
) {

    interface ITaskStageExecutor {
        suspend fun execute(): Boolean
    }

    enum class Stages {
        NONE,
        START,
        SUCCESS,
        FAILED
    }

//    constructor(viewType: TetroidView?) {
//        this.viewType = viewType
//    }
//
//    constructor(viewType: TetroidView, obj: LogObj, oper: LogOper, stage: Stages) {
//        setValues(viewType, obj, oper, stage, true)
//    }
//
//    constructor(viewType: TetroidView, obj: LogObj, oper: LogOper, stage: Stages, writeLog: Boolean) {
//        setValues(viewType, obj, oper, stage, writeLog)
//    }

//    private fun setValues(task: TetroidView, obj: LogObj, oper: LogOper, stage: Stages, writeLog: Boolean) {
//        viewType = task
//        this.obj = obj
//        this.oper = oper
//        this.stage = stage
//        this.writeLog = writeLog
//    }
}
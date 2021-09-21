package com.gee12.mytetroid.data

import com.gee12.mytetroid.logs.TaskStage.Stages
import com.gee12.mytetroid.logs.TetroidLog.Objs
import com.gee12.mytetroid.logs.TetroidLog.Opers

interface ITaskProgress {
    suspend fun nextStage(obj: Objs, oper: Opers, stage: Stages)
    suspend fun nextStage(obj: Objs, oper: Opers, stageExecutor: suspend () -> Boolean): Boolean
}
package com.gee12.mytetroid.data

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage.Stages

interface ITaskProgress {
    suspend fun nextStage(obj: LogObj, oper: LogOper, stage: Stages)
    suspend fun <L,R> nextStage(obj: LogObj, oper: LogOper, stageExecutor: suspend () -> Either<L,R>): Either<L,R>
}
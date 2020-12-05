package com.gee12.mytetroid.data;

import com.gee12.mytetroid.logs.TaskStage;
import com.gee12.mytetroid.logs.TetroidLog;

public interface ITaskProgress {

    void nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage);
    boolean nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.ITaskStageExecutor stageExecutor);
}

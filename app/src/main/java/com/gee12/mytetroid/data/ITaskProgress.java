package com.gee12.mytetroid.data;

import com.gee12.mytetroid.TaskStage;
import com.gee12.mytetroid.TetroidLog;

public interface ITaskProgress {

    void nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.Stages stage);
    boolean nextStage(TetroidLog.Objs obj, TetroidLog.Opers oper, TaskStage.ITaskStageExecutor stageExecutor);
}

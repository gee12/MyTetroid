package com.gee12.mytetroid.data;

import com.gee12.mytetroid.TetroidLog;

public interface ITaskProgress {
    void nextStage(String stageName);
    void stageResult(TetroidLog.Opers oper, boolean res);
}

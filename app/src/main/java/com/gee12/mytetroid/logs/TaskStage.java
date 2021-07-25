package com.gee12.mytetroid.logs;

import com.gee12.mytetroid.common.Constants;

public class TaskStage {

    public interface ITaskStageExecutor {
        boolean execute();
    }

    public enum Stages {
        START,
        SUCCESS,
        FAILED
    }

    Constants.TetroidView viewType;
    TetroidLog.Objs obj;
    TetroidLog.Opers oper;
    Stages stage;
    boolean writeLog;

    public TaskStage(Constants.TetroidView viewType) {
        this.viewType = viewType;
    }

    public TaskStage(Constants.TetroidView viewType, TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage) {
        setValues(viewType, obj, oper, stage, true);
    }

    public TaskStage(Constants.TetroidView viewType, TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage, boolean writeLog) {
        setValues(viewType, obj, oper, stage, writeLog);
    }

    private void setValues(Constants.TetroidView task, TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage, boolean writeLog) {
        this.viewType = task;
        this.obj = obj;
        this.oper = oper;
        this.stage = stage;
        this.writeLog = writeLog;
    }
}

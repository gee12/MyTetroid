package com.gee12.mytetroid;

public class TaskStage {

    public interface ITaskStageExecutor {
        boolean execute();
    }

    public enum Stages {
        START,
        SUCCESS,
        FAILED
    }

    Class clazz;
    TetroidLog.Objs obj;
    TetroidLog.Opers oper;
    Stages stage;
    boolean writeLog;

    public TaskStage(Class clazz) {
        this.clazz = clazz;
    }

    public TaskStage(Class clazz, TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage) {
        setValues(clazz, obj, oper, stage, true);
    }

    public TaskStage(Class clazz, TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage, boolean writeLog) {
        setValues(clazz, obj, oper, stage, writeLog);
    }

    private void setValues(Class clazz, TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage, boolean writeLog) {
        this.clazz = clazz;
        this.obj = obj;
        this.oper = oper;
        this.stage = stage;
        this.writeLog = writeLog;
    }
}

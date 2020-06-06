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

    public TaskStage(Class clazz) {
        this.clazz = clazz;
    }

    public TaskStage(Class clazz, TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage) {
        this.clazz = clazz;
        setValues(obj, oper, stage);
    }

    private void setValues(TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage) {
        this.obj = obj;
        this.oper = oper;
        this.stage = stage;
    }
}

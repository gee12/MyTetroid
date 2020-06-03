package com.gee12.mytetroid;

public class TaskStage {

    public enum Stages {
        START,
        SUCCESS,
        FAILED
    }

    TetroidLog.Objs obj;
    TetroidLog.Opers oper;
    String stageName;
    Stages stage;

    public TaskStage(TetroidLog.Objs obj, TetroidLog.Opers oper, Stages stage) {
        this.obj = obj;
        this.oper = oper;
        this.stage = stage;
    }
}

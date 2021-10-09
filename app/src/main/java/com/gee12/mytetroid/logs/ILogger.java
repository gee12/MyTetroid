package com.gee12.mytetroid.logs;

@Deprecated
public interface ILogger {
    /**
     * Типы логов.
     */
    enum Types {
        INFO,
        DEBUG,
        WARNING,
        ERROR
    }

    void log(String s);
    void log(int resId);
    void log(String s, Types type);
    void log(int resId, Types type);
    void log(Exception ex);
    void log(String s, Exception ex);
}

package com.gee12.mytetroid;

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
    void log(String s, Types type);
    void log(Exception ex);
    void log(String s, Exception ex);
}

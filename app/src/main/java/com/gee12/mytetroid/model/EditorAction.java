package com.gee12.mytetroid.model;

import com.gee12.htmlwysiwygeditor.ActionType;

/**
 * Типы данных для хранения параметров команд в панели инструментов редактора в базе данных.
 */
public class EditorAction {

    private ActionType type;
    private int order;
    private boolean isEnabled;
    private boolean isActive;

    public ActionType getType() {
        return type;
    }

    public int getOrder() {
        return order;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isActive() {
        return isActive;
    }
}

package com.gee12.mytetroid.model

import com.gee12.htmlwysiwygeditor.enums.ActionType

/**
 * Типы данных для хранения параметров команд в панели инструментов редактора в базе данных.
 */
data class EditorAction(
    val type: ActionType,
    val order: Int = 0,
    val isEnabled: Boolean = false,
    val isActive: Boolean = false,
)

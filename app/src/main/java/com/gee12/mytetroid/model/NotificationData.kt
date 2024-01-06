package com.gee12.mytetroid.model

sealed class NotificationData(
    val title: String,
    val message: String? = null,
    val type: Type? = null,
) {

    enum class Type {
        INFO,
        DEBUG,
        WARNING,
        ERROR,
    }

    class Empty : NotificationData(
        title = "",
        type = Type.INFO
    )

    class Info(
        title: String,
        message: String? = null,
    ) : NotificationData(title, message, Type.INFO)

    class Debug(
        title: String,
        message: String? = null,
    ) : NotificationData(title, message, Type.DEBUG)

    class Warning(
        title: String,
        message: String? = null,
    ) : NotificationData(title, message, Type.WARNING)

    class Error(
        title: String,
        message: String? = null,
    ) : NotificationData(title, message, Type.ERROR)

    fun getFullMassage(): String {
        return buildString {
            appendLine(title)
            if (!message.isNullOrBlank()) {
                append(message)
            }
        }
    }

}
package com.gee12.mytetroid.model

import com.gee12.mytetroid.logs.LogType

data class NotificationData(
    val title: String,
    val message: String? = null,
    val type: LogType? = null,
) {

    companion object {

        val Empty = NotificationData(title = "")

    }

}
package com.gee12.mytetroid.ui.base

import android.content.Intent
import android.os.Bundle
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission

abstract class BaseEvent : VMEvent() {

    // ui
    object ShowMoreInLogs : BaseEvent()

    // permission
    sealed class Permission : BaseEvent() {

        data class Check(
            val permission: TetroidPermission,
            val requestCode: PermissionRequestCode,
        ) : Permission()

        data class ShowRequest(
            val permission: TetroidPermission,
            val requestCode: PermissionRequestCode,
            val requestCallback: (() -> Unit)?,
        ) : Permission()

        data class Granted(
            val permission: TetroidPermission,
            val requestCode: PermissionRequestCode,
        ) : Permission()

        data class Canceled(
            val permission: TetroidPermission,
            val requestCode: PermissionRequestCode,
        ) : Permission()
    }

    // long-term tasks
    data class TaskStarted(
        val titleResId: Int? = null,
    ) : BaseEvent()
    object TaskFinished : BaseEvent()
    object ShowProgress : BaseEvent()
    object HideProgress : BaseEvent()
    data class ShowProgressWithText(
        val message: String,
    ) : BaseEvent()
}
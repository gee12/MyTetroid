package com.gee12.mytetroid.model.permission

import android.app.Activity

data class PermissionRequestData(
    val permission: TetroidPermission,
    val activity: Activity,
    val requestCode: PermissionRequestCode,
    val onManualPermissionRequest: (() -> Unit) -> Unit
)
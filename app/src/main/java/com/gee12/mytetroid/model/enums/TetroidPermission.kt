package com.gee12.mytetroid.model.enums

import android.Manifest
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.interactor.PermissionManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider

enum class TetroidPermission {
    ReadStorage,
    WriteStorage,
    Camera,
    RecordAudio,
    Termux;

    fun toAndroidPermission(): String {
        return when (this) {
            ReadStorage -> Manifest.permission.READ_EXTERNAL_STORAGE
            // запуск камеры для захвата изображения
            Camera -> Manifest.permission.CAMERA
            // голосовой ввод
            RecordAudio -> Manifest.permission.RECORD_AUDIO
            // запуск команд Termux
            Termux -> PermissionManager.PERMISSION_TERMUX
            else -> ""
        }
    }

    fun getPermissionRequestMessage(resourcesProvider: IResourcesProvider): String {
        return when (this) {
            ReadStorage -> R.string.ask_request_read_ext_storage
            WriteStorage -> R.string.ask_request_write_ext_storage
            Camera -> R.string.ask_request_camera
            RecordAudio -> R.string.ask_request_record_audio
            Termux -> R.string.ask_permission_termux
        }.let {
            resourcesProvider.getString(it)
        }
    }

}
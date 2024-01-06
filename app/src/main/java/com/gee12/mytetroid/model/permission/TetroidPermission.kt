package com.gee12.mytetroid.model.permission

import android.Manifest
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.manager.PermissionManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider

sealed class TetroidPermission {
    object FullFileStorage : TetroidPermission()
    sealed class FileStorage(val uri: Uri) : TetroidPermission() {
        class Read(uri: Uri) : FileStorage(uri)
        class Write(uri: Uri) : FileStorage(uri)
    }
    object Camera : TetroidPermission()
    object RecordAudio : TetroidPermission()
    object Termux : TetroidPermission()

    fun toAndroidPermission(): String {
        return when (this) {
            is FileStorage.Read -> Manifest.permission.READ_EXTERNAL_STORAGE
            is FileStorage.Write -> Manifest.permission.READ_EXTERNAL_STORAGE
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
            FullFileStorage -> R.string.ask_permission_on_all_files_on_device_title
            is FileStorage.Read -> R.string.ask_request_read_ext_storage
            is FileStorage.Write -> R.string.ask_request_write_ext_storage
            Camera -> R.string.ask_request_camera
            RecordAudio -> R.string.ask_request_record_audio
            Termux -> R.string.ask_permission_termux
        }.let {
            resourcesProvider.getString(it)
        }
    }

}
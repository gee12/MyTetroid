package com.gee12.mytetroid.ui.base

import androidx.documentfile.provider.DocumentFile

interface ITetroidFileStorage {
    fun isUseFileStorage() = false
    fun onStorageAccessGranted(requestCode: Int, root: DocumentFile)
    fun onFolderSelected(requestCode: Int, folder: DocumentFile)
    fun onFileSelected(requestCode: Int, files: List<DocumentFile>)
    //fun onFileCreated(requestCode: Int, file: DocumentFile)
    // TODO: принятие файла(ов) в компонент по Intent
    //fun onFileReceived(files: List<DocumentFile>)
    //fun onNonFileReceived(intent: Intent)
}
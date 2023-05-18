package com.gee12.mytetroid.domain.manager

import android.content.Context
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*

class FileStorageManager(
    private val context: Context,
) {

    fun isFileApiUsed(): Boolean {
        return Build.VERSION.SDK_INT <= 29
    }

    fun checkReadFileStoragePermission(root: DocumentFile): Boolean {
        // на Android 10 и ниже проверка возвращает false, поэтому на этих устройствах не проверяем
        return isFileApiUsed() || root.canRead()
    }

    fun checkWriteFileStoragePermission(root: DocumentFile): Boolean {
        // на Android 10 и ниже проверка возвращает false, поэтому на этих устройствах не проверяем
        return isFileApiUsed() || root.isWritable(context)
    }

    fun checkFolder(folder: DocumentFile): DocumentFile? {
        // используем File Api (uri со схемой file://) когда это возможно, т.к. работает быстрее
        return when {
            // если файл уже использует File Api, то оставляем как есть
            folder.isRawFile -> {
                folder
            }
            // если версия Android еще использует File Api, то преобразуем uri (например, со схемой content://)
            // в формат для File Api (uri со схемой file://)
            isFileApiUsed() -> {
                DocumentFileCompat.fromFullPath(
                    context = context,
                    fullPath = folder.getAbsolutePath(context),
                    documentType = DocumentFileType.FOLDER,
                    requiresWriteAccess = true,
                )
                //folder.toRawDocumentFile(getContext())
            }
            // иначе используем Scoped Storage и DocumentApi (в т.ч. со схемой content://)
            else -> {
                folder
            }
        }
    }

}
package com.gee12.mytetroid.domain.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.files.folderChooser
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.extension.fromTreeUri
import com.anggrayudi.storage.extension.getStorageId
import com.anggrayudi.storage.extension.isDocumentsDocument
import com.anggrayudi.storage.extension.isExternalStorageDocument
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.domain.ComponentWrapper
import java.io.File

class FileStorageManager(
    private val context: Context,
    var folderPickerCallback: FolderPickerCallback?,
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
                /*|| SimpleStorage.hasStorageAccess(
                        context = context,
                        fullPath = root.getAbsolutePath(context),
                        requiresWriteAccess = true
                    )*/
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

    /**
     * Копипаста из SimpleStorage
     */
    fun openFolderPicker(wrapper: ComponentWrapper, requestCode: Int, initialPath: FileFullPath? = null) {
        initialPath?.checkIfStorageIdIsAccessibleInSafSelector()

        if (Build.VERSION.SDK_INT < 21) {
            MaterialDialog(context).folderChooser(
                context,
                initialDirectory = initialPath?.let { File(it.absolutePath) },
                allowFolderCreation = true,
                selection = { _, file ->
                    folderPickerCallback?.onFolderSelected(requestCode, DocumentFile.fromFile(file))
                }
            ).negativeButton(android.R.string.cancel, click = { it.cancel() })
                .onCancel { folderPickerCallback?.onCanceledByUser(requestCode) }
                .show()
            return
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || SimpleStorage.hasStoragePermission(context)) {
            val intent = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            } else {
                externalStorageRootAccessIntent
            }
            addInitialPathToIntent(intent, initialPath)
            if (!wrapper.startActivityForResult(intent, requestCode))
                folderPickerCallback?.onActivityHandlerNotFound(requestCode, intent)
                return
        } else {
            folderPickerCallback?.onStoragePermissionDenied(requestCode)
        }
    }

    fun handleActivityResultForFolderPicker(requestCode: Int, uri: Uri) {
        val folder = context.fromTreeUri(uri)
        val storageId = uri.getStorageId(context)
        val storageType = StorageType.fromStorageId(storageId)

        if (folder == null || !folder.canModify(context)) {
            folderPickerCallback?.onStorageAccessDenied(requestCode, folder, storageType, storageId)
            return
        }
        if (uri.toString().let { it == DocumentFileCompat.DOWNLOADS_TREE_URI || it == DocumentFileCompat.DOCUMENTS_TREE_URI }
            || DocumentFileCompat.isRootUri(uri)
            && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && storageType == StorageType.SD_CARD || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            && !DocumentFileCompat.isStorageUriPermissionGranted(context, storageId)
        ) {
            saveUriPermission(uri)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && storageType == StorageType.EXTERNAL
            || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && saveUriPermission(uri)
            || folder.canModify(context) && (uri.isDocumentsDocument || !uri.isExternalStorageDocument)
            || DocumentFileCompat.isStorageUriPermissionGranted(context, storageId)
        ) {
            folderPickerCallback?.onFolderSelected(requestCode, folder)
        } else {
            folderPickerCallback?.onStorageAccessDenied(requestCode, folder, storageType, storageId)
        }
    }

    private fun saveUriPermission(root: Uri) = try {
        val writeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(root, writeFlags)
        // корень зла:
        //SimpleStorage.cleanupRedundantUriPermissions(context.applicationContext)
        true
    } catch (e: SecurityException) {
        false
    }

    private fun addInitialPathToIntent(intent: Intent, initialPath: FileFullPath?) {
        if (Build.VERSION.SDK_INT >= 26) {
            initialPath?.toDocumentUri(context)?.let { intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }
    }

    private val externalStorageRootAccessIntent: Intent
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            sm.primaryStorageVolume.createOpenDocumentTreeIntent()
        } else {
            SimpleStorage.getDefaultExternalStorageIntent(context)
        }

    interface FolderPickerCallback {

        fun onCanceledByUser(requestCode: Int) {
            // default implementation
        }

        fun onActivityHandlerNotFound(requestCode: Int, intent: Intent) {
            // default implementation
        }

        fun onStoragePermissionDenied(requestCode: Int)

        fun onStorageAccessDenied(requestCode: Int, folder: DocumentFile?, storageType: StorageType, storageId: String)

        fun onFolderSelected(requestCode: Int, folder: DocumentFile)
    }

}
package com.gee12.mytetroid.domain.provider

import android.net.Uri
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.makeFolderPath
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidRecord
import java.io.File

interface IRecordPathProvider {
    fun getUriToRecordFolder(record: TetroidRecord): Uri
    fun getPathToRecordFolder(record: TetroidRecord): FilePath.Folder
    fun getRelativePathToRecordFolder(record: TetroidRecord): String
    fun getRelativePathToRecordHtml(record: TetroidRecord): String
    fun getPathToRecordFolderInTrash(record: TetroidRecord): FilePath
    fun getPathToFileInRecordFolder(record: TetroidRecord, fileName: String): String
    fun getRelativePathToRecordAttach(attach: TetroidFile): String
    fun getRelativePathToFileInRecordFolder(record: TetroidRecord, fileName: String): String
}

class RecordPathProvider(
    private val storagePathProvider: IStoragePathProvider,
) : IRecordPathProvider {

    /**
     * Получение пути к каталогу записи в виде Uri, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     */
    override fun getUriToRecordFolder(record: TetroidRecord): Uri {
        val storageUri = if (record.isTemp) {
            storagePathProvider.getUriToStorageTrashFolder()
        } else {
            storagePathProvider.getUriToBaseFolder()
        }
        val file = File(makeFolderPath(storageUri.toString(), record.dirName))
        return Uri.fromFile(file)
    }

    /**
     * Получение пути к каталогу записи, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     */
    override fun getPathToRecordFolder(record: TetroidRecord): FilePath.Folder {
        val storagePath = if (record.isTemp) {
            storagePathProvider.getPathToStorageTrashFolder().fullPath
        } else {
            storagePathProvider.getPathToBaseFolder().fullPath
        }
        return FilePath.Folder(storagePath, record.dirName)
    }

    override fun getRelativePathToRecordFolder(record: TetroidRecord): String {
        return makeFolderPath(Constants.BASE_DIR_NAME, record.dirName)
    }

    override fun getRelativePathToRecordHtml(record: TetroidRecord): String {
        return getRelativePathToFileInRecordFolder(record, record.fileName)
    }

    override fun getPathToRecordFolderInTrash(record: TetroidRecord): FilePath {
        return FilePath.Folder(storagePathProvider.getPathToStorageTrashFolder().fullPath, record.dirName)
    }

    override fun getPathToFileInRecordFolder(record: TetroidRecord, fileName: String): String {
        return makePath(getPathToRecordFolder(record).fullPath, fileName)
    }

    override fun getRelativePathToRecordAttach(attach: TetroidFile): String {
        val record = attach.record
        val fileDisplayName = attach.name
        val ext = fileDisplayName.getExtensionWithoutComma()
        val fileIdName = attach.id.withExtension(ext)

        return getRelativePathToFileInRecordFolder(record, fileIdName)
    }

    override fun getRelativePathToFileInRecordFolder(record: TetroidRecord, fileName: String): String {
        return makePath(Constants.BASE_DIR_NAME, record.dirName, fileName)
    }

}
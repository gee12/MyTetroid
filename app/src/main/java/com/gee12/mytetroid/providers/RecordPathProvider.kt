package com.gee12.mytetroid.providers

import com.gee12.mytetroid.common.extensions.makeFolderPath
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.model.TetroidRecord

interface IRecordPathProvider {
    fun getUriToRecordFolder(record: TetroidRecord): String
    fun getPathToRecordFolder(record: TetroidRecord): String
    fun getPathToRecordFolderInTrash(record: TetroidRecord): String
    fun getPathToFileInRecordFolder(record: TetroidRecord, fileName: String): String
}

class RecordPathProvider(
    private val storagePathProvider: IStoragePathProvider,
) : IRecordPathProvider {

    /**
     * Получение пути к каталогу записи в виде Uri, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     */
    override fun getUriToRecordFolder(record: TetroidRecord): String {
        val storageUri = if (record.isTemp) {
            storagePathProvider.getUriToStorageTrashFolder()
        } else {
            storagePathProvider.getUriToStorageBaseFolder()
        }
        return makeFolderPath(storageUri.toString(), record.dirName)
    }

    /**
     * Получение пути к каталогу записи, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     */
    override fun getPathToRecordFolder(record: TetroidRecord): String {
        val storagePath = if (record.isTemp) {
            storagePathProvider.getPathToStorageTrashFolder()
        } else {
            storagePathProvider.getPathToStorageBaseFolder()
        }
        return makePath(storagePath, record.dirName)
    }

    override fun getPathToRecordFolderInTrash(record: TetroidRecord): String {
        return makePath(storagePathProvider.getPathToStorageTrashFolder(), record.dirName)
    }

    override fun getPathToFileInRecordFolder(record: TetroidRecord, fileName: String): String {
        return makePath(getPathToRecordFolder(record), fileName)
    }

}
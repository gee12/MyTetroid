package com.gee12.mytetroid.helpers

import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.TetroidRecord

interface IRecordPathHelper {
    fun getUriToRecordFolder(record: TetroidRecord): String
    fun getPathToRecordFolder(record: TetroidRecord): String
    fun getPathToRecordFolderInTrash(record: TetroidRecord): String
    fun getPathToFileInRecordFolder(record: TetroidRecord, fileName: String): String
}

class RecordPathHelper(
    private val storagePathHelper: IStoragePathHelper,
) : IRecordPathHelper {

    /**
     * Получение пути к каталогу записи в виде Uri, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     */
    override fun getUriToRecordFolder(record: TetroidRecord): String {
        val storageUri = if (record.isTemp) storagePathHelper.getUriToStorageTrashFolder() else storagePathHelper.getUriToStorageBaseFolder()
        return "$storageUri${Constants.SEPAR}${record.dirName}${Constants.SEPAR}"
    }

    /**
     * Получение пути к каталогу записи, с учетом того, что это может быть временная запись.
     * Запись может находиться в хранилище в каталоге base/ или в каталоге корзины.
     */
    override fun getPathToRecordFolder(record: TetroidRecord): String {
        val storagePath = if (record.isTemp) storagePathHelper.getPathToStorageTrashFolder() else storagePathHelper.getPathToStorageBaseFolder()
        return "$storagePath${Constants.SEPAR}${record.dirName}"
    }

    override fun getPathToRecordFolderInTrash(record: TetroidRecord): String {
        return "${storagePathHelper.getPathToStorageTrashFolder()}${Constants.SEPAR}${record.dirName}"
    }

    override fun getPathToFileInRecordFolder(record: TetroidRecord, fileName: String): String {
        return "${getPathToRecordFolder(record)}${Constants.SEPAR}$fileName"
    }

}
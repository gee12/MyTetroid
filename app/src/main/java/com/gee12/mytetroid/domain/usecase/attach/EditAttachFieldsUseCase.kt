package com.gee12.mytetroid.domain.usecase.attach

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.extensions.makePathToFile
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.domain.usecase.record.CheckRecordFolderUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import java.io.File

/**
 * Изменение свойств прикрепленного файла.
 * Проверка существования каталога записи и файла происходит только
 * если у имени файла было изменено расширение.
 */
class EditAttachFieldsUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val recordPathProvider: IRecordPathProvider,
    private val cryptManager: IStorageCryptManager,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, EditAttachFieldsUseCase.Params>() {

    data class Params(
        val attach: TetroidFile,
        val name: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val attach = params.attach
        val name = params.name

        if (name.isEmpty()) {
            return Failure.Attach.NameIsEmpty.toLeft()
        }
        logger.logOperStart(LogObj.FILE_FIELDS, LogOper.CHANGE, attach)
        val record = attach.record
        // TODO: уйдет когда объекты переведу на kotlin
//        if (record == null) {
//            logger.logError(resourcesProvider.getString(R.string.log_file_record_is_null))
//            return 0
//        }
        // сравниваем расширения
        val ext = FileUtils.getExtensionWithComma(attach.name)
        val newExt = FileUtils.getExtensionWithComma(name)
        val isExtChanged = !Utils.isEquals(ext, newExt, true)
        var folderPath: String? = null
        var filePath: String? = null
        var srcFile: File? = null
        if (isExtChanged) {
            // проверяем существование каталога записи
            folderPath = recordPathProvider.getPathToRecordFolder(record)
            checkRecordFolderUseCase.run(
                CheckRecordFolderUseCase.Params(
                    folderPath = folderPath,
                    isCreate = false,
                )
            ).onFailure {
                return it.toLeft()
            }
            // проверяем существование самого файла
            val fileIdName = attach.id + ext
            filePath = makePath(folderPath, fileIdName)
            srcFile = File(filePath)
            if (!srcFile.exists()) {
//                logger.logError(resourcesProvider.getString(R.string.error_file_is_missing_mask) + filePath)
                return Failure.File.NotExist(path = srcFile.path).toLeft()
            }
        }
        val oldName = attach.getName(true)
        // обновляем поля
        val isCrypted = attach.isCrypted
        attach.name = encryptFieldIfNeed(name, isCrypted)
        if (isCrypted) {
            attach.setDecryptedName(name)
        }

        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                // меняем расширение, если изменилось
                if (isExtChanged) {
                    val newFileIdName = attach.id + newExt
                    val destFile = makePathToFile(folderPath.orEmpty(), newFileIdName)
                    val fromTo = resourcesProvider.getStringFromTo(filePath.orEmpty(), newFileIdName)
                    if (srcFile!!.renameTo(destFile)) {
                        logger.logOperRes(LogObj.FILE, LogOper.RENAME, fromTo, false)
                    } else {
                        logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, false)
                        return Failure.File.RenameTo(filePath = srcFile.path, newName = destFile.path).toLeft()
                    }
                }
                None.toRight()
            }.onFailure {
                logger.logOperCancel(LogObj.FILE_FIELDS, LogOper.CHANGE)
                // возвращаем изменения
                attach.name = oldName
                if (isCrypted) {
                    attach.setDecryptedName(cryptManager.decryptTextBase64(oldName))
                }
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

}
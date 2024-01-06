package com.gee12.mytetroid.domain.usecase.attach

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.changeName
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.domain.usecase.record.GetRecordFolderUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.model.FilePath

/**
 * Изменение свойств прикрепленного файла.
 * Проверка существования каталога записи и файла происходит только
 * если у имени файла было изменено расширение.
 */
class EditAttachFieldsUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val cryptManager: IStorageCryptManager,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
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
        val oldExtension = attach.name.getExtensionWithoutComma()
        val newExtension = name.getExtensionWithoutComma()
        val isExtensionChanged = !Utils.isEquals(oldExtension, newExtension, true)
        var srcFilePath: FilePath? = null
        var srcFile: DocumentFile? = null

        if (isExtensionChanged) {
            val recordFolder = getRecordFolderUseCase.run(
                GetRecordFolderUseCase.Params(
                    record = record,
                    createIfNeed = false,
                    inTrash = false,
                )
            ).foldResult(
                onLeft = {
                    return it.toLeft()
                },
                onRight = { it }
            )
            val folderPath = recordFolder.getAbsolutePath(context)

            // проверяем существование самого файла
            val fileIdName = attach.id.withExtension(oldExtension)
            srcFilePath = FilePath.File(folderPath, fileIdName)
            srcFile = recordFolder.child(
                context = context,
                path = fileIdName,
                requiresWriteAccess = true,
            )

            if (srcFile == null || !srcFile.exists()) {
                return Failure.File.NotExist(srcFilePath).toLeft()
            }
        }
        val oldName = attach.getName(true)
        // обновляем поля
        val isEncrypted = attach.isCrypted
        attach.name = encryptFieldIfNeed(name, isEncrypted)
        if (isEncrypted) {
            attach.setDecryptedName(name)
        }

        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                // меняем расширение, если изменилось
                if (isExtensionChanged && srcFile != null && srcFilePath != null) {
                    val newFileIdName = attach.id.withExtension(newExtension)
                    val fromTo = resourcesProvider.getStringFromTo(srcFilePath.fullPath, newFileIdName)
                    val fileWithNewName = srcFile.changeName(
                        context = context,
                        newBaseName = attach.id,
                        newExtension = newExtension,
                    )
                    if (fileWithNewName != null) {
                        logger.logOperRes(LogObj.FILE, LogOper.RENAME, fromTo, show = false)
                    } else {
                        logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, show = false)
                        return Failure.File.Rename(path = srcFilePath, newName = newFileIdName).toLeft()
                    }
                }
                None.toRight()
            }.onFailure {
                logger.logOperCancel(LogObj.FILE_FIELDS, LogOper.CHANGE)
                // возвращаем изменения
                attach.name = oldName
                if (isEncrypted) {
                    attach.setDecryptedName(cryptManager.decryptTextBase64(oldName))
                }
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

}
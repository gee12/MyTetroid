package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.common.extensions.getStringTo
import com.gee12.mytetroid.domain.interactor.StorageTreeObserver
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.file.MoveFileOrFolderUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FileName
import com.gee12.mytetroid.model.FilePath

/**
 * Сохранение структуры хранилища в файл mytetra.xml.
 */
class SaveStorageUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storagePathProvider: IStoragePathProvider,
    private val storageProvider: IStorageProvider,
    private val dataNameProvider: IDataNameProvider,
    private val storageTreeInteractor: StorageTreeObserver,
    private val moveFileUseCase: MoveFileOrFolderUseCase,
    private val getStorageTrashFolderUseCase: GetStorageTrashFolderUseCase,
) : UseCase<UseCase.None, SaveStorageUseCase.Params>() {

    object Params

    suspend fun run(): Either<Failure, None> {
        return run(Params)
    }

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = storageProvider.storage
        val storageDataProcessor = storageProvider.dataProcessor
        val xmlFileName = Constants.MYTETRA_XML_FILE_NAME
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val xmlFilePath = FilePath.File(storageFolderPath, xmlFileName)

        logger.logDebug(resourcesProvider.getString(R.string.log_saving_mytetra_xml))

        return try {
            // 1 - сохраняем дерево во временный файл mytetra.xml_tmp
            val temporaryFileName = "${xmlFileName}_tmp"
            val temporaryXmlFile = storageFolder?.makeFile(
                context = context,
                name = temporaryFileName,
                mimeType = MimeType.TEXT,
                mode = CreateMode.REPLACE,
            ) ?: return Failure.File.Get(FilePath.File(storageFolderPath, temporaryFileName)).toLeft()
            val temporaryXmlFilePath = FilePath.FileFull(temporaryXmlFile.getAbsolutePath(context))

            val isSavedToTemporary = temporaryXmlFile.openOutputStream(context, append = false)?.use {
                storageDataProcessor.save(it)
            } ?: false

            if (isSavedToTemporary) {
                // TODO: ...
                onBeforeStorageTreeSave()

                // 2 - перемещаем старую версию файла mytetra.xml в корзину
                // Файла может и не быть, если это первоначальное создание файлов хранилища
                val xmlFile = storageFolder.child(
                    context = context,
                    path = xmlFileName,
                    requiresWriteAccess = true,
                )

                if (xmlFile != null) {
                    getStorageTrashFolderUseCase.run(
                        GetStorageTrashFolderUseCase.Params(
                            storage = storage!!,
                            createIfNotExist = true,
                        )
                    ).flatMap { storageTrashFolder ->
                        val fileNameInTrash = "${dataNameProvider.createDateTimePrefix()}_$xmlFileName"

                        moveFileUseCase.run(
                            MoveFileOrFolderUseCase.Params(
                                srcFileOrFolder = xmlFile,
                                destFolder = storageTrashFolder,
                                newName = fileNameInTrash,
                            )
                        )
                    }.onFailure {
                        // 3 - если не удалось переместить в корзину, удаляем
                        if (xmlFile.exists() && !xmlFile.delete()) {
                            logger.logOperError(LogObj.FILE, LogOper.DELETE, xmlFilePath.fullPath, false, show = false)
                            return Failure.File.Delete(xmlFilePath).toLeft()
                        }
                    }
                }

                // 4 - переименовываем актуальную версию файла из временного имени mytetra.xml_tmp в mytetra.xml
                val isRenamed = temporaryXmlFile.changeName(context, newBaseName = xmlFileName, newExtension = "") != null
                if (!isRenamed) {
                    val fromTo = resourcesProvider.getStringFromTo(temporaryXmlFilePath.fullPath, xmlFilePath.fullPath)
                    logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, show = false)
                    return Failure.File.Rename(
                        path = temporaryXmlFilePath,
                        newName = xmlFilePath.fullPath,
                    ).toLeft()
                }

                // TODO: ...
                onStorageTreeSaved()

                None.toRight()
            } else {
                // не удалось записать дерево во временный файл mytetra.xml_tmp
                val to = resourcesProvider.getStringTo(temporaryXmlFilePath.fullPath)
                logger.logOperError(LogObj.FILE, LogOper.SAVE, to, false, show = false)

                // пытаемся сохранить напрямую
                val xmlFile = storageFolder.child(
                    context = context,
                    path = xmlFileName,
                    requiresWriteAccess = true,
                ) ?: return Failure.File.Get(xmlFilePath).toLeft()

                val isSavedToDestination = xmlFile.openOutputStream(context, append = false)?.use {
                    storageDataProcessor.save(it)
                } ?: false
                if (isSavedToDestination) {
                    None.toRight()
                } else {
                    Failure.File.Write(xmlFilePath).toLeft()
                }
            }
        } catch (ex: Exception) {
            logger.logError(ex, true)
            Failure.File.Write(xmlFilePath).toLeft()
        }
    }

    private fun onBeforeStorageTreeSave() {
        storageTreeInteractor.stopObserver()
    }

    private suspend fun onStorageTreeSaved() {
        storageTreeInteractor.startObserver(
            storagePath = storagePathProvider.getPathToMyTetraXml()
        )
    }

}
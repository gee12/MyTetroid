package com.gee12.mytetroid.usecase.storage

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.helpers.IStoragePathProvider
import com.gee12.mytetroid.providers.IDataNameProvider
import com.gee12.mytetroid.interactors.StorageTreeInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.usecase.file.MoveFileUseCase
import java.io.File
import java.io.FileOutputStream

/**
 * Сохранение структуры хранилища в файл mytetra.xml.
 */
class SaveStorageUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storagePathProvider: IStoragePathProvider,
    private val storageDataProcessor: IStorageDataProcessor,
    private val dataNameProvider: IDataNameProvider,
    private val storageTreeInteractor: StorageTreeInteractor,
    private val moveFileUseCase: MoveFileUseCase,
) : UseCase<UseCase.None, SaveStorageUseCase.Params>() {

    object Params
//    data class Params(
//        val storageProvider: IStorageProvider,
//        val storagePathProvider: IStoragePathHelper,
//        val storageDataProcessor: IStorageDataProcessor,
//    )

    suspend fun run(): Either<Failure, None> {
        return run(Params)
    }

//    suspend fun run(storageProvider: IStorageProvider): Either<Failure, None> {
//        return run(
//            Params(storageProvider)
//        )
//    }

    override suspend fun run(params: Params): Either<Failure, None> {
//        val storagePathProvider = params.storageProvider.pathProvider

        val destPath = storagePathProvider.getPathToMyTetraXml()
        val tempPath = destPath + "_tmp"
        logger.logDebug(resourcesProvider.getString(R.string.log_saving_mytetra_xml))
        return try {
            val saveResult = run {
                val fos = FileOutputStream(tempPath, false)
                storageDataProcessor.save(fos)
            }
            if (saveResult) {
                // TODO: ...
                onBeforeStorageTreeSave()

                val to = File(destPath)
                // перемещаем старую версию файла mytetra.xml в корзину
                val nameInTrash = "${dataNameProvider.createDateTimePrefix()}_${Constants.MYTETRA_XML_FILE_NAME}"
                moveFileUseCase.run(
                    MoveFileUseCase.Params(
                        srcFullFileName = destPath,
                        destPath = storagePathProvider.getPathToStorageTrashFolder(),
                        newFileName = nameInTrash,
                    )
                ).onFailure {
                    // если не удалось переместить в корзину, удаляем
                    if (to.exists() && !to.delete()) {
                        logger.logOperError(LogObj.FILE, LogOper.DELETE, destPath, false, false)
                        return Failure.File.Delete(filePath = to.path).toLeft()
                    }
                }
//                if (dataNameProvider.moveFile(destPath, storagePathProvider.getPathToStorageTrashFolder(), nameInTrash) <= 0) {
//                    // если не удалось переместить в корзину, удаляем
//                    if (to.exists() && !to.delete()) {
//                        logger.logOperError(LogObj.FILE, LogOper.DELETE, destPath, false, false)
//                        return Failure.Storage.Save.RemoveOldXmlFile(
//                            pathToFile = to.path
//                        ).toLeft()
//                    }
//                }
                // задаем правильное имя актуальной версии файла mytetra.xml
                val from = File(tempPath)
                if (!from.renameTo(to)) {
                    val fromTo = resourcesProvider.getStringFromTo(tempPath, destPath)
                    logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, false)
                    return Failure.Storage.Save.RenameXmlFileFromTempName(
                        from = from.path,
                        to = to.path,
                    ).toLeft()
                }

                // TODO: ...
                onStorageTreeSaved(/*storagePathProvider*/)
                None.toRight()
            } else {
                Failure.Storage.Save.SaveXmlFile(pathToFile = destPath).toLeft()
            }
        } catch (ex: Exception) {
            logger.logError(ex, true)
            Failure.Storage.Save.SaveXmlFile(pathToFile = destPath, ex).toLeft()
        }
    }

    private fun onBeforeStorageTreeSave() {
        storageTreeInteractor.stopStorageTreeObserver()
    }

    private suspend fun onStorageTreeSaved(/*storagePathProvider: IStoragePathProvider*/) {
        storageTreeInteractor.startStorageTreeObserver(
            storagePath = storagePathProvider.getPathToMyTetraXml()
        )
    }

}
package com.gee12.mytetroid.usecase.storage

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getStringFromTo
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.helpers.IStoragePathHelper
import com.gee12.mytetroid.interactors.DataInteractor
import com.gee12.mytetroid.interactors.StorageTreeInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Сохранение структуры хранилища в файл mytetra.xml.
 */
class SaveStorageUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val storagePathHelper: IStoragePathHelper,
    private val storageDataProcessor: IStorageDataProcessor,
    private val dataInteractor: DataInteractor,
    private val storageTreeInteractor: StorageTreeInteractor,
) : UseCase<Boolean, SaveStorageUseCase.Params>() {

    object Params

    suspend fun run(): Either<Failure, Boolean> {
        return run(Params)
    }

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        val destPath = storagePathHelper.getPathToMyTetraXml()
        val tempPath = destPath + "_tmp"
        logger.logDebug(resourcesProvider.getString(R.string.log_saving_mytetra_xml))
        return try {
            val saveResult = withContext(Dispatchers.IO) {
                val fos = FileOutputStream(tempPath, false)
                storageDataProcessor.save(fos)
            }
            if (saveResult) {
                // TODO: ...
                onBeforeStorageTreeSave()

                val to = File(destPath)
                // перемещаем старую версию файла mytetra.xml в корзину
                val nameInTrash = dataInteractor.createDateTimePrefix() + "_" + Constants.MYTETRA_XML_FILE_NAME
                if (dataInteractor.moveFile(destPath, storagePathHelper.getPathToStorageTrashFolder(), nameInTrash) <= 0) {
                    // если не удалось переместить в корзину, удаляем
                    if (to.exists() && !to.delete()) {
                        //LogManager.log(context.getString(R.string.log_failed_delete_file) + destPath, LogManager.Types.ERROR);
                        logger.logOperError(LogObj.FILE, LogOper.DELETE, destPath, false, false)
                        return Failure.Storage.Save.RemoveOldXmlFile.toLeft()
                    }
                }
                // задаем правильное имя актуальной версии файла mytetra.xml
                val from = File(tempPath)
                if (!from.renameTo(to)) {
                    val fromTo = resourcesProvider.getStringFromTo(tempPath, destPath)
                    //LogManager.log(String.format(context.getString(R.string.log_rename_file_error_mask), tempPath, destPath), LogManager.Types.ERROR);
                    logger.logOperError(LogObj.FILE, LogOper.RENAME, fromTo, false, false)
                    return Failure.Storage.Save.RenameXmlFileFromTempName.toLeft()
                }

                // TODO: ...
                onStorageTreeSaved()
                true.toRight()
            } else {
                Failure.Storage.Save.SaveXmlFile(storagePath = destPath).toLeft()
            }
        } catch (ex: Exception) {
            logger.logError(ex, true)
            Failure.Storage.Save.SaveXmlFile(storagePath = destPath, ex).toLeft()
        }
    }

    private fun onBeforeStorageTreeSave() {
        storageTreeInteractor.stopStorageTreeObserver()
    }

    private suspend fun onStorageTreeSaved() {
        storageTreeInteractor.startStorageTreeObserver(
            storagePath = storagePathHelper.getPathToMyTetraXml()
        )
    }

}
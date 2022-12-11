package com.gee12.mytetroid.usecase.record

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import java.io.File

/**
 * Проверка существования каталога записи и его создание при необходимости.
 */
class CheckRecordFolderUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) : UseCase<CheckRecordFolderUseCase.Result, CheckRecordFolderUseCase.Params>() {

    data class Params(
        val folderPath: String,
        val isCreate: Boolean,
        val showMessage: Boolean = false,
    )

    sealed class Result {
        object Created : Result()
        object Success : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val folderPath = params.folderPath
        val showMessage = params.showMessage

        val folder = File(folderPath)
        return try {
            if (!folder.exists()) {
                return if (params.isCreate) {
                    logger.logWarning(resourcesProvider.getString(R.string.log_create_record_dir, folderPath), showMessage)
                    if (folder.mkdirs()) {
                        logger.logOperRes(LogObj.RECORD_DIR, LogOper.CREATE, "", showMessage)
                        Result.Created.toRight()
                    } else {
                        Failure.Record.CheckFolder.CreateFolderError(folderPath).toLeft()
                    }
                } else {
                    Failure.Record.CheckFolder.FolderNotExist(folderPath).toLeft()
                }
            } else {
                Result.Success.toRight()
            }
        } catch (ex: Exception) {
            Failure.Record.CheckFolder.UnknownError(folderPath, ex).toLeft()
        }
    }

}
package com.gee12.mytetroid.domain.usecase.record.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.ImageUtils
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.record.CheckRecordFolderUseCase
import java.io.File
import java.lang.Exception

/**
 * Сохранение файла изображения в каталог записи.
 * @param deleteSrcFile Нужно ли удалить исходный файл после сохранения файла назначения
 */
class SaveImageFromUriUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
) : UseCase<TetroidImage, SaveImageFromUriUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val srcUri: Uri,
        val deleteSrcFile: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidImage> {
        val record = params.record
        val srcUri = params.srcUri
        val deleteSrcFile = params.deleteSrcFile

        val srcPath = srcUri.path
        logger.logDebug(resourcesProvider.getString(R.string.log_start_image_file_saving_mask, srcPath.orEmpty(), record.id))

        // генерируем уникальное имя файла
        val nameId = dataNameProvider.createUniqueImageName()
        val image = TetroidImage(nameId, record)

        // проверяем существование каталога записи
        val folderPath = recordPathProvider.getPathToRecordFolder(record)
        checkRecordFolderUseCase.run(
            CheckRecordFolderUseCase.Params(
                folderPath = folderPath,
                isCreate = true,
            )
        ).onFailure {
            return it.toLeft()
        }
        val destFullName = recordPathProvider.getPathToFileInRecordFolder(record, nameId)
        logger.logDebug(resourcesProvider.getString(R.string.log_start_image_file_converting_mask, destFullName))
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            ImageUtils.convertImage(context, srcUri, destFullName, Bitmap.CompressFormat.PNG, 100)

            val destFile = File(destFullName)
            if (destFile.exists()) {
                if (deleteSrcFile) {
                    logger.logDebug(resourcesProvider.getString(R.string.log_start_image_file_deleting_mask, srcUri))
                    val srcFile = File(srcPath)
                    // удаляем исходный файл за ненадобностью
                    if (!srcFile.delete()) {
                        logger.logWarning(resourcesProvider.getString(R.string.log_error_deleting_src_image_file) + srcPath, true)
                    }
                }
            } else {
//                logger.logError(resourcesProvider.getString(R.string.error_save_image_file_mask))
                return Failure.Image.SaveFile(fileName = destFile.path).toLeft()
            }
        } catch (ex: Exception) {
//            logger.logError(resourcesProvider.getString(R.string.error_save_image_file_mask), ex)
            return Failure.Image.SaveFile(fileName = destFullName, ex).toLeft()
        }

        return image.toRight()
    }

}
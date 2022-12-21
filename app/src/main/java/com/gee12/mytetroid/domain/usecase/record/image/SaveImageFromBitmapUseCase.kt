package com.gee12.mytetroid.domain.usecase.record.image

import android.graphics.Bitmap
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.ImageUtils
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.record.CheckRecordFolderUseCase
import java.io.File
import java.lang.Exception

/**
 * Сохранение изображения в каталог записи.
 */
class SaveImageFromBitmapUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
) : UseCase<TetroidImage, SaveImageFromBitmapUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val bitmap: Bitmap,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidImage> {
        val record = params.record
        val bitmap = params.bitmap

        logger.logOperRes(LogObj.IMAGE, LogOper.SAVE, record, false)

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

        val destFullName: String = recordPathProvider.getPathToFileInRecordFolder(record, nameId)
        logger.logDebug(resourcesProvider.getString(R.string.log_start_image_file_converting_mask, destFullName))
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            ImageUtils.saveBitmap(bitmap, destFullName, Bitmap.CompressFormat.PNG, 100)

            val destFile = File(destFullName)
            if (!destFile.exists()) {
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
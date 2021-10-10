package com.gee12.mytetroid.interactors

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Exception

class ImagesInteractor(
    private val logger: ITetroidLogger,
    private val dataInteractor: DataInteractor,
    private val recordsInteractor: RecordsInteractor
) {

    /**
     * Сохранение файла изображения в каталог записи.
     * @param context
     * @param record
     * @param srcUri
     * @param deleteSrcFile Нужно ли удалить исходный файл после сохранения файла назначения
     * @return
     */
    suspend fun saveImage(context: Context, record: TetroidRecord?, srcUri: Uri?, deleteSrcFile: Boolean): TetroidImage? {
        if (record == null || srcUri == null) {
            logger.logEmptyParams("ImagesInteractor.saveImage()")
            return null
        }
        val srcPath = srcUri.path
        logger.logDebug(context.getString(R.string.log_start_image_file_saving_mask).format(srcPath, record.id))

        // генерируем уникальное имя файла
        val nameId: String = dataInteractor.createUniqueImageName()
        val image = TetroidImage(nameId, record)

        // проверяем существование каталога записи
        val dirPath: String = recordsInteractor.getPathToRecordFolder(record)
        val dirRes: Int = recordsInteractor.checkRecordFolder(context, dirPath, true)
        if (dirRes <= 0) {
            return null
        }
        val destFullName: String = recordsInteractor.getPathToFileInRecordFolder(record, nameId)
        logger.logDebug(context.getString(R.string.log_start_image_file_converting_mask).format(destFullName))
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            withContext(Dispatchers.IO) {
                ImageUtils.convertImage(context, srcUri, destFullName, Bitmap.CompressFormat.PNG, 100)
            }
            val destFile = File(destFullName)
            if (destFile.exists()) {
                if (deleteSrcFile) {
                    logger.logDebug(context.getString(R.string.log_start_image_file_deleting_mask).format(srcUri))
                    val srcFile = File(srcPath)
                    // удаляем исходный файл за ненадобностью
                    if (!srcFile.delete()) {
                        logger.logWarning(context.getString(R.string.log_error_deleting_src_image_file) + srcPath, true)
                    }
                }
            } else {
                logger.logError(context.getString(R.string.log_error_image_file_saving))
                return null
            }
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.log_error_image_file_saving), ex)
            return null
        }
        return image
    }

    /**
     * Сохранение изображения в каталог записи.
     * @param context
     * @param record
     * @param bitmap
     * @return
     */
    suspend fun saveImage(context: Context, record: TetroidRecord?, bitmap: Bitmap?): TetroidImage? {
        if (record == null || bitmap == null) {
            logger.logEmptyParams("ImagesInteractor.saveImage()")
            return null
        }
        logger.logOperRes(LogObj.IMAGE, LogOper.SAVE, record, false)

        // генерируем уникальное имя файла
        val nameId: String = dataInteractor.createUniqueImageName()
        val image = TetroidImage(nameId, record)

        // проверяем существование каталога записи
        val dirPath: String = recordsInteractor.getPathToRecordFolder(record)
        val dirRes: Int = recordsInteractor.checkRecordFolder(context, dirPath, true)
        if (dirRes <= 0) {
            return null
        }
        val destFullName: String = recordsInteractor.getPathToFileInRecordFolder(record, nameId)
        logger.logDebug(context.getString(R.string.log_start_image_file_converting_mask).format(destFullName))
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            withContext(Dispatchers.IO) {
                ImageUtils.saveBitmap(bitmap, destFullName, Bitmap.CompressFormat.PNG, 100)
            }
            val destFile = File(destFullName)
            if (!destFile.exists()) {
                logger.logError(context.getString(R.string.log_error_image_file_saving))
                return null
            }
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.log_error_image_file_saving), ex)
            return null
        }
        return image
    }

}
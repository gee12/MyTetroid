package com.gee12.mytetroid.interactors

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.logs.TetroidLog
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.utils.ImageUtils
import java.io.File
import java.lang.Exception

class ImagesInteractor(
    val dataInteractor: DataInteractor,
    val recordsInteractor: RecordsInteractor
) {

    /**
     * Сохранение файла изображения в каталог записи.
     * @param context
     * @param record
     * @param srcUri
     * @param deleteSrcFile Нужно ли удалить исходный файл после сохранения файла назначения
     * @return
     */
    fun saveImage(context: Context, record: TetroidRecord?, srcUri: Uri?, deleteSrcFile: Boolean): TetroidImage? {
        if (record == null || srcUri == null) {
            LogManager.emptyParams(context, "ImagesInteractor.saveImage()")
            return null
        }
        val srcPath = srcUri.path
        LogManager.log(
            context, String.format(
                context.getString(R.string.log_start_image_file_saving_mask),
                srcPath, record.id
            ), ILogger.Types.DEBUG
        )

        // генерируем уникальное имя файла
        val nameId: String = dataInteractor.createUniqueImageName()
        val image = TetroidImage(nameId, record)

        // проверяем существование каталога записи
        val dirPath: String = recordsInteractor.getPathToRecordFolder(context, record)
        val dirRes: Int = recordsInteractor.checkRecordFolder(context, dirPath, true)
        if (dirRes <= 0) {
            return null
        }
        val destFullName: String = recordsInteractor.getPathToFileInRecordFolder(context, record, nameId)
        LogManager.log(
            context, String.format(
                context.getString(R.string.log_start_image_file_converting_mask),
                destFullName
            ), ILogger.Types.DEBUG
        )
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            ImageUtils.convertImage(context, srcUri, destFullName, Bitmap.CompressFormat.PNG, 100)
            val destFile = File(destFullName)
            if (destFile.exists()) {
                if (deleteSrcFile) {
                    LogManager.log(
                        context, String.format(
                            context.getString(R.string.log_start_image_file_deleting_mask),
                            srcUri
                        ), ILogger.Types.DEBUG
                    )
                    val srcFile = File(srcPath)
                    // удаляем исходный файл за ненадобностью
                    if (!srcFile.delete()) {
                        LogManager.log(
                            context,
                            context.getString(R.string.log_error_deleting_src_image_file) + srcPath,
                            ILogger.Types.WARNING,
                            Toast.LENGTH_LONG
                        )
                    }
                }
            } else {
                LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ILogger.Types.ERROR)
                return null
            }
        } catch (ex: Exception) {
            LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ex)
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
    fun saveImage(context: Context, record: TetroidRecord?, bitmap: Bitmap?): TetroidImage? {
        if (record == null || bitmap == null) {
            LogManager.emptyParams(context, "ImagesInteractor.saveImage()")
            return null
        }
        TetroidLog.logOperRes(context, TetroidLog.Objs.IMAGE, TetroidLog.Opers.SAVE, record, -1)

        // генерируем уникальное имя файла
        val nameId: String = dataInteractor.createUniqueImageName()
        val image = TetroidImage(nameId, record)

        // проверяем существование каталога записи
        val dirPath: String = recordsInteractor.getPathToRecordFolder(context, record)
        val dirRes: Int = recordsInteractor.checkRecordFolder(context, dirPath, true)
        if (dirRes <= 0) {
            return null
        }
        val destFullName: String = recordsInteractor.getPathToFileInRecordFolder(context, record, nameId)
        LogManager.log(
            context, String.format(
                context.getString(R.string.log_start_image_file_converting_mask),
                destFullName
            ), ILogger.Types.DEBUG
        )
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            ImageUtils.saveBitmap(bitmap, destFullName, Bitmap.CompressFormat.PNG, 100)
            val destFile = File(destFullName)
            if (!destFile.exists()) {
                LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ILogger.Types.ERROR)
                return null
            }
        } catch (ex: Exception) {
            LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ex)
            return null
        }
        return image
    }

}
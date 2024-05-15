package com.gee12.mytetroid.domain.usecase.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.openInputStream
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord

/**
 * Сохранение файла изображения в каталог записи.
 * @param deleteSrcImageFile Нужно ли удалить исходный файл после сохранения файла назначения
 */
class SaveImageFromUriUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val saveImageFromBitmapUseCase: SaveImageFromBitmapUseCase,
) : UseCase<TetroidImage, SaveImageFromUriUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val srcImageUri: Uri,
        val deleteSrcImageFile: Boolean,
        val format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        val quality: Int = 100,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidImage> {
        val record = params.record
        val srcUri = params.srcImageUri
        val deleteSrcFile = params.deleteSrcImageFile

        val srcFilePath = FilePath.FileFull(srcUri.path.orEmpty())
        logger.logDebug(resourcesProvider.getString(R.string.log_start_image_file_saving_mask, srcFilePath.fullPath, record.id))

        val srcFile = DocumentFileCompat.fromUri(context ,srcUri)
            ?: return Failure.File.Get(srcFilePath).toLeft()

        return loadBitmap(
            imageFile = srcFile,
            filePath = srcFilePath,
        ).flatMap { bitmap ->
            saveImageFromBitmapUseCase.run(
                SaveImageFromBitmapUseCase.Params(
                    record = record,
                    bitmap = bitmap,
                    format = params.format,
                    quality = params.quality,
                )
            )
        }.flatMap { image ->
            if (deleteSrcFile) {
                // удаляем исходный файл за ненадобностью
                if (!srcFile.delete()) {
                    logger.logOperRes(LogObj.IMAGE, LogOper.DELETE, add = srcFilePath.fullPath, show = false)
                } else {
                    logger.logOperError(LogObj.IMAGE, LogOper.DELETE, add = srcFilePath.fullPath, more = false, show = false)
                }
            }
            image.toRight()
        }
    }

    private fun loadBitmap(
        imageFile: DocumentFile,
        filePath: FilePath,
    ): Either<Failure, Bitmap> {
        return try {
            imageFile.openInputStream(context)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream).toRight()
            } ?: Failure.File.Read(filePath).toLeft()
        } catch (ex: Exception) {
            Failure.Image.LoadFromFile(filePath, ex).toLeft()
        }
    }

}
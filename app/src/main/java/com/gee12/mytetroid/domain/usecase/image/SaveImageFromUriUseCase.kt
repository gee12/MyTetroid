package com.gee12.mytetroid.domain.usecase.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.usecase.record.GetRecordFolderUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.enums.ImagesSaveMode
import com.gee12.mytetroid.model.obj.TetroidImage
import com.gee12.mytetroid.model.obj.TetroidRecord

/**
 * Сохранение файла изображения в каталог записи.
 * @param deleteSrcImageFile Нужно ли удалить исходный файл после сохранения файла назначения
 */
class SaveImageFromUriUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val settingsManager: CommonSettingsManager,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val saveImageFromBitmapUseCase: SaveImageFromBitmapUseCase,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
) : UseCase<TetroidImage, SaveImageFromUriUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val srcImageUri: Uri,
        val deleteSrcImageFile: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidImage> {
        val record = params.record
        val srcUri = params.srcImageUri
        val deleteSrcFile = params.deleteSrcImageFile

        val srcFilePath = FilePath.FileFull(srcUri.path.orEmpty())
        logger.logDebug(resourcesProvider.getString(R.string.log_start_image_file_saving_mask, srcFilePath.fullPath, record.id))

        val srcFile = DocumentFileCompat.fromUri(context, srcUri)
            ?: return Failure.File.Get(srcFilePath).toLeft()

        return loadBitmap(
            imageFile = srcFile,
            filePath = srcFilePath,
        ).flatMap { bitmap ->
            val format = when (settingsManager.getImagesSaveMode()) {
                ImagesSaveMode.AS_IS -> null
                ImagesSaveMode.CONVERT_TO_PNG -> Bitmap.CompressFormat.PNG
                ImagesSaveMode.CONVERT_TO_JPG -> Bitmap.CompressFormat.JPEG
            }

            if (format == null) {
                // генерируем уникальное имя файла
                val nameId = dataNameProvider.createUniqueImageName(extension = srcFile.extension)
                val image = TetroidImage(nameId, record)

                copyImageFile(
                    record = record,
                    srcFile = srcFile,
                    newFileName = nameId,
                ).map {
                    image.apply {
                        width = bitmap.width
                        height = bitmap.height
                    }
                }
            } else {
                saveImageFromBitmapUseCase.run(
                    SaveImageFromBitmapUseCase.Params(
                        record = record,
                        bitmap = bitmap,
                        format = format,
                        quality = settingsManager.getImagesSaveQuality(),
                    )
                )
            }
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

    private suspend fun copyImageFile(
        record: TetroidRecord,
        srcFile: DocumentFile,
        newFileName: String,
    ): Either<Failure, None> {
        logger.logOperRes(LogObj.IMAGE, LogOper.SAVE, record, false)

        val recordFolder = getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = record,
                createIfNeed = true,
                inTrash = record.isTemporary,
            )
        ).foldResult(
            onLeft = {
                return it.toLeft()
            },
            onRight = { it }
        )

        val srcFilePath = FilePath.FileFull(srcFile.getAbsolutePath(context).ifEmpty { srcFile.uri.toString() })
        val destFilePath = FilePath.File(recordFolder.getAbsolutePath(context), newFileName)

        // создание нового пустого файла
        val destFile = recordFolder.makeFile(
            context = context,
            name = newFileName,
            mimeType = srcFile.mimeType,
            mode = CreateMode.REPLACE,
        ) ?: return Failure.File.Create(srcFilePath).toLeft()

        return try {
            val copiedBytesCount = srcFile.openInputStream(context)?.use { inputStream ->
                destFile.openOutputStream(context, append = false)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                } ?: return Failure.File.Write(destFilePath).toLeft()
            } ?: return Failure.File.Read(srcFilePath).toLeft()
            if (copiedBytesCount > 0) {
                val to = resourcesProvider.getString(R.string.log_to_mask, destFilePath.fullPath)
                logger.logOperRes(LogObj.IMAGE, LogOper.COPY, to, show = false)
                None.toRight()
            } else {
                Failure.File.Copy(from = srcFilePath, to = destFilePath).toLeft()
            }
        } catch (ex: Exception) {
            Failure.File.Copy(from = srcFilePath, to = destFilePath, ex).toLeft()
        }
    }

}
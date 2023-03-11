package com.gee12.mytetroid.domain.usecase.image

import android.content.Context
import android.graphics.Bitmap
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidImage
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.record.GetRecordFolderUseCase
import com.gee12.mytetroid.model.FilePath
import java.lang.Exception

/**
 * Сохранение изображения в каталог записи.
 */
class SaveImageFromBitmapUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
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

        val recordFolder = getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = record,
                createIfNeed = true,
                inTrash = false,
            )
        ).foldResult(
            onLeft = {
                return it.toLeft()
            },
            onRight = { it }
        )
        val filePath = FilePath.File(recordFolder.getAbsolutePath(context), nameId)

        val imageFile = recordFolder.makeFile(
            context = context,
            name = nameId,
            mimeType = MimeType.IMAGE,
            mode = CreateMode.REPLACE,
        ) ?: return Failure.File.Get(filePath).toLeft()

        logger.logDebug(resourcesProvider.getString(R.string.log_start_image_file_converting_mask, filePath.fullPath))

        // конвертируем изображение в формат PNG и сохраняем в каталог записи
        return saveBitmap(
            bitmap = bitmap,
            file = imageFile,
            filePath = filePath,
            format = Bitmap.CompressFormat.PNG,
            quality = 100,
        ).flatMap {
            if (!imageFile.exists()) {
                return Failure.Image.SaveToFile(filePath).toLeft()
            } else {
                None.toRight()
            }
        }.flatMap {
            setImageDimensions(
                image = image,
                bitmap = bitmap,
            )
        }
    }

    @Throws(java.lang.Exception::class)
    private fun saveBitmap(
        bitmap: Bitmap,
        file: DocumentFile,
        filePath: FilePath,
        format: Bitmap.CompressFormat,
        quality: Int,
    ): Either<Failure, None> {
        return try {
            file.openOutputStream(context, append = false)?.use { outputStream ->
                bitmap.compress(format, quality, outputStream)
                outputStream.flush()

                None.toRight()
            } ?: Failure.File.Write(filePath).toLeft()
        } catch (ex: Exception) {
            return Failure.Image.SaveToFile(filePath, ex).toLeft()
        }
    }

    private suspend fun setImageDimensions(
        image: TetroidImage,
        bitmap: Bitmap,
//        file: DocumentFile,
    ): Either<Failure, TetroidImage> {
//        return file.openInputStream(context)?.use { inputStream ->
//            setImageDimensionsUseCase.run(
//                SetImageDimensionsUseCase.Params(
//                    image = image,
//                    inputStream = inputStream,
//                )
//            ).map { image }
//        } ?: Failure.File.Read(filePath).toLeft()
        return image.apply {
            width = bitmap.width
            height = bitmap.height
        }.toRight()
    }

}
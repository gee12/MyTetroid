package com.gee12.mytetroid.domain.usecase.image

import android.content.Context
import android.graphics.BitmapFactory
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openInputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.ImageDimension

class GetImageDimensionsUseCase(
    private val context: Context,
) : UseCase<ImageDimension, GetImageDimensionsUseCase.Params>() {

    data class Params(
        val imageFile: DocumentFile,
    )

    override suspend fun run(params: Params): Either<Failure, ImageDimension> {
        val imageFile = params.imageFile
        return getBitmapOptions(
            imageFile = imageFile,
            filePath = FilePath.FileFull(imageFile.getAbsolutePath(context)),
        ).map { options ->
            ImageDimension(
                width = options.outWidth,
                height = options.outHeight,
            )
        }
    }

    private fun getBitmapOptions(
        imageFile: DocumentFile,
        filePath: FilePath,
    ): Either<Failure, BitmapFactory.Options> {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            imageFile.openInputStream(context)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
                options.toRight()
            } ?: Failure.File.Read(filePath).toLeft()
        } catch (ex: Exception) {
            Failure.Image.LoadFromFile(filePath, ex).toLeft()
        }
    }

}
package com.gee12.mytetroid.domain.usecase.image

import android.net.Uri
import android.webkit.MimeTypeMap
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.domain.usecase.file.GetContentUriFromFileUseCase
import com.gee12.mytetroid.model.FilePath
import java.io.File
import java.net.URI


/**
 * Получение Uri и MIME-type файла изображения по пути.
 */
class PrepareImageForOpenUseCase(
    private val getContentUriFromFileUseCase: GetContentUriFromFileUseCase,
) : UseCase<PrepareImageForOpenUseCase.Result, PrepareImageForOpenUseCase.Params>() {

    data class Params(
        val fullFileName: String,
    )

    data class Result(
        val uri: Uri,
        val mimeType: String,
    )

    override suspend fun run(params: Params): Either<Failure, Result> {
        val fullFileName = params.fullFileName

        return getFile(fullFileName).flatMap { file ->
            getContentFileUri(file).flatMap { uri ->
                getMimeType(fullFileName).flatMap { mimeType ->
                    Result(
                        uri = uri,
                        mimeType = mimeType,
                    ).toRight()
                }
            }
        }
    }

    private fun getFile(fullFileName: String): Either<Failure, File> {
        return File(URI(fullFileName)).toRight()
    }

    private suspend fun getContentFileUri(file: File): Either<Failure, Uri> {
        return getContentUriFromFileUseCase.run(
            GetContentUriFromFileUseCase.Params(file)
        )
    }

    private fun getMimeType(fullFileName: String): Either<Failure, String> {
        val filePath = FilePath.FileFull(fullFileName)
        val ext = fullFileName.getExtensionWithoutComma()

        // определяем mimeType файла по расширению, если оно есть
        return if (ext.isNotEmpty()) {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            mimeType?.toRight() ?:
                Failure.File.UnknownMimeType(filePath).toLeft()
        } else {
            Failure.File.UnknownExtension(filePath).toLeft()
        }
    }

}
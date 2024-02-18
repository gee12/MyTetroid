package com.gee12.mytetroid.domain.usecase.file

import android.net.Uri
import android.webkit.MimeTypeMap
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.model.FilePath
import java.io.File


/**
 * Получение Uri и MIME-type файла.
 */
class PrepareFileForOpenUseCase(
    private val getContentUriFromFileUseCase: GetContentUriFromFileUseCase,
) : UseCase<PrepareFileForOpenUseCase.Result, PrepareFileForOpenUseCase.Params>() {

    data class Params(
        val file: File,
    )

    data class Result(
        val uri: Uri,
        val mimeType: String,
    )

    override suspend fun run(params: Params): Either<Failure, Result> {
        val file = params.file
        val fullFileName = file.absolutePath

        return getContentFileUri(file).flatMap { uri ->
            getMimeType(fullFileName).flatMap { mimeType ->
                Result(
                    uri = uri,
                    mimeType = mimeType,
                ).toRight()
            }
        }
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
package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.model.FilePath
import java.io.File

/**
 * Получение Uri формата "content://" из объекта File.
 *
 * Начиная с API 24, для предоставления доступа к файлам, который ассоциируется с приложением
 * (по сути, для открытия файла другими приложениями с помощью Intent),
 * нужно использовать механизм FileProvider.
 * Uri должен быть иметь scheme "content://"
 */
class GetContentUriFromFileUseCase(
    private val context: Context,
    private val appBuildInfoProvider: BuildInfoProvider,
) : UseCase<Uri, GetContentUriFromFileUseCase.Params>() {

    data class Params(
        val file: File,
    )

    override suspend fun run(params: Params): Either<Failure, Uri> {
        val file = params.file
        val filePath = FilePath.FileFull(file.absolutePath)
        return try {
            // Начиная с API 24, для предоставления доступа к файлам, который ассоциируется с приложением
            // (по сути, для открытия файла другими приложениями с помощью Intent),
            // нужно использовать механизм FileProvider.
            // Uri должен быть иметь scheme "content://"
            FileProvider.getUriForFile(
                context,
                "${appBuildInfoProvider.applicationId}.provider",
                file
            ).toRight()
        } catch (ex: Exception) {
            Failure.File.GetContentUri(filePath, ex).toLeft()
        }
    }

}
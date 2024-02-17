package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openInputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.readText
import com.gee12.mytetroid.model.FilePath

/**
 * Чтение текстового файла.
 */
class ReadTextFileUseCase(
    private val context: Context,
) : UseCase<String, ReadTextFileUseCase.Params>() {

    data class Params(
        val file: DocumentFile,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val file = params.file
        val scriptFilePath = FilePath.FileFull(file.getAbsolutePath(context))

        return try {
            file.openInputStream(context)?.use { inputStream ->
                inputStream.readText()
            }.orEmpty().toRight()
        } catch (ex: Exception) {
            Failure.File.Read(scriptFilePath, ex).toLeft()
        }
    }

}
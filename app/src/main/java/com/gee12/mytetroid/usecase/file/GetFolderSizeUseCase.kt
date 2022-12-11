package com.gee12.mytetroid.usecase.file

import android.content.Context
import android.text.format.Formatter
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.toFile
import com.gee12.mytetroid.common.utils.FileUtils

class GetFolderSizeUseCase(
    private val context: Context,
) : UseCase<String, GetFolderSizeUseCase.Params>() {

    data class Params(
        val folderPath: String,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val folderPath = params.folderPath

        return try {
            val file = folderPath.toFile()
            if (!file.exists()) {
                return Failure.File.NotExist(path = folderPath).toLeft()
            }
            val size = FileUtils.getFileSize(file)
            Formatter.formatFileSize(context, size).toRight()
        } catch (ex: SecurityException) {
            Failure.File.AccessDenied(path = folderPath, ex).toLeft()
        } catch (ex: Exception) {
            Failure.File.GetFileSize(path = folderPath, ex).toLeft()
        }
    }

}
package com.gee12.mytetroid.domain.usecase.file

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.toFile
import java.util.*

class GetFileModifiedDateUseCase(
) : UseCase<Date, GetFileModifiedDateUseCase.Params>() {

    data class Params(
        val filePath: String,
    )

    override suspend fun run(params: Params): Either<Failure, Date> {
        val filePath = params.filePath

        return try {
            val file = filePath.toFile()
            if (!file.exists()) {
                return Failure.File.NotExist(path = filePath).toLeft()
            }
            Date(file.lastModified()).toRight()
        } catch (ex: SecurityException) {
            Failure.File.AccessDenied(path = filePath, ex).toLeft()
        } catch (ex: Exception) {
            Failure.File.GetFileSize(path = filePath, ex).toLeft()
        }
    }

}
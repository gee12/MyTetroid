package com.gee12.mytetroid.domain.usecase.file

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.FileUtils
import java.io.BufferedReader
import java.io.StringReader

/**
 * Разбиение строки на блоки.
 */
class ReadTextBlocksFromStringUseCase : UseCase<List<String>, ReadTextBlocksFromStringUseCase.Params>() {

    data class Params(
        val data: String,
        val linesInBlock: Int,
    )

    override suspend fun run(params: Params): Either<Failure, List<String>> {
        return try {
            FileUtils.readToBlocks(BufferedReader(StringReader(params.data)), params.linesInBlock).toRight()
        } catch (ex: Exception) {
            Failure.UnknownError(ex).toLeft()
        }
    }

}
package com.gee12.mytetroid.domain.usecase.file

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.model.FilePath
import java.io.BufferedReader
import java.io.FileReader

/**
 * Построчное чтение текстового файла с формированием результата в виде блоков.
 */
class ReadTextBlocksFromFileUseCase : UseCase<List<String>, ReadTextBlocksFromFileUseCase.Params>() {

    data class Params(
        val fullFileName: String,
        val linesInBlock: Int,
    )

    override suspend fun run(params: Params): Either<Failure, List<String>> {
        return try {
            FileUtils.readToBlocks(BufferedReader(FileReader(params.fullFileName)), params.linesInBlock).toRight()
        } catch (ex: Exception) {
            Failure.File.Read(path = FilePath.FileFull(params.fullFileName), ex).toLeft()
        }
    }

}
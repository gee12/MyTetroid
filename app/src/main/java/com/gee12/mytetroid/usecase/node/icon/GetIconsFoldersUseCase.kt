package com.gee12.mytetroid.usecase.node.icon

import com.gee12.mytetroid.common.*
import java.io.File

/**
 * Получение списка каталогов с иконками в каталоге "icons/".
 */
class GetIconsFoldersUseCase(
) : UseCase<List<String>, GetIconsFoldersUseCase.Params>() {

    data class Params(
        val pathToIconsFolder: String,
    )

    override suspend fun run(params: Params): Either<Failure, List<String>> {
        val folder = File(params.pathToIconsFolder)
        if (!folder.exists()) {
            return Failure.Folder.IsMissing(path = folder.path).toLeft()
        }
        return folder.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.toRight()
            ?: emptyList<String>().toRight()
    }

}
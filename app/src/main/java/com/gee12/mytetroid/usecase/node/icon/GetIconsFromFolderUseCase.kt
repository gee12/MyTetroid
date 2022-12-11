package com.gee12.mytetroid.usecase.node.icon

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.model.TetroidIcon
import java.io.File

/**
 * Получение списка иконок (файлов .svg) в подкаталоге каталога "icons/".
 */
class GetIconsFromFolderUseCase(
) : UseCase<List<TetroidIcon>, GetIconsFromFolderUseCase.Params>() {

    data class Params(
        val pathToIcons: String,
        val folderName: String,
    )

    override suspend fun run(params: Params): Either<Failure, List<TetroidIcon>> {
        val pathToIcons = params.pathToIcons
        val folderName = params.folderName

        val iconsFolderFullName = makePath(pathToIcons, folderName)
        val folder = File(iconsFolderFullName)
        if (!folder.exists()) {
            return Failure.Folder.NotExist(path = folder.path).toLeft()
        }

        return folder.listFiles()?.filter { file ->
            file.isFile && file.name.lowercase().endsWith(".svg")
        }?.map { file ->
            TetroidIcon(folderName, file.name)
        }?.toRight()
            ?: emptyList<TetroidIcon>().toRight()
    }

}
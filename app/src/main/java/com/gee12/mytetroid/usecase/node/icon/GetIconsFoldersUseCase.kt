package com.gee12.mytetroid.usecase.node.icon

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.toFile
import com.gee12.mytetroid.providers.IStoragePathProvider

/**
 * Получение списка каталогов с иконками в каталоге "icons/".
 */
class GetIconsFoldersUseCase(
    private val storagePathProvider: IStoragePathProvider,
) : UseCase<List<String>, GetIconsFoldersUseCase.Params>() {

    object Params

    override suspend fun run(params: Params): Either<Failure, List<String>> {
        val folder = storagePathProvider.getPathToIcons().toFile()
        if (!folder.exists()) {
            return Failure.Folder.NotExist(path = folder.path).toLeft()
        }
        return folder.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.toRight()
            ?: emptyList<String>().toRight()
    }

}
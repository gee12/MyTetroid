package com.gee12.mytetroid.domain.usecase.node.icon

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath

/**
 * Получение списка имен каталогов с иконками в каталоге "icons/".
 */
class GetIconsFolderNamesUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
) : UseCase<List<String>, GetIconsFolderNamesUseCase.Params>() {

    object Params

    override suspend fun run(params: Params): Either<Failure, List<String>> {
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val iconsFolderRelativePath = storagePathProvider.getRelativePathToIconsFolder()
        val iconsFolderPath = FilePath.Folder(storageFolderPath, iconsFolderRelativePath)

        val iconsFolder = storageFolder?.child(
            context = context,
            path = iconsFolderRelativePath,
            requiresWriteAccess = false,
        ) ?: return Failure.Folder.Get(iconsFolderPath).toLeft()

        if (!iconsFolder.exists()) {
            return Failure.Folder.NotExist(iconsFolderPath).toLeft()
        }
        return iconsFolder.listFiles()
            .filter { it.isDirectory }
            .mapNotNull { it.name }
            .toRight()
    }

}
package com.gee12.mytetroid.domain.usecase.node.icon

import android.content.Context
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.extension
import com.anggrayudi.storage.file.getAbsolutePath
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makeFolderPath
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.ImageFileType

/**
 * Получение списка иконок (изображений) в конкретном подкаталоге каталога "icons/".
 */
class GetNodesIconsFromFolderUseCase(
    private val context: Context,
    private val storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
) : UseCase<List<TetroidIcon>, GetNodesIconsFromFolderUseCase.Params>() {

    data class Params(
        val folderName: String,
    )

    override suspend fun run(params: Params): Either<Failure, List<TetroidIcon>> {
        val folderName = params.folderName
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val iconFolderRelativePath = makeFolderPath(storagePathProvider.getRelativePathToIconsFolder(), folderName)
        val iconFolderPath = FilePath.Folder(storageFolderPath, iconFolderRelativePath)

        val iconsFolder = storageFolder?.child(
            context = context,
            path = iconFolderRelativePath,
            requiresWriteAccess = false,
        ) ?: return Failure.Folder.Get(iconFolderPath).toLeft()

        if (!iconsFolder.exists()) {
            return Failure.Folder.NotExist(iconFolderPath).toLeft()
        }

        return iconsFolder.listFiles().filter { file ->
            file.isFile
                    && file.name != null
                    && file.extension in ImageFileType.values().filter { it.supportedAsNodeIcon }.map { it.extension }
        }.map { file ->
            TetroidIcon(folderName, file.name)
        }.toRight()
    }

}
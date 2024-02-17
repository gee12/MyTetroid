package com.gee12.mytetroid.domain.usecase.storage

import androidx.documentfile.provider.DocumentFile
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.usecase.file.GetFolderUseCase
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Получение каталога корзины хранилище с созданием, если нужно.
 */
class GetStorageTrashFolderUseCase(
    private val appPathProvider: IAppPathProvider,
    private val getFolderUseCase: GetFolderUseCase,
) : UseCase<DocumentFile, GetStorageTrashFolderUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val isCreateIfNotExist: Boolean = true,
    )

    override suspend fun run(params: Params): Either<Failure, DocumentFile> {
        val storage = params.storage
        val trashFolderPath = appPathProvider.getPathToTrashFolder().fullPath
        val storageTrashFolderPath = FilePath.Folder(trashFolderPath, storage.id.toString())

        return getFolderUseCase.run(
            GetFolderUseCase.Params(
                path = storageTrashFolderPath.fullPath,
                isCreateIfNotExist = true,
            )
        )
    }

}
package com.gee12.mytetroid.domain.usecase.storage

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.domain.manager.ClipboardManager
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.usecase.file.ClearFolderUseCase
import com.gee12.mytetroid.model.TetroidStorage

class ClearStorageTrashFolderUseCase(
    private val storagePathProvider: IStoragePathProvider,
    private val clearFolderUseCase: ClearFolderUseCase,
) : UseCase<UseCase.None, ClearStorageTrashFolderUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        // очищаем "буфер обмена", т.к. каталог(и) записи из корзины будут удалены
        // и нечего будет вставлять
        ClipboardManager.clear()

        val trashPath = storagePathProvider.getPathToStorageTrashFolder()
        return clearFolderUseCase.run(
            ClearFolderUseCase.Params(
                folderPath = trashPath.fullPath,
            )
        )
    }

}
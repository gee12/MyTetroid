package com.gee12.mytetroid.domain.usecase.storage

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.domain.manager.ClipboardManager
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.usecase.file.ClearFolderUseCase

class ClearAllStoragesTrashFolderUseCase(
    private val appPathProvider: IAppPathProvider,
    private val clearFolderUseCase: ClearFolderUseCase,
) : UseCase<UseCase.None, ClearAllStoragesTrashFolderUseCase.Params>() {

    object Params

    override suspend fun run(params: Params): Either<Failure, None> {
        // очищаем "буфер обмена", т.к. каталог(и) записи из корзины будут удалены
        // и нечего будет вставлять
        ClipboardManager.clear()

        return clearFolderUseCase.run(
            ClearFolderUseCase.Params(
                folderPath = appPathProvider.getPathToTrashFolder().fullPath,
            )
        )
    }

}
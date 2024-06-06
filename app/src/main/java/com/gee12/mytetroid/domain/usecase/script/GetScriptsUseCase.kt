package com.gee12.mytetroid.domain.usecase.script

import android.content.Context
import com.anggrayudi.storage.file.child
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.obj.TetroidObject
import com.gee12.mytetroid.model.TetroidScript

class GetScriptsUseCase(
    private val context: Context,
    private val storagePathProvider: IStoragePathProvider,
    private val storageProvider: IStorageProvider,
    private val scriptsManager: ScriptsManager,
) : UseCase<List<TetroidScript>, GetScriptsUseCase.Params>() {

    data class Params(
        val scriptObject: TetroidObject?,
    )

    override suspend fun run(params: Params): Either<Failure, List<TetroidScript>> {
        val scriptObject = params.scriptObject

        val scripts = scriptsManager.getScripts(obj = scriptObject)

        val storageFolderPath = storagePathProvider.getPathToRootFolder().fullPath
        val storageFolder = storageProvider.rootFolder

        return scripts.onEach { script ->
            val scriptFileRelativePath = storagePathProvider.getRelativePathToScript(script.fileName)
            val scriptFilePath = FilePath.File(storageFolderPath, scriptFileRelativePath)

            val scriptFile = storageFolder?.child(
                context = context,
                path = scriptFileRelativePath,
                requiresWriteAccess = true,
            )
            if (scriptFile == null || !scriptFile.exists()) {
                script.errors = listOf(
                    Failure.Script.FileIsNotExist(script, scriptFilePath)
                )
            }
        }.toRight()

    }

}
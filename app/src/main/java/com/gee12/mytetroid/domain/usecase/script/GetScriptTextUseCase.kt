package com.gee12.mytetroid.domain.usecase.script

import android.content.Context
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.usecase.file.ReadTextFileUseCase
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidScript

class GetScriptTextUseCase(
    private val context: Context,
    private val storagePathProvider: IStoragePathProvider,
    private val storageProvider: IStorageProvider,
    private val readTextFileUseCase: ReadTextFileUseCase,
) : UseCase<String, GetScriptTextUseCase.Params>() {

    data class Params(
        val script: TetroidScript,
    )

    override suspend fun run(params: Params): Either<Failure, String> {
        val script = params.script
        val scriptFileRelativePath = storagePathProvider.getRelativePathToScript(script.fileName)
        val storageFolderPath = storagePathProvider.getPathToRootFolder().fullPath
        val scriptFilePath = FilePath.File(storageFolderPath, scriptFileRelativePath)
        val storageFolder = storageProvider.rootFolder

        val scriptFile = storageFolder?.child(
            context = context,
            path = scriptFileRelativePath,
            requiresWriteAccess = true,
        ) ?: return Failure.File.Get(scriptFilePath).toLeft()

        return readTextFileUseCase.run(
            ReadTextFileUseCase.Params(scriptFile)
        )
    }

}
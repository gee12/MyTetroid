package com.gee12.mytetroid.domain.usecase.script

import android.content.Context
import com.anggrayudi.storage.file.child
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidScript

class DeleteScriptFileUseCase(
    private val context: Context,
    private val storagePathProvider: IStoragePathProvider,
    private val storageProvider: IStorageProvider,
    private val scriptsManager: ScriptsManager,
) : UseCase<UseCase.None, DeleteScriptFileUseCase.Params>() {

    data class Params(
        val script: TetroidScript,
        val withFile: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val script = params.script
        return if (scriptsManager.deleteScript(script)) {
            if (params.withFile) {
                deleteScriptFile(fileName = script.fileName)
            } else {
                None.toRight()
            }
        } else {
            Failure.Database.Delete.toLeft()
        }
    }

    private fun deleteScriptFile(fileName: String): Either<Failure, None> {
        val scriptFileRelativePath = storagePathProvider.getRelativePathToScript(fileName)
        val storageFolderPath = storagePathProvider.getPathToRootFolder().fullPath
        val scriptFilePath = FilePath.File(storageFolderPath, scriptFileRelativePath)
        val storageFolder = storageProvider.rootFolder

        val scriptFile = storageFolder?.child(
            context = context,
            path = scriptFileRelativePath,
            requiresWriteAccess = true,
        ) ?: return None.toRight()

        return if (scriptFile.delete()) {
            None.toRight()
        } else {
            Failure.File.Delete(scriptFilePath).toLeft()
        }
    }

}
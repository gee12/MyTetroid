package com.gee12.mytetroid.domain.usecase.script

import android.content.Context
import com.anggrayudi.storage.file.child
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidScript

class EditScriptUseCase(
    private val context: Context,
    private val storagePathProvider: IStoragePathProvider,
    private val storageProvider: IStorageProvider,
    private val scriptsManager: ScriptsManager,
    private val saveScriptTextToFileUseCase: SaveScriptTextToFileUseCase,
) : UseCase<UseCase.None, EditScriptUseCase.Params>() {

    data class Params(
        val script: TetroidScript,
        val name: String,
        val fileName: String,
        val description: String?,
        val scriptText: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val script = params.script
        val name = params.name
        val fileName = params.fileName
        val description = params.description
        val oldFileName = script.fileName
        return if (scriptsManager.updateScriptFields(script, name, fileName, description)) {
            if (oldFileName != fileName) {
                renameScriptFile(oldFileName, fileName)
            } else {
                None.toRight()
            }.flatMap {
                saveScriptTextToFileUseCase.run(
                    SaveScriptTextToFileUseCase.Params(
                        scriptFileName = script.fileName,
                        scriptText = params.scriptText,
                    )
                )
            }
        } else {
            Failure.Database.Update.toLeft()
        }
    }

    private fun renameScriptFile(currentFileName: String, newFileName: String): Either<Failure, None> {
        val scriptFileRelativePath = storagePathProvider.getRelativePathToScript(currentFileName)
        val storageFolderPath = storagePathProvider.getPathToRootFolder().fullPath
        val scriptFilePath = FilePath.File(storageFolderPath, scriptFileRelativePath)
        val storageFolder = storageProvider.rootFolder

        val scriptFile = storageFolder?.child(
            context = context,
            path = scriptFileRelativePath,
            requiresWriteAccess = true,
        ) ?: return None.toRight()

        return if (scriptFile.renameTo(newFileName)) {
            None.toRight()
        } else {
            Failure.File.Rename(scriptFilePath, newFileName).toLeft()
        }
    }

}
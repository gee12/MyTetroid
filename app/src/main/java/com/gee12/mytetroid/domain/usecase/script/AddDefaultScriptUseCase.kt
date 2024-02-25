package com.gee12.mytetroid.domain.usecase.script

import android.content.Context
import com.gee12.htmlwysiwygeditor.ext.readTextFileFromAssets
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.enums.DefaultScript

class AddDefaultScriptUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val storageProvider: IStorageProvider,
    private val scriptsManager: ScriptsManager,
    private val saveScriptTextToFileUseCase: SaveScriptTextToFileUseCase,
    private val getUniqueScriptFileNameUseCase: GetUniqueScriptFileNameUseCase,
) : UseCase<UseCase.None, AddDefaultScriptUseCase.Params>() {

    data class Params(
        val script: DefaultScript,
    )

    private val storageId: Int
        get() = storageProvider.storage?.id.orZero()

    override suspend fun run(params: Params): Either<Failure, None> {
        val defaultScript = params.script

        return getUniqueScriptFileNameUseCase.run(
            GetUniqueScriptFileNameUseCase.Params(
                originalFileName = defaultScript.fileName,
            )
        ).flatMap { newFileName ->
            val script = TetroidScript(
                storageId = storageId,
                fileName = newFileName,
                description = buildString {
                    append(defaultScript.getTitle(resourcesProvider))
                    append(": ")
                    append(defaultScript.getDescription(resourcesProvider))
                },
            )
            if (scriptsManager.insertScript(script)) {
                saveScriptTextToFile(defaultScript, script)
            } else {
                Failure.Database.Insert.toLeft()
            }
        }
    }

    private suspend fun saveScriptTextToFile(
        defaultScript: DefaultScript,
        script: TetroidScript,
    ): Either<Failure, None> {
        val scriptPath = "scripts/${defaultScript.fileName}"
        return context.readTextFileFromAssets(scriptPath)?.let { scriptText ->
            saveScriptTextToFileUseCase.run(
                SaveScriptTextToFileUseCase.Params(
                    scriptFileName = script.fileName,
                    scriptText = scriptText,
                )
            )
        } ?: Failure.Script.FileIsNotExist(script, FilePath.FileFull(scriptPath)).toLeft()
    }

}
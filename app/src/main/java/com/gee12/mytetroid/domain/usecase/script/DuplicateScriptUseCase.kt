package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.TetroidScriptToObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

class DuplicateScriptUseCase(
    private val scriptsManager: ScriptsManager,
    private val getUniqueScriptFileNameUseCase: GetUniqueScriptFileNameUseCase,
    private val getScriptTextUseCase: GetScriptTextUseCase,
    private val saveScriptUseCase: SaveScriptUseCase,
) : UseCase<TetroidScript, DuplicateScriptUseCase.Params>() {

    data class Params(
        val script: TetroidScript,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidScript> {
        val script = params.script

        return getScriptTextUseCase.run(
            GetScriptTextUseCase.Params(script)
        ).flatMap { scriptText ->
            getUniqueScriptFileNameUseCase.run(
                GetUniqueScriptFileNameUseCase.Params(
                    originalFileName = script.fileName,
                )
            ).flatMap { newFileName ->
                saveScriptUseCase.run(
                    SaveScriptUseCase.Params(
                        name = script.name,
                        fileName = newFileName,
                        description = script.description,
                        scriptText = scriptText,
                    )
                )
            }.flatMap { newScript ->
                duplicateScriptObjects(
                    scriptId = script.id!!,
                    newScript = newScript,
                )
                newScript.toRight()
            }
        }
    }

    private suspend fun duplicateScriptObjects(scriptId: Int, newScript: TetroidScript) {
        scriptsManager.getScriptObjects(
            scriptId = scriptId,
        ).onEach { scriptToObjectDbEntity ->
            scriptsManager.insertScriptToObject(
                scriptToObject = TetroidScriptToObject(
                    scriptId = newScript.id!!,
                    objectId = scriptToObjectDbEntity.objectId,
                    objectType = scriptToObjectDbEntity.objectTypeId?.let { TetroidObjectType.getById(it) },
                )
            )
        }
    }

}
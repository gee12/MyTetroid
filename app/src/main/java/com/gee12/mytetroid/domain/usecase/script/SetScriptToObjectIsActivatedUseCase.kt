package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.model.TetroidScriptToObject

class SetScriptToObjectIsActivatedUseCase(
    private val scriptsManager: ScriptsManager,
) : UseCase<UseCase.None, SetScriptToObjectIsActivatedUseCase.Params>() {

    data class Params(
        val scriptToObject: TetroidScriptToObject,
        val isActive: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val scriptToObject = params.scriptToObject
        val isActive = params.isActive

        return if (scriptsManager.updateScriptIsActiveForObject(scriptToObject, isActive)) {
            None.toRight()
        } else {
            Failure.Database.Update.toLeft()
        }
    }

}
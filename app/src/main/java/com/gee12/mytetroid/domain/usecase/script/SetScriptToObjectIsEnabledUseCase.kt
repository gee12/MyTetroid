package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.model.TetroidScriptToObject

class SetScriptToObjectIsEnabledUseCase(
    private val scriptsManager: ScriptsManager,
) : UseCase<UseCase.None, SetScriptToObjectIsEnabledUseCase.Params>() {

    data class Params(
        val scriptToObject: TetroidScriptToObject,
        val isEnabled: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val scriptToObject = params.scriptToObject
        val isEnabled = params.isEnabled

        return if (scriptsManager.updateScriptIsActiveForObject(scriptToObject, isEnabled)) {
            None.toRight()
        } else {
            Failure.Database.Update.toLeft()
        }
    }

}
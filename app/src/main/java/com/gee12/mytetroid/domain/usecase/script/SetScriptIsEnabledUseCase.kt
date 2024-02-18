package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.model.ITetroidObject
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.TetroidScriptToObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

class SetScriptIsEnabledUseCase(
    private val scriptsManager: ScriptsManager,
) : UseCase<UseCase.None, SetScriptIsEnabledUseCase.Params>() {

    data class Params(
        val script: TetroidScript,
        val obj: ITetroidObject?,
        val isActive: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val script = params.script
        val obj = params.obj
        val isActive = params.isActive
        var scriptToObject = script.getScriptObject(obj)

        return if (scriptToObject != null) {
            if (scriptsManager.updateScriptIsActiveForObject(scriptToObject, isActive)) {
                None.toRight()
            } else {
                Failure.Database.Update.toLeft()
            }
        } else {
            scriptToObject = TetroidScriptToObject(
                scriptId = script.id.orZero(),
                objectId = obj?.id,
                objectType = obj?.type?.let { TetroidObjectType.getById(it) },
                objectName = obj?.name,
                isActive = isActive,
            )
            if (scriptsManager.insertScriptToObject(scriptToObject)) {
                None.toRight()
            } else {
                Failure.Database.Insert.toLeft()
            }
        }
    }

}
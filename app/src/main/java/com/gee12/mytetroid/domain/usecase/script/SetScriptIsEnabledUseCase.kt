package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.TetroidScriptToObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

class SetScriptIsEnabledUseCase(
    private val scriptsManager: ScriptsManager,
) : UseCase<UseCase.None, SetScriptIsEnabledUseCase.Params>() {

    data class Params(
        val script: TetroidScript,
        val obj: TetroidObject?,
        val isActive: Boolean,
        val forCurrentObjectOnly : Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val script = params.script
        val obj = params.obj
        val isActive = params.isActive

        return setScriptIsActivated(
            script = script,
            obj = obj,
            isActive = isActive,
        ).flatMap {
            // устанавливаем и для всего хранилища (obj=null), если нужно
            if (!params.forCurrentObjectOnly && obj != null) {
                setScriptIsActivated(
                    script = script,
                    obj = null,
                    isActive = isActive,
                )
            } else {
                None.toRight()
            }
        }
    }

    private suspend fun setScriptIsActivated(
        script: TetroidScript,
        obj: TetroidObject?,
        isActive: Boolean,
    ): Either<Failure, None> {
        val scriptToObject = script.getScriptObject(obj)
            ?: TetroidScriptToObject(
                scriptId = script.id.orZero(),
                objectId = obj?.id,
                objectType = obj?.type?.let { TetroidObjectType.getById(it) },
                isActive = isActive,
            ).also {
                it.script = script
                it.obj = obj
            }

        return if (scriptsManager.updateScriptIsActiveForObject(scriptToObject, isActive)) {
            None.toRight()
        } else {
            Failure.Database.Update.toLeft()
        }
    }

}
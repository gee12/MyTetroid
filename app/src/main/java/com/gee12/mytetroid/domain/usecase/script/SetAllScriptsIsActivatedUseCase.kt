package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.model.obj.TetroidObject

class SetAllScriptsIsActivatedUseCase(
    private val scriptsManager: ScriptsManager,
    private val setScriptIsEnabledUseCase: SetScriptIsActivatedUseCase,
) : UseCase<UseCase.None, SetAllScriptsIsActivatedUseCase.Params>() {

    data class Params(
        val obj: TetroidObject?,
        val isActive: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val isActive = params.isActive
        // добавляем только по всему хранилищу (null),
        // а удаляем и по объекту (obj), и по всему хранилищу (null)
        val obj = if (isActive) null else params.obj

        val scripts = scriptsManager.getScripts(obj = obj)

        scripts.forEach { script ->
            setScriptIsEnabledUseCase.run(
                SetScriptIsActivatedUseCase.Params(
                    script = script,
                    obj = obj,
                    isActive = isActive,
                    forCurrentObjectOnly = false,
                )
            ).onFailure {
                return it.toLeft()
            }
        }

        return None.toRight()
    }

}
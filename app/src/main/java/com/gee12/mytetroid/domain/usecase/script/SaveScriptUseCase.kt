package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.common.toLeft
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.TetroidScript

class SaveScriptUseCase(
    private val storageProvider: IStorageProvider,
    private val scriptsManager: ScriptsManager,
    private val saveScriptTextToFileUseCase: SaveScriptTextToFileUseCase,
) : UseCase<UseCase.None, SaveScriptUseCase.Params>() {

    data class Params(
        val fileName: String,
        val description: String,
        val scriptText: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val script = TetroidScript(
            storageId = storageProvider.storage?.id.orZero(),
            fileName = params.fileName,
            description = params.description,
        )
        return if (scriptsManager.insertScript(script)) {
            saveScriptTextToFileUseCase.run(
                SaveScriptTextToFileUseCase.Params(
                    scriptFileName = script.fileName,
                    scriptText = params.scriptText,
                )
            )
        } else {
            Failure.Database.Insert.toLeft()
        }
    }

}
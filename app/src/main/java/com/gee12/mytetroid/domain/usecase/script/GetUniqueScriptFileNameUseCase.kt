package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.common.extensions.splitToBaseAndExtension
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FileName

class GetUniqueScriptFileNameUseCase(
    private val storageProvider: IStorageProvider,
    private val scriptsManager: ScriptsManager,
) : UseCase<String, GetUniqueScriptFileNameUseCase.Params>() {

    data class Params(
        val originalFileName: String,
    )

    private val storageId: Int
        get() = storageProvider.storage?.id.orZero()

    override suspend fun run(params: Params): Either<Failure, String> {
        val originalFileName = params.originalFileName
        var newFilename = originalFileName
        var index = 1
        while (!scriptsManager.isUniqueFileName(storageId, scriptId = null, fileName = newFilename)) {
            val parts = originalFileName.splitToBaseAndExtension()
            newFilename = when (parts) {
                is FileName.FromFullName -> {
                    "${parts.fullName}_${index}"
                }
                is FileName.FromParts -> {
                    "${parts.base}_${index}.${parts.extension}"
                }
            }
            index++
        }
        return newFilename.toRight()
    }

}
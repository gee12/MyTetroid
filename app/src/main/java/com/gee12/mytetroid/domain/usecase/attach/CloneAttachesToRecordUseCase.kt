package com.gee12.mytetroid.domain.usecase.attach

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.model.obj.TetroidFile
import com.gee12.mytetroid.model.obj.TetroidRecord

/**
 * Перемещение или копирование прикрепленных файлов в другую запись.
 */
class CloneAttachesToRecordUseCase(
    private val dataNameProvider: IDataNameProvider,
    private val cryptManager: IStorageCryptManager,
) : UseCase<UseCase.None, CloneAttachesToRecordUseCase.Params>() {

    data class Params(
        val srcRecord: TetroidRecord,
        val destRecord: TetroidRecord,
        val isCutting: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val destRecord = params.destRecord
        val isCutting = params.isCutting

        if (srcRecord.attachedFilesCount > 0) {
            val isEncrypted = destRecord.isEncrypted
            val attaches = mutableListOf<TetroidFile>()
            for (srcAttach in srcRecord.attachedFiles) {
                // генерируем уникальные идентификаторы, если запись копируется
                val id = if (isCutting) srcAttach.id else dataNameProvider.createUniqueId()
                val name = srcAttach.name
                val attach = TetroidFile(
                    id = id,
                    name = encryptFieldIfNeed(name, isEncrypted) ?: name,
                    isEncrypted = isEncrypted,
                    fileType = srcAttach.fileType,
                    record = destRecord,
                ).apply {
                    if (isEncrypted) {
                        decryptedName = name
                        isDecrypted = true
                    }
                }
                attaches.add(attach)
            }
            destRecord.attachedFiles = attaches
        }

        return None.toRight()
    }

    private fun encryptFieldIfNeed(value: String?, isEncrypt: Boolean): String? {
        return if (isEncrypt) value?.let { cryptManager.encryptTextBase64(value) } else value
    }


}
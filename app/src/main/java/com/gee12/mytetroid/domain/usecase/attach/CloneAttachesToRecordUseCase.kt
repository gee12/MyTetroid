package com.gee12.mytetroid.domain.usecase.attach

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidRecord

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
        val isCutted: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val srcRecord = params.srcRecord
        val destRecord = params.destRecord
        val isCutted = params.isCutted

        if (srcRecord.attachedFilesCount > 0) {
            val isCrypted = destRecord.isCrypted
            val attaches = mutableListOf<TetroidFile>()
            for (srcAttach in srcRecord.attachedFiles) {
                // генерируем уникальные идентификаторы, если запись копируется
                val id = if (isCutted) srcAttach.id else dataNameProvider.createUniqueId()
                val name = srcAttach.name
                val attach = TetroidFile(
                    isCrypted,
                    id,
                    encryptFieldIfNeed(name, isCrypted),
                    srcAttach.fileType,
                    destRecord,
                )
                if (isCrypted) {
                    attach.setDecryptedName(name)
                    attach.setIsCrypted(true)
                    attach.setIsDecrypted(true)
                }
                attaches.add(attach)
            }
            destRecord.attachedFiles = attaches
        }

        return None.toRight()
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

}
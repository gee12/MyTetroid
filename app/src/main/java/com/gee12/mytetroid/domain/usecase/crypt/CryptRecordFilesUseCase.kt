package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getIdString
import com.gee12.mytetroid.domain.provider.IRecordPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidRecord
import java.io.File

/**
 * Зашифровка или расшифровка файла записи и прикрепленных файлов при необходимости.
 */
class CryptRecordFilesUseCase(
    private val logger: ITetroidLogger,
    private val resourcesProvider: IResourcesProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val encryptOrDecryptFileUseCase: EncryptOrDecryptFileUseCase,
) : UseCase<Boolean, CryptRecordFilesUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val isCrypted: Boolean,
        val isEncrypt: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        val record = params.record
        val isCrypted = params.isCrypted
        val isEncrypt = params.isEncrypt

        // файл записи
        val recordFolderPath = recordPathProvider.getPathToRecordFolder(record)
        var file = File(recordFolderPath, record.fileName)

        encryptOrDecryptFileUseCase.run(
            EncryptOrDecryptFileUseCase.Params(
                file = file,
                isCrypted = isCrypted,
                isEncrypt = isEncrypt,
            )
        ).onFailure {
            return it.toLeft()
        }

        // прикрепленные файлы
        if (record.attachedFilesCount > 0) {
            for (attach in record.attachedFiles) {
                file = File(recordFolderPath, attach.idName)
                if (!file.exists()) {
                    logger.logWarning(resourcesProvider.getString(R.string.error_file_is_missing_mask) + attach.getIdString(resourcesProvider), false)
                    continue
                }
                encryptOrDecryptFileUseCase.run(
                    EncryptOrDecryptFileUseCase.Params(
                        file = file,
                        isCrypted = isCrypted,
                        isEncrypt = isEncrypt,
                    )
                ).onFailure {
                    return it.toLeft()
                }

            }
        }
        return true.toRight()
    }
}
package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.ITaskProgress
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase

class ChangePasswordUseCase(
    private val storageProvider: IStorageProvider,
    private val cryptManager: IStorageCryptManager,
    private val saveStorageUseCase: SaveStorageUseCase,
    private val decryptStorageUseCase: DecryptStorageUseCase,
    private val initPasswordUseCase: InitPasswordUseCase,
    private val savePasswordInConfigUseCase: SavePasswordInConfigUseCase,
) : UseCase<Boolean, ChangePasswordUseCase.Params>() {

    data class Params(
        val curPassword: String,
        val newPassword: String,
        val taskProgress: ITaskProgress,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        // сначала устанавливаем текущий пароль
        return initPassword(params, initNewPassword = false)
            // и расшифровываем хранилище
            .flatMap {
                decryptStorage(params)
            }.flatMap { result ->
                ifEitherOrFalseSuspend(result) {
                    // теперь устанавливаем новый пароль
                    initPassword(params, initNewPassword = true)
                        // и перешифровываем зашифрованные ветки
                        .flatMap {
                            reEncryptStorage(params)
                        }.flatMap { result ->
                            ifEitherOrFalseSuspend(result) {
                                // сохраняем mytetra.xml
                                saveStorage(params)
                                    // сохраняем database.ini
                                    .flatMap { savePasswordInConfig(params) }
                                    .map { true }
                            }
                        }
                }
            }
    }

    private suspend fun initPassword(params: Params, initNewPassword: Boolean): Either<Failure, None> {
        val logObj = if (initNewPassword) LogObj.NEW_PASS else LogObj.CUR_PASS
        params.taskProgress.nextStage(logObj, LogOper.SET, TaskStage.Stages.START)

        return initPasswordUseCase.run(
            InitPasswordUseCase.Params(
                password = if (initNewPassword) params.newPassword else params.curPassword,
            )
        )
    }

    private suspend fun decryptStorage(params: Params) : Either<Failure, Boolean> {
        val storage = storageProvider.storage
            ?: return Failure.Storage.StorageNotInited.toLeft()

        return params.taskProgress.nextStage(LogObj.STORAGE, LogOper.DECRYPT) {
            decryptStorageUseCase.run(
                DecryptStorageUseCase.Params(decryptFiles = true)
            ).map { result ->
                storage.isDecrypted = result
                result
            }
        }
    }

    /**
     * Перешифровка хранилища (перед этим ветки должны быть расшифрованы).
     */
    private suspend fun reEncryptStorage(params: Params) : Either<Failure, Boolean> {
        return params.taskProgress.nextStage(LogObj.STORAGE, LogOper.REENCRYPT) {
            cryptManager.encryptNodes(
                nodes = storageProvider.getRootNodes(),
                isReencrypt = true
            ).toRight()
        }
    }

    private suspend fun saveStorage(params: Params) : Either<Failure, None> {
        return params.taskProgress.nextStage(LogObj.STORAGE, LogOper.SAVE) {
            saveStorageUseCase.run()
        }
    }

    private suspend fun savePasswordInConfig(params: Params) : Either<Failure, None> {
        return params.taskProgress.nextStage(LogObj.NEW_PASS, LogOper.SAVE) {
            savePasswordInConfigUseCase.run(
                SavePasswordInConfigUseCase.Params(
                    password = params.newPassword,
                )
            )
        }
    }

}
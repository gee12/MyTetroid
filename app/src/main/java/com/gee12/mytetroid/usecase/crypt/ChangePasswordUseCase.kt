package com.gee12.mytetroid.usecase.crypt

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.ITaskProgress
import com.gee12.mytetroid.interactors.EncryptionInteractor
import com.gee12.mytetroid.interactors.PasswordInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.logs.TaskStage
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChangePasswordUseCase(
    private val logger: ITetroidLogger,
    private val cryptInteractor: EncryptionInteractor,
    private val passInteractor: PasswordInteractor,
    private val storagesRepo: StoragesRepo,
    private val saveStorageUseCase: SaveStorageUseCase,
    private val decryptStorageUseCase: DecryptStorageUseCase,
) : UseCase<Boolean, ChangePasswordUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val curPass: String,
        val newPass: String,
        val taskProgress: ITaskProgress,
    )

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        val storage = params.storage
        val curPass = params.curPass
        val newPass = params.newPass
        val taskProgress = params.taskProgress

        // сначала устанавливаем текущий пароль
        taskProgress.nextStage(LogObj.CUR_PASS, LogOper.SET, TaskStage.Stages.START)
        initPass(storage, curPass)
            .onFailure {
                return it.toLeft()
            }.onSuccess { result ->
                if (!result) {
                    return false.toRight()
                }
            }

        // и расшифровываем хранилище
        if (!taskProgress.nextStage(LogObj.STORAGE, LogOper.DECRYPT) {
                decryptStorageUseCase.run(
                    DecryptStorageUseCase.Params(decryptFiles = true)
                ).map { result ->
                    storage.isDecrypted = result
                    result
                }.foldResult(
                    onLeft = {
                        logger.logFailure(it)
                        false
                    },
                    onRight = {
                        it
                    }
                )
            }
        ) return false.toRight()

        // теперь устанавливаем новый пароль
        taskProgress.nextStage(LogObj.NEW_PASS, LogOper.SET, TaskStage.Stages.START)
        initPass(storage, newPass)

        // и перешифровываем зашифрованные ветки
        if (!taskProgress.nextStage(LogObj.STORAGE, LogOper.REENCRYPT) {
                cryptInteractor.reencryptStorage()
            }
        ) return false.toRight()

        // сохраняем mytetra.xml
        taskProgress.nextStage(LogObj.STORAGE, LogOper.SAVE) {
            saveStorage()
        }

        // сохраняем данные в database.ini
        taskProgress.nextStage(LogObj.NEW_PASS, LogOper.SAVE, TaskStage.Stages.START)
        passInteractor.savePassCheckData(newPass)

        return true.toRight()
    }

    private suspend fun initPass(storage: TetroidStorage, pass: String): Either<Failure, Boolean> {
        passInteractor.initPass(storage, pass)
        return storagesRepo.updateStorage(storage).toRight()
    }

    private suspend fun saveStorage(): Boolean {
        return withContext(Dispatchers.IO) {
            saveStorageUseCase.run()
        }.foldResult(
            onLeft = {
                logger.logFailure(it)
                false
            },
            onRight = { it }
        )
    }

}
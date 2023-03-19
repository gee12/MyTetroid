package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*

/**
 * Установка пароля хранилища впервые.
 */
class SetupPasswordUseCase(
    private val initPasswordUseCase: InitPasswordUseCase,
    private val savePasswordInConfigUseCase: SavePasswordInConfigUseCase,
) : UseCase<UseCase.None, SetupPasswordUseCase.Params>() {

    data class Params(
        val password: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        return savePasswordInConfig(params).flatMap {
            initPassword(params)
        }
    }

    private suspend fun savePasswordInConfig(params: Params): Either<Failure, None> {
        return savePasswordInConfigUseCase.run(
            SavePasswordInConfigUseCase.Params(
                password = params.password,
            )
        )
    }

    private suspend fun initPassword(params: Params) : Either<Failure, None> {
        return initPasswordUseCase.run(
            InitPasswordUseCase.Params(
                password = params.password,
            )
        )
    }

}
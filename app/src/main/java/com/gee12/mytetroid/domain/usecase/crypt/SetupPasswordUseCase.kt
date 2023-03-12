package com.gee12.mytetroid.domain.usecase.crypt

import com.gee12.mytetroid.common.*

/**
 * Установка пароля хранилища впервые.
 */
class SetupPasswordUseCase(
    private val initPasswordUseCase: InitPasswordUseCase,
    private val savePasswordCheckDataUseCase: SavePasswordCheckDataUseCase,
) : UseCase<UseCase.None, SetupPasswordUseCase.Params>() {

    data class Params(
        val password: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        return savePassCheckData(params)
            .flatMap { initPassword(params) }
            .map { None }
    }

    private suspend fun initPassword(params: Params) : Either<Failure, None> {
        return initPasswordUseCase.run(
            InitPasswordUseCase.Params(
                password = params.password,
            )
        )
    }

    private suspend fun savePassCheckData(params: Params): Either<Failure, None> {
        return savePasswordCheckDataUseCase.run(
            SavePasswordCheckDataUseCase.Params(
                password = params.password,
            )
        )
    }

}
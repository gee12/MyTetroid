package com.gee12.mytetroid.domain.usecase.script

import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.map
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidScript

class GetActiveScriptsForRecordUseCase(
    private val getScriptsUseCase: GetScriptsUseCase,
) : UseCase<List<TetroidScript>, GetActiveScriptsForRecordUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
    )

    override suspend fun run(params: Params): Either<Failure, List<TetroidScript>> {
        return getScriptsUseCase.run(
            GetScriptsUseCase.Params(
                scriptObject = params.record
            )
        ).map { scripts ->
            scripts.filter {
                it.isActiveByErrors()
            }
        }
    }

}
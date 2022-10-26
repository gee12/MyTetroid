package com.gee12.mytetroid.usecase

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.helpers.CommonSettingsProvider
import com.gee12.mytetroid.logs.ITetroidLogger

class InitAppUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val commonSettingsProvider: CommonSettingsProvider,
) : UseCase<UseCase.None, InitAppUseCase.Params>() {

    object Params

    override suspend fun run(params: Params): Either<Failure, None> {

        logger.logRaw("************************************************************")
        logger.log(context.getString(R.string.log_app_start_mask).format(Utils.getVersionName(logger, context)), false)
        if (commonSettingsProvider.isCopiedFromFree()) {
            logger.log(R.string.log_settings_copied_from_free, true)
        }

        return None.toRight()
    }
}
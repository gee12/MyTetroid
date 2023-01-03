package com.gee12.mytetroid.domain.usecase

import android.content.Context
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getAppVersionName
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.provider.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import java.io.File

class InitAppUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val settingsManager: CommonSettingsManager,
) : UseCase<UseCase.None, InitAppUseCase.Params>() {

    object Params

    override suspend fun run(params: Params): Either<Failure, None> {

        logger.logRaw("************************************************************")
        logger.log(resourcesProvider.getString(R.string.log_app_start_mask, context.getAppVersionName().orEmpty()), false)

        if (settingsManager.isCopiedFromFree()) {
            logger.log(R.string.log_settings_copied_from_free, show = true)
        }

        createDefaultFolders()

        settingsManager.init()

        return None.toRight()
    }

    private fun createDefaultFolders() {
        createFolder(
            path = settingsManager.getDefaultTrashPath(),
            name = Constants.TRASH_DIR_NAME
        )
        createFolder(
            path = settingsManager.getDefaultLogPath(),
            name = Constants.LOG_DIR_NAME
        )
    }

    private fun createFolder(path: String, name: String) {
        try {
            val dir = File(path)
            when (FileUtils.createDirsIfNeed(dir)) {
                1 -> logger.log(resourcesProvider.getString(R.string.log_created_folder_mask, name, path), show = false)
                -1 -> logger.logError(resourcesProvider.getString(R.string.error_create_folder_in_path_mask, name, path), show = false)
                else -> {}
            }
        } catch (ex: Exception) {
            logger.logError(ex, false)
        }
    }


}
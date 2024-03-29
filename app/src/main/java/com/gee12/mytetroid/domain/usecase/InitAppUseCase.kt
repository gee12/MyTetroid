package com.gee12.mytetroid.domain.usecase

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileType
import com.anggrayudi.storage.file.makeFolder
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getAppVersionName
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath

/**
 * Первоначальная инициализация компонентов приложения.
 */
class InitAppUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val settingsManager: CommonSettingsManager,
    private val appPathProvider: IAppPathProvider,
) : UseCase<UseCase.None, InitAppUseCase.Params>() {

    object Params

    override suspend fun run(params: Params): Either<Failure, None> {

        settingsManager.init()

        logger.init(
            path = appPathProvider.getPathToLogsFolder().fullPath,
            isWriteToFile = settingsManager.isWriteLogToFile()
        )
        logger.logRaw("************************************************************")
        logger.log(resourcesProvider.getString(R.string.log_app_start_mask, context.getAppVersionName().orEmpty()), false)

        if (settingsManager.isCopiedFromFree()) {
            logger.log(R.string.log_settings_copied_from_free, show = true)
        }

        createDefaultFolders()

        return None.toRight()
    }

    private fun createDefaultFolders() {
        runBeforeLeft(
            { createFolder(path = appPathProvider.getPathToTrashFolder()) },
            { createFolder(path = appPathProvider.getPathToLogsFolder()) },
            /*{
                // на API >= 30 автоматически каталог /Downloads/mytetroid создать нельзя
                if (Build.VERSION.SDK_INT >= 30) {
                    createFolder(path = appPathProvider.getPathToDownloadsFolder())
                } else {
                    None.toRight()
                }
            },*/
        ).onFailure {
            logger.logFailure(it, show = true)
        }
    }

    private fun createFolder(path: FilePath.Folder): Either<Failure, DocumentFile> {
        return try {
            var folder = DocumentFileCompat.fromFullPath(
                context = context,
                fullPath = path.fullPath,
                documentType = DocumentFileType.FOLDER,
                requiresWriteAccess = true,
            )

            if (folder == null || !folder.exists()) {

                val parentFolder = DocumentFileCompat.fromFullPath(
                    context = context,
                    fullPath = path.path,
                    documentType = DocumentFileType.FOLDER,
                    requiresWriteAccess = true,
                ) ?: return Failure.Folder.Get(FilePath.FolderFull(path.path)).toLeft()

                folder = parentFolder.makeFolder(
                    context,
                    name = path.folderName,
                    mode = CreateMode.REUSE
                ) ?: return Failure.Folder.Create(path).toLeft()

                logger.log(resourcesProvider.getString(R.string.log_created_folder_mask, path.fullPath), show = false)
            }

            folder.toRight()
        } catch (ex: Exception) {
            Failure.Folder.Create(path, ex).toLeft()
        }
    }


}
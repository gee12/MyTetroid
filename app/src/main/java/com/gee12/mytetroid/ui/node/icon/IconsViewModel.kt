package com.gee12.mytetroid.ui.node.icon

import android.app.Application
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.domain.provider.CommonSettingsProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.usecase.node.icon.GetIconsFoldersUseCase
import com.gee12.mytetroid.domain.usecase.node.icon.GetIconsFromFolderUseCase
import com.gee12.mytetroid.ui.base.BaseStorageViewModel
import java.io.File

class IconsViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
    private val getIconsFoldersUseCase: GetIconsFoldersUseCase,
    private val getIconsFromFolderUseCase: GetIconsFromFolderUseCase,
) : BaseStorageViewModel(
    app = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    commonSettingsProvider = commonSettingsProvider,
    storageProvider = storageProvider,
) {

    lateinit var nodeId: String

    override fun startInitStorageFromBase(storageId: Int) {}

    override fun isStorageCrypted() = storageProvider.isExistCryptedNodes()

    fun init(nodeId: String, currentIconPath: String) {
        this.nodeId = nodeId

        launchOnMain {
            getIconsFolders()
                .map { selectCurrentIcon(currentIconPath) }
        }
    }

    private suspend fun getIconsFolders(): Either<Failure, Unit> {
        return withIo {
            getIconsFoldersUseCase.run(
                GetIconsFoldersUseCase.Params
            )
        }.onFailure {
            logFailure(it)
        }.onSuccess { folders ->
            sendEvent(IconsEvent.IconsFolders(folders))
        }.map { Unit }
    }

    private fun selectCurrentIcon(iconPath: String) {
        launchOnIo {
            val pathParts = iconPath.split(File.separator).toTypedArray()
            if (pathParts.size >= 2) {
                val name = pathParts[pathParts.size - 1]
                val folder = pathParts[pathParts.size - 2]
                withMain {
                    sendEvent(IconsEvent.CurrentIcon(icon = TetroidIcon(folder, name)))
                }
            }
        }
    }

    fun getIconsFromFolder(folderName: String) {
        launchOnMain {
            withIo {
                getIconsFromFolderUseCase.run(
                    GetIconsFromFolderUseCase.Params(folderName)
                )
            }.onFailure {
                logFailure(it)
                sendEvent(IconsEvent.IconsFromFolder(folderName, icons = null))
            }.onSuccess { icons ->
                sendEvent(IconsEvent.IconsFromFolder(folderName, icons))
            }
        }
    }

    fun loadIconIfNeed(icon: TetroidIcon) {
        if (icon.icon == null) {
            try {
                val pathToIcons = storagePathProvider.getPathToIcons()
                val fullFileName = makePath(pathToIcons, icon.folder, icon.name)
                icon.icon = FileUtils.loadSVGFromFile(fullFileName)
            } catch (ex: Exception) {
                logger.logError(ex, show = true)
            }
        }
    }

}
package com.gee12.mytetroid.ui.node.icon

import android.app.Application
import android.graphics.drawable.Drawable
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.usecase.image.LoadDrawableFromFileUseCase
import com.gee12.mytetroid.domain.usecase.node.icon.GetIconsFolderNamesUseCase
import com.gee12.mytetroid.domain.usecase.node.icon.GetNodesIconsFromFolderUseCase
import com.gee12.mytetroid.ui.base.BaseStorageViewModel
import java.io.File

class IconsViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    storageProvider: IStorageProvider,
    storagePathProvider: IStoragePathProvider,
    private val getIconsFolderNamesUseCase: GetIconsFolderNamesUseCase,
    private val getNodesIconsFromFolderUseCase: GetNodesIconsFromFolderUseCase,
    private val loadDrawableFromFileUseCase: LoadDrawableFromFileUseCase,
) : BaseStorageViewModel(
    app = app,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
    storageProvider = storageProvider,
    storagePathProvider = storagePathProvider,
) {

    lateinit var nodeId: String

    override fun startInitStorageFromBase(storageId: Int) {}

    override fun isStorageEncrypted() = storageProvider.isExistCryptedNodes()

    fun init(nodeId: String, currentIconPath: String) {
        this.nodeId = nodeId

        launchOnMain {
            getIconsFolders()
                .map { selectCurrentIcon(currentIconPath) }
        }
    }

    private suspend fun getIconsFolders(): Either<Failure, Unit> {
        return withIo {
            getIconsFolderNamesUseCase.run(
                GetIconsFolderNamesUseCase.Params
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
            sendEvent(IconsEvent.LoadIconsFromFolder.InProcess)
            withIo {
                getNodesIconsFromFolderUseCase.run(
                    GetNodesIconsFromFolderUseCase.Params(folderName)
                )
            }.onFailure { failure ->
                logFailure(failure)
                sendEvent(IconsEvent.LoadIconsFromFolder.Failed(folderName, failure))
            }.onSuccess { icons ->
                sendEvent(IconsEvent.LoadIconsFromFolder.Success(folderName, icons))
            }
        }
    }

    fun loadIconIfNeed(icon: TetroidIcon, onLoadedCallback: (Drawable) -> Unit) {
        launchOnIo {
            if (icon.icon == null) {
                try {
                    icon.icon = if (!icon.name.isNullOrEmpty()) {
                        loadDrawableFromFileUseCase.run(
                            LoadDrawableFromFileUseCase.Params(
                                relativeIconPath = makePath(icon.folder, icon.name)
                            )
                        ).foldResult(
                            onLeft = { null },
                            onRight = { it }
                        )
                    } else {
                        null
                    }
                } catch (ex: Exception) {
                    logger.logError(ex, show = false)
                }
            }
            withMain {
                icon.icon?.let {
                    onLoadedCallback(it)
                }
            }
        }
    }

}
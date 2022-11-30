package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.usecase.node.icon.GetIconsFoldersUseCase
import com.gee12.mytetroid.usecase.node.icon.GetIconsFromFolderUseCase
import java.io.File

class IconsViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
    private val storagePathHelper: IStoragePathHelper,
    private val getIconsFoldersUseCase: GetIconsFoldersUseCase,
    private val getIconsFromFolderUseCase: GetIconsFromFolderUseCase,
) : BaseViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
) {

    sealed class IconsEvent : ViewEvent() {
        data class IconsFolders(val folders: List<String>) : IconsEvent()
        data class IconsFromFolder(val folder: String, val icons: List<TetroidIcon>?) : IconsEvent()
        data class CurrentIcon(val icon: TetroidIcon) : IconsEvent()
    }

    lateinit var nodeId: String

    private val pathToIcons: String
        get() = storagePathHelper.getPathToIcons()

    fun init(nodeId: String, currentIconPath: String) {
        this.nodeId = nodeId

        launchOnMain {
            getIconsFolders()
            selectCurrentIcon(currentIconPath)
        }
    }

    private suspend fun getIconsFolders() {
        withIo {
            getIconsFoldersUseCase.run(
                GetIconsFoldersUseCase.Params(
                    pathToIconsFolder = pathToIcons,
                )
            )
        }.onFailure {
            logFailure(it)
        }.onSuccess { folders ->
            if (folders.isEmpty()) {
                logWarning(getString(R.string.log_icons_dir_absent_mask, Constants.ICONS_DIR_NAME), show = true)
            } else {
                sendViewEvent(IconsEvent.IconsFolders(folders))
            }
        }
    }

    private suspend fun selectCurrentIcon(iconPath: String) {
        withIo {
            val pathParts = iconPath.split(File.separator).toTypedArray()
            if (pathParts.size >= 2) {
                val name = pathParts[pathParts.size - 1]
                val folder = pathParts[pathParts.size - 2]
                sendViewEvent(
                    IconsEvent.CurrentIcon(icon = TetroidIcon(folder, name))
                )
            }
        }
    }

    fun getIconsFromFolder(folderName: String) {
        launchOnMain {
            withIo {
                getIconsFromFolderUseCase.run(
                    GetIconsFromFolderUseCase.Params(
                        pathToIcons = pathToIcons,
                        folderName = folderName,
                    )
                )
            }.onFailure {
                logFailure(it)
                sendViewEvent(IconsEvent.IconsFromFolder(folderName, icons = null))
            }.onSuccess { icons ->
                sendViewEvent(IconsEvent.IconsFromFolder(folderName, icons))
            }
        }
    }

    fun loadIconIfNeed(icon: TetroidIcon) {
        if (icon.icon == null) {
            try {
                val fullFileName = makePath(pathToIcons, icon.folder, icon.name)
                icon.icon = FileUtils.loadSVGFromFile(fullFileName)
            } catch (ex: Exception) {
                logger.logError(ex, show = true)
            }
        }
    }

}
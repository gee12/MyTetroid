package com.gee12.mytetroid.ui.file

import android.app.Application
import android.os.Environment
import com.anggrayudi.storage.file.isWritable
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.ui.base.BaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.Collections
import kotlin.coroutines.CoroutineContext

class FolderPickerViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
) : BaseViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    private var comparatorAscending = java.util.Comparator<FileItem> { (name1), (name2) ->
        name1.compareTo(name2)
    }

    private lateinit var currentFolder: File
    private var isPickFiles = false
    private var isEmptyFolderOnly = false

    val currentPath: String
        get() = currentFolder.absolutePath

    fun init(
        isPickFiles: Boolean,
        isEmptyFolderOnly: Boolean,
        initialPath: String,
    ) {
        this.isPickFiles = isPickFiles
        this.isEmptyFolderOnly = isEmptyFolderOnly

        moveToFolder(folder = File(initialPath))
    }

    fun moveToFolder(folder: File) {
        launchOnMain {
            var failures = emptyList<Failure>()
            val checkedFolder = withIo {
                failures = checkFolder(folder)
                val isFolderValid = failures.all {
                    it !is Failure.Folder.NotExist
                            && it !is Failure.Folder.NotFolder
                }
                if (isFolderValid) {
                    folder
                } else {
                    getExternalStorageFolder().also {
                        failures = checkFolder(it)
                    }
                }
            }
            sendEvent(
                FolderPickerEvent.LoadFolder(
                    path = checkedFolder.absolutePath,
                    items = getFolderFiles(checkedFolder),
                    failures = failures,
                )
            )
            currentFolder = checkedFolder
        }
    }

    private fun getFolderFiles(folder: File): List<FileItem> {
        val resultList = mutableListOf<FileItem>()
        val foldersList = mutableListOf<FileItem>()
        val filesList = mutableListOf<FileItem>()

        val files = folder.listFiles() ?: emptyArray()
        files.forEach {
            if (it.isDirectory) {
                foldersList.add(FileItem(name = it.name, isFolder = true))
            } else {
                filesList.add(FileItem(name = it.name, isFolder = false))
            }
        }

        val isParentFolderValid = folder.parentFile?.let { parent ->
            checkFolder(parent).all { it !is Failure.Folder.NotExist }
        } ?: false
        if (isParentFolderValid) {
            resultList.add(FileItem(name = "...", isFolder = true, isMoveUp = true))
        }

        Collections.sort(foldersList, comparatorAscending)
        resultList.addAll(foldersList)

        Collections.sort(filesList, comparatorAscending)
        resultList.addAll(filesList)

        return resultList
    }

    private fun checkFolder(file: File): List<Failure> {
        val path = FilePath.FolderFull(file.absolutePath)
        return buildList {
            if (!file.exists()) {
                add(Failure.Folder.NotExist(path))
            }
            if (!isPickFiles && !file.isDirectory) {
                add(Failure.Folder.NotFolder(path))
            }
            if (!isPickFiles && !isFolderWritable(file)) {
                add(Failure.Folder.Write(path))
            }
            if (!isPickFiles && isEmptyFolderOnly && !isFolderEmpty(file)) {
                add(Failure.Folder.NotEmpty(path))
            }
        }
    }

    fun onSelectFileItem(position: Int, item: FileItem) {
        if (position == 0 && item.isMoveUp) {
            moveUp()
        } else {
            val folder = File(currentFolder, item.name)
            if (!item.isFolder) {
                if (isPickFiles) {
                    launchOnMain {
                        sendEvent(FolderPickerEvent.SelectPathAndExit(path = folder.absolutePath))
                    }
                }
            } else {
                moveToFolder(folder)
            }
        }
    }

    fun moveToHome() {
        moveToFolder(folder = getExternalStorageFolder())
    }

    fun moveUp() {
        val parent = currentFolder.parentFile
        if (parent != null) {
            val isParentFolderValid = checkFolder(parent).all {
                it !is Failure.Folder.NotExist
            }
            if (isParentFolderValid) {
                moveToFolder(parent)
            }
        } else {
            launchOnMain {
                sendEvent(FolderPickerEvent.Exit)
            }
        }
    }

    fun createNewFolder(folderName: String) {
        launchOnMain {
            val folderPath = FilePath.Folder(currentFolder.absolutePath, folderName)
            try {
                val folder = File(currentFolder, folderName)
                val isFolderCreated = withIo {
                    folder.mkdirs()
                }
                if (!isFolderCreated) {
                    sendEvent(
                        FolderPickerEvent.ShowErrorMessage(
                            getString(R.string.error_create_folder_mask, folder.absolutePath)
                        )
                    )
                }
                moveToFolder(currentFolder)
            } catch (ex: Exception) {
                ex.printStackTrace()
                sendEvent(
                    FolderPickerEvent.ShowErrorMessage(
                        failureHandler.getFailureMessage(
                            Failure.Folder.Create(folderPath, ex)
                        ).getFullMessage()
                    )
                )
            }
        }
    }

    fun onApply() {
        launchOnMain {
            when {
                isPickFiles -> {
                    sendEvent(
                        FolderPickerEvent.ShowErrorMessage(
                            resourcesProvider.getString(R.string.error_select_file)
                        )
                    )
                }
                !isPickFiles && !isFolderWritable(currentFolder) -> {
                    sendEvent(
                        FolderPickerEvent.ShowErrorMessage(
                            resourcesProvider.getString(R.string.error_folder_is_not_writable)
                        )
                    )
                }
                isEmptyFolderOnly && !isFolderEmpty(currentFolder) -> {
                    sendEvent(
                        FolderPickerEvent.ShowErrorMessage(
                            resourcesProvider.getString(R.string.error_folder_is_not_empty)
                        )
                    )
                }
                else -> {
                    sendEvent(
                        FolderPickerEvent.SelectPathAndExit(path = currentFolder.absolutePath)
                    )
                }
            }
        }
    }

    private fun isFolderWritable(folder: File): Boolean {
        return folder.isWritable(getContext())
    }

    private fun isFolderEmpty(folder: File): Boolean {
        val childs = folder.listFiles()
        return childs == null || childs.isEmpty()
    }

    private fun getExternalStorageFolder(): File {
        return Environment.getExternalStorageDirectory()
    }

    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

}

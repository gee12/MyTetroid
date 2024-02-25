package com.gee12.mytetroid.ui.scripts

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.extensions.orZero
import com.gee12.mytetroid.common.onFailure
import com.gee12.mytetroid.common.onSuccess
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.GetObjectByTypeAndIdUseCase
import com.gee12.mytetroid.domain.usecase.file.PrepareFileForOpenUseCase
import com.gee12.mytetroid.domain.usecase.file.ReadTextFileUseCase
import com.gee12.mytetroid.domain.usecase.script.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidObject
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.TetroidScriptToObject
import com.gee12.mytetroid.model.enums.DefaultScript
import com.gee12.mytetroid.ui.base.BaseStorageViewModel
import java.io.File

class ScriptsViewModel(
    app: Application,
    buildInfoProvider: BuildInfoProvider,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    failureHandler: IFailureHandler,
    settingsManager: CommonSettingsManager,
    appPathProvider: IAppPathProvider,
    storageProvider: IStorageProvider,
    storagePathProvider: IStoragePathProvider,
    private val getScriptsUseCase: GetScriptsUseCase,
    private val getObjectByTypeAndIdUseCase: GetObjectByTypeAndIdUseCase,
    private val readTextFileUseCase: ReadTextFileUseCase,
    private val getScriptTextUseCase: GetScriptTextUseCase,
    private val saveScriptUseCase: SaveScriptUseCase,
    private val addDefaultScriptUseCase: AddDefaultScriptUseCase,
    private val editScriptUseCase: EditScriptUseCase,
    private val setScriptIsEnabledUseCase: SetScriptIsEnabledUseCase,
    private val setScriptToObjectIsEnabledUseCase: SetScriptToObjectIsEnabledUseCase,
    private val duplicateScriptUseCase: DuplicateScriptUseCase,
    private val deleteScriptFileUseCase: DeleteScriptFileUseCase,
    private val prepareFileForOpenUseCase: PrepareFileForOpenUseCase,
) : BaseStorageViewModel(
    app = app,
    buildInfoProvider = buildInfoProvider,
    resourcesProvider = resourcesProvider,
    logger = logger,
    notificator = notificator,
    failureHandler = failureHandler,
    settingsManager = settingsManager,
    appPathProvider = appPathProvider,
    storageProvider = storageProvider,
    storagePathProvider = storagePathProvider,
) {

    private var scriptObject: TetroidObject? = null
    private var isScriptsChanged = false

    fun init(
        objectTypeId: Int?,
        objectId: String?,
    ) {
        launchOnMain {
            initScriptObject(objectTypeId, objectId)
            loadScripts(isShowDefaultScriptsIfNeed = true)
        }
    }

    private suspend fun initScriptObject(objectTypeId: Int?, objectId: String?) {
        if (objectTypeId != null && !objectId.isNullOrEmpty()) {
            withIo {
                getObjectByTypeAndIdUseCase.run(
                    GetObjectByTypeAndIdUseCase.Params(
                        objectId = objectId,
                        objectTypeId = objectTypeId,
                    )
                ).onFailure {
                    logFailure(failure = it, show = true)
                }.onSuccess {
                    scriptObject = it
                }
            }
        }
    }

    fun loadScripts(
        isShowDefaultScriptsIfNeed: Boolean = false,
        isMoveToLastItem: Boolean = false,
    ) {
        launchOnMain {
            withIo {
                getScriptsUseCase.run(
                    GetScriptsUseCase.Params(scriptObject)
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess { scripts ->
                sendEvent(ScriptsEvent.LoadScripts(scriptObject, scripts, isMoveToLastItem))

                if (scripts.isEmpty() && isShowDefaultScriptsIfNeed) {
                    sendEvent(ScriptsEvent.ShowRequestForDefaultScripts)
                }
            }
        }
    }

    fun openScriptForEdit(script: TetroidScript) {
        launchOnMain {
            val scriptText = withIo {
                getScriptTextUseCase.run(
                    GetScriptTextUseCase.Params(script)
                ).foldResult(
                    onLeft = { null },
                    onRight = { it }
                )
            }
            sendEvent(ScriptsEvent.ShowScriptDialog(
                script = script,
                scriptText = scriptText.orEmpty(),
                isNew = false,
            ))
        }
    }

    fun addNewScript(fileName: String, description: String, scriptText: String) {
        launchOnMain {
            withIo {
                saveScriptUseCase.run(
                    SaveScriptUseCase.Params(
                        fileName = fileName,
                        description = description,
                        scriptText = scriptText,
                    )
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                isScriptsChanged = true
                logOperRes(LogObj.SCRIPT, LogOper.ADD)
                loadScripts(isMoveToLastItem = true)
            }
        }
    }

    fun addNewScriptFromFile(scriptFile: DocumentFile) {
        launchOnMain {
            withIo {
                readTextFileUseCase.run(
                    ReadTextFileUseCase.Params(scriptFile)
                )
            }.onFailure {
                 logFailure(failure = it, show = true)
            }.onSuccess { scriptText ->
               sendEvent(ScriptsEvent.ShowScriptDialog(
                   script = TetroidScript(
                       storageId = storageProvider.storage?.id.orZero(),
                       fileName = scriptFile.name.orEmpty(),
                       description = null,
                   ),
                   scriptText = scriptText,
                   isNew = true,
               ))
            }
        }
    }

    fun addDefaultScript(script: DefaultScript) {
        launchOnMain {
            withIo {
                addDefaultScriptUseCase.run(
                    AddDefaultScriptUseCase.Params(script)
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                isScriptsChanged = true
                logOperRes(LogObj.SCRIPT, LogOper.ADD)
                loadScripts(isMoveToLastItem = true)
            }
        }
    }

    fun setScriptIsActiveForCurrentObject(script: TetroidScript, isActive: Boolean) {
        setScriptIsActive(script, isActive, obj = scriptObject)
    }

    fun setScriptIsActiveForAllStorage(script: TetroidScript, isActive: Boolean) {
        setScriptIsActive(script, isActive, obj = null)
    }

    private fun setScriptIsActive(script: TetroidScript, isActive: Boolean, obj: TetroidObject?) {
        launchOnMain {
            withIo {
                setScriptIsEnabledUseCase.run(
                    SetScriptIsEnabledUseCase.Params(
                        script = script,
                        obj = obj,
                        isActive = isActive,
                    )
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                isScriptsChanged = true
                loadScripts()
            }
        }
    }

    fun setScriptToObjectEnabled(scriptToObject: TetroidScriptToObject, isEnabled: Boolean) {
        launchOnMain {
            withIo {
                setScriptToObjectIsEnabledUseCase.run(
                    SetScriptToObjectIsEnabledUseCase.Params(
                        scriptToObject = scriptToObject,
                        isEnabled = isEnabled,
                    )
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                isScriptsChanged = true
                loadScripts()
            }
        }
    }

    fun editScript(
        script: TetroidScript,
        fileName: String,
        description: String?,
        scriptText: String,
    ) {
        launchOnMain {
            withIo {
                editScriptUseCase.run(
                    EditScriptUseCase.Params(
                        script = script,
                        fileName = fileName,
                        description = description,
                        scriptText = scriptText,
                    )
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                isScriptsChanged = true
                logOperRes(LogObj.SCRIPT, LogOper.CHANGE)
                loadScripts()
            }
        }
    }

    fun duplicateScript(script: TetroidScript) {
        launchOnMain {
            withIo {
                duplicateScriptUseCase.run(
                    DuplicateScriptUseCase.Params(
                        script = script,
                    )
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                isScriptsChanged = true
                logOperRes(LogObj.SCRIPT, LogOper.ADD)
                loadScripts(isMoveToLastItem = true)
            }
        }
    }

    fun deleteScript(script: TetroidScript, withFile: Boolean) {
        launchOnMain {
            withIo {
                deleteScriptFileUseCase.run(
                    DeleteScriptFileUseCase.Params(
                        script = script,
                        withFile = withFile,
                    )
                )
            }.onFailure {
                logFailure(failure = it, show = true)
            }.onSuccess {
                isScriptsChanged = true
                logOperRes(LogObj.SCRIPT, LogOper.DELETE)
                loadScripts()
            }
        }
    }

    fun prepareScriptFileForOpen(script: TetroidScript) {
        launchOnMain {
            withIo {
                val scriptsFolderPath = storagePathProvider.getPathToScriptsFolder()
                val scriptFileName = makePath(scriptsFolderPath, script.fileName)
                prepareFileForOpenUseCase.run(
                    PrepareFileForOpenUseCase.Params(
                        file = File(scriptFileName)
                    )
                )
            }.onFailure {
                logFailure(it, show = true)
            }.onSuccess { result ->
                sendEvent(
                    ScriptsEvent.OpenScriptFile(
                        uri = result.uri,
                        mimeType = result.mimeType,
                    )
                )
            }
        }
    }

    fun isScriptForObject(): Boolean {
        return scriptObject != null
    }

    fun isScriptsChanged(): Boolean {
        return isScriptsChanged
    }

}
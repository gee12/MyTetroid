package com.gee12.mytetroid.ui.search

import android.app.Application
import com.gee12.mytetroid.R
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.SearchProfile
import com.gee12.mytetroid.model.obj.TetroidNode
import com.gee12.mytetroid.model.enums.SearchInNodeMode
import com.gee12.mytetroid.ui.base.BaseStorageViewModel

class SearchViewModel(
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
    private val commonSettingsManager: CommonSettingsManager,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
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
    private var currentNodeId: String? = null // текущая открытая ветка в главном окне
    private var selectedNode: TetroidNode? = null // выбранная ветка для поиска

    fun initStorage(nodeId: String?) {
        this.currentNodeId = nodeId

        launchOnMain {
            val searchProfile = withIo {
                buildSearchProfileFromPrefs().also { profile ->
                    selectedNode = getNodeByMode(
                        searchInNodeMode = profile.searchInNodeMode
                    )
                    profile.node = selectedNode
                }
            }
            sendEvent(SearchEvent.Init(searchProfile))
        }
    }

    private suspend fun getNodeByMode(searchInNodeMode: SearchInNodeMode): TetroidNode? {
        val nodeId = when (searchInNodeMode) {
            SearchInNodeMode.NONE -> {
                selectedNode?.id
            }
            SearchInNodeMode.IN_CURRENT_NODE -> {
                currentNodeId
            }
            SearchInNodeMode.IN_SELECTED_NODE -> {
                selectedNode?.id ?: CommonSettings.getSearchNodeId(getContext()) ?: currentNodeId
            }
        }
        return nodeId?.let {
            getNodeByIdUseCase.run(
                GetNodeByIdUseCase.Params(nodeId)
            ).foldResult(
                onLeft = {
                    logFailure(it, show = false)
                    null
                },
                onRight = { it }
            )
        }
    }

    fun selectSearchInNodeMode(searchInNodeMode: SearchInNodeMode) {
        launchOnMain {
            selectedNode = withIo {
                getNodeByMode(searchInNodeMode)
            }
            sendEvent(SearchEvent.ChangeSelectedNode(node = selectedNode, searchInNodeMode))
        }
    }

    fun selectNode(node: TetroidNode) {
        selectedNode = node
        launchOnMain {
            sendEvent(SearchEvent.ChangeSelectedNode(node = selectedNode, searchInNodeMode = SearchInNodeMode.IN_SELECTED_NODE))
        }
    }

    fun checkValuesAndFinish(searchProfile: SearchProfile) {
        when {
            searchProfile.query.isEmpty() -> {
                showMessage(R.string.title_enter_query)
            }
            searchProfile.searchInNodeMode == SearchInNodeMode.IN_CURRENT_NODE && selectedNode == null -> {
                showMessage(R.string.log_cur_node_is_not_selected)
            }
            searchProfile.searchInNodeMode == SearchInNodeMode.IN_SELECTED_NODE && selectedNode == null -> {
                showMessage(R.string.log_select_node_to_search)
            }
            else -> {
                saveSearchProfileAndFinish(searchProfile)
            }
        }
    }

    private fun saveSearchProfileAndFinish(searchProfile: SearchProfile) {
        searchProfile.nodeId = selectedNode?.id
        // сохраняем параметры поиска
        savePrefsFromSearchProfile(searchProfile)
        launchOnMain {
            sendEvent(SearchEvent.Finish(searchProfile))
        }
    }

    private fun buildSearchProfileFromPrefs(): SearchProfile {
        val context = getContext()
        return SearchProfile(
            query = CommonSettings.getSearchQuery(context).orEmpty(),
            inRecordText = CommonSettings.isSearchInText(context),
            inRecordName = CommonSettings.isSearchInRecordsNames(context),
            inRecordAuthor = CommonSettings.isSearchInAuthor(context),
            inRecordUrl = CommonSettings.isSearchInUrl(context),
            inRecordTags = CommonSettings.isSearchInTags(context),
            inRecordFolderName = commonSettingsManager.isSearchInRecordFolderName(),
            inNodeName = CommonSettings.isSearchInNodes(context),
            inAttachName = CommonSettings.isSearchInFiles(context),
            inObjectsId = CommonSettings.isSearchInIds(context),
            isSplitToWords = CommonSettings.isSearchSplitToWords(context),
            isOnlyWholeWords = CommonSettings.isSearchInWholeWords(context),
            searchInNodeMode = CommonSettings.getSearchInNodeMode(context),
            nodeId = CommonSettings.getSearchNodeId(context),
        )
    }

    private fun savePrefsFromSearchProfile(searchProfile: SearchProfile) {
        val context = getContext()
        CommonSettings.setSearchQuery(context, searchProfile.query)
        CommonSettings.setSearchInText(context, searchProfile.inRecordText)
        CommonSettings.setSearchInRecordsNames(context, searchProfile.inRecordName)
        CommonSettings.setSearchInAuthor(context, searchProfile.inRecordAuthor)
        CommonSettings.setSearchInUrl(context, searchProfile.inRecordUrl)
        CommonSettings.setSearchInTags(context, searchProfile.inRecordTags)
        commonSettingsManager.setSearchInRecordFolderName(searchProfile.inRecordFolderName)
        CommonSettings.setSearchInNodes(context, searchProfile.inNodeName)
        CommonSettings.setSearchInFiles(context, searchProfile.inAttachName)
        CommonSettings.setSearchInIds(context, searchProfile.inObjectsId)
        CommonSettings.setSearchSplitToWords(context, searchProfile.isSplitToWords)
        CommonSettings.setSearchInWholeWords(context, searchProfile.isOnlyWholeWords)
        CommonSettings.setSearchInNodeMode(context, searchProfile.searchInNodeMode)
        CommonSettings.setSearchNodeId(context, selectedNode?.id)
    }

}

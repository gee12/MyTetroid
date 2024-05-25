package com.gee12.mytetroid.ui.search

import com.gee12.mytetroid.model.SearchProfile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.enums.SearchInNodeMode
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class SearchEvent : BaseEvent() {
    data class Init(val searchProfile: SearchProfile) : SearchEvent()
    data class ChangeSelectedNode(
        val node: TetroidNode?,
        val searchInNodeMode: SearchInNodeMode,
    ) : SearchEvent()
    data class Finish(val searchProfile: SearchProfile) : SearchEvent()
}
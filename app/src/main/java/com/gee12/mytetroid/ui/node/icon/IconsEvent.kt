package com.gee12.mytetroid.ui.node.icon

import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class IconsEvent : BaseEvent() {
    data class IconsFolders(val folders: List<String>) : IconsEvent()
    sealed class LoadIconsFromFolder : IconsEvent() {
        object InProcess : LoadIconsFromFolder()
        data class Success(val folder: String, val icons: List<TetroidIcon>?) : LoadIconsFromFolder()
        data class Failed(val folder: String, val failure: Failure) : LoadIconsFromFolder()
    }
    data class CurrentIcon(val icon: TetroidIcon) : IconsEvent()
}
package com.gee12.mytetroid.ui.node.icon

import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.ui.base.BaseEvent

sealed class IconsEvent : BaseEvent() {
    data class IconsFolders(val folders: List<String>) : IconsEvent()
    data class IconsFromFolder(val folder: String, val icons: List<TetroidIcon>?) : IconsEvent()
    data class CurrentIcon(val icon: TetroidIcon) : IconsEvent()
}
package com.gee12.mytetroid.model

import com.gee12.mytetroid.R
import com.gee12.mytetroid.domain.provider.IResourcesProvider

sealed class QuicklyNode {
    object IsNotSet : QuicklyNode()
    object NeedLoadStorage : QuicklyNode()
    object NeedLoadAllNodes : QuicklyNode()
    data class NotFound(val nodeId: String) : QuicklyNode()
    data class NotDecrypted(val node: TetroidNode) : QuicklyNode()
    data class Loaded(val node: TetroidNode) : QuicklyNode()

    fun getName(resourcesProvider: IResourcesProvider): String {
        return when (this) {
            is IsNotSet -> resourcesProvider.getString(R.string.hint_node_is_not_set)
            NeedLoadStorage -> resourcesProvider.getString(R.string.hint_need_load_storage)
            NeedLoadAllNodes -> resourcesProvider.getString(R.string.hint_need_load_all_nodes)
            is NotFound -> resourcesProvider.getString(R.string.hint_node_not_found)
            is NotDecrypted -> resourcesProvider.getString(R.string.hint_node_not_decrypted)
            is Loaded -> node.name
        }
    }

    fun getLoadedNodeOrNull(): TetroidNode? {
        return if (this is Loaded) {
            node
        } else {
            null
        }
    }

}

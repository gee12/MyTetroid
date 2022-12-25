package com.gee12.mytetroid.ui.main

import android.os.Bundle
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.ui.storage.StorageEvent

sealed class MainEvent : StorageEvent() {
    // migration
    object Migrated : MainEvent()

    sealed class Node(val node: TetroidNode) : MainEvent() {
        class Encrypt(node: TetroidNode) : Node(node)
        class DropEncrypt(node: TetroidNode) : Node(node)
        class Show(node: TetroidNode) : Node(node)
        class Created(node: TetroidNode) : Node(node)
        class Inserted(node: TetroidNode) : Node(node)
        class Renamed(node: TetroidNode) : Node(node)
        class AskForDelete(node: TetroidNode) : Node(node)
        class Cutted(node: TetroidNode) : Node(node)
        class Deleted(node: TetroidNode) : Node(node)
    }
    data class SetCurrentNode(
        val node: TetroidNode?
    ) : MainEvent()
    object UpdateNodes : MainEvent()

    sealed class Record : MainEvent() {
        data class Open(
            val bundle: Bundle,
        ) : MainEvent()
        data class Deleted(
            val record: TetroidRecord,
        ) : MainEvent()
        data class Cutted(
            val record: TetroidRecord,
        ) : MainEvent()
    }
    data class ShowRecords(
        val records: List<TetroidRecord>,
        val viewId: Int,
        val dropSearch: Boolean = true,
    ) : MainEvent()
    data class RecordsFiltered(
        val query: String,
        val records: List<TetroidRecord>,
        val viewId: Int,
    ) : MainEvent()
    object UpdateRecords : MainEvent()

    // tags
    sealed class Tags : MainEvent() {
        object UpdateTags : Tags()
        data class ReloadTags(val tagsMap: Map<String, TetroidTag>) : Tags()
        data class UpdateSelectedTags(
            val selectedTags: List<TetroidTag>,
            val isMultiTagsMode: Boolean,
        ) : Tags()
    }

    // attaches
    data class ShowAttaches(
        val attaches: List<TetroidFile>,
    ) : MainEvent()
    data class AttachesFiltered(
        val query: String,
        val attaches: List<TetroidFile>,
        val viewId: Int,
    ) : MainEvent()
    object UpdateAttaches : MainEvent()
    data class AttachDeleted(
        val attach: TetroidFile,
    ) : MainEvent()

    // favorites
    object UpdateFavoritesTitle : MainEvent()

    // global search
    data class GlobalSearchStart(
        val query: String?,
    ) : MainEvent()
    object GlobalResearch : MainEvent()
    data class GlobalSearchFinished(
        val found: Map<ITetroidObject, FoundType>,
        val profile: SearchProfile,
    ) : MainEvent()

    // file system
    data class AskForOperationWithoutFolder(
        val clipboardParams: ClipboardParams,
    ) : MainEvent()
    data class AskForOperationWithoutFile(
        val clipboardParams: ClipboardParams,
    ) : MainEvent()
    object OpenFilePicker : MainEvent()
    object OpenFolderPicker : MainEvent()

    object Exit : MainEvent()
}
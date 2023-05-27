package com.gee12.mytetroid.ui.main

import android.os.Bundle
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.ui.storage.StorageEvent

sealed class MainEvent(
    val startTaskEvent: Boolean = false,
    val endTaskEvent: Boolean = false,
) : StorageEvent() {
    // migration
    object Migrated : MainEvent()

    sealed class Node(
        val node: TetroidNode,
        startTaskEvent: Boolean = false,
        endTaskEvent: Boolean = false,
    ) : MainEvent(startTaskEvent, endTaskEvent) {

        class Encrypt(node: TetroidNode) : Node(node)
        class DropEncrypt(node: TetroidNode) : Node(node)
        class Show(node: TetroidNode) : Node(node)
        class Created(node: TetroidNode) : Node(node)
        sealed class Insert(
            node: TetroidNode,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Node(node, startTaskEvent, endTaskEvent) {
            class InProcess(node: TetroidNode) : Insert(node, startTaskEvent = true)
            class Failed(node: TetroidNode, failure: Failure) : Insert(node, endTaskEvent = true)
            class Success(node: TetroidNode) : Insert(node)
        }
        class Renamed(node: TetroidNode) : Node(node)
        class AskForDelete(node: TetroidNode) : Node(node)
        sealed class Cut(
            node: TetroidNode,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Node(node, startTaskEvent, endTaskEvent) {
            class InProcess(node: TetroidNode) : Cut(node, startTaskEvent = true)
            class Failed(node: TetroidNode, failure: Failure) : Insert(node, endTaskEvent = true)
            class Success(node: TetroidNode) : Cut(node)
        }
        sealed class Delete(
            node: TetroidNode,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Node(node, startTaskEvent, endTaskEvent) {
            class InProcess(node: TetroidNode) : Delete(node, startTaskEvent = true)
            class Failed(node: TetroidNode, failure: Failure) : Insert(node, endTaskEvent = true)
            class Success(node: TetroidNode) : Delete(node)
        }
        class Reordered(
            node: TetroidNode,
            val flatPosition: Int,
            val positionInNode: Int,
            val newPositionInNode: Int
        ) : Node(node)
    }
    data class SetCurrentNode(
        val node: TetroidNode?
    ) : MainEvent()
    object UpdateNodes : MainEvent()

    sealed class Record : MainEvent() {
        data class Open(
            val recordId: String,
            val bundle: Bundle,
        ) : Record()
        data class Deleted(
            val record: TetroidRecord,
        ) : Record()
        data class Cutted(
            val record: TetroidRecord,
        ) : Record()
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
    data class UpdateRecordsList(
        val records: List<TetroidRecord>,
        val curMainViewId: Int,
    ) : MainEvent()

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
    data class ReloadAttaches(
        val attaches: List<TetroidFile>,
    ) : MainEvent()
    data class AttachDeleted(
        val attach: TetroidFile,
    ) : MainEvent()
    object PickAttach : MainEvent()
    data class PickFolderForAttach(val attach: TetroidFile) : MainEvent()

    // favorites
    object UpdateFavoritesNodeTitle : MainEvent()

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

    object Exit : MainEvent()
}
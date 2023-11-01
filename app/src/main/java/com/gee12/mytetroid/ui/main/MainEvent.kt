package com.gee12.mytetroid.ui.main

import android.os.Bundle
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.ui.storage.StorageEvent

sealed class MainEvent(
    val startTaskEvent: Boolean = false,
    val endTaskEvent: Boolean = false,
) : StorageEvent() {

    // ui
    data class UpdateToolbar(
        val page: PageType,
        val viewType: MainViewType,
        val title: String?,
    ) : MainEvent()

    data class OpenPage(
        val page: PageType,
    ) : MainEvent()
    data class ShowMainView(
        val viewType: MainViewType,
    ) : MainEvent()
    object ClearMainView : MainEvent()
    object CloseFoundView : MainEvent()

    object HandleReceivedIntent : MainEvent()

    // nodes
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
            class Failed(node: TetroidNode, val failure: Failure) : Insert(node, endTaskEvent = true)
            class Success(node: TetroidNode) : Insert(node, endTaskEvent = true)
        }

        class Renamed(node: TetroidNode) : Node(node)

        class AskForDelete(node: TetroidNode) : Node(node)

        sealed class Cut(
            node: TetroidNode,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Node(node, startTaskEvent, endTaskEvent) {
            class InProcess(node: TetroidNode) : Cut(node, startTaskEvent = true)
            class Failed(node: TetroidNode, val failure: Failure) : Cut(node, endTaskEvent = true)
            class Success(node: TetroidNode) : Cut(node, endTaskEvent = true)
        }

        sealed class Delete(
            node: TetroidNode,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Node(node, startTaskEvent, endTaskEvent) {
            class InProcess(node: TetroidNode) : Delete(node, startTaskEvent = true)
            class Failed(node: TetroidNode, val failure: Failure) : Delete(node, endTaskEvent = true)
            class Success(node: TetroidNode) : Delete(node, endTaskEvent = true)
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

    // records
    sealed class Record(
        startTaskEvent: Boolean = false,
        endTaskEvent: Boolean = false,
    ) : MainEvent(startTaskEvent, endTaskEvent) {

        data class Open(
            val recordId: String,
            val bundle: Bundle,
        ) : Record()

        sealed class Create(
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Record(startTaskEvent, endTaskEvent) {
            class InProcess(val name: String) : Create(startTaskEvent = true)
            class Failed(val name: String, val failure: Failure) : Create(endTaskEvent = true)
            class Success(val record: TetroidRecord) : Create(endTaskEvent = true)
        }

        sealed class Insert(
            val record: TetroidRecord,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Record(startTaskEvent, endTaskEvent) {
            class InProcess(record: TetroidRecord) : Insert(record, startTaskEvent = true)
            class Failed(record: TetroidRecord, val failure: Failure) : Insert(record, endTaskEvent = true)
            class Success(record: TetroidRecord) : Insert(record, endTaskEvent = true)
        }

        sealed class Cut(
            val record: TetroidRecord,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Record(startTaskEvent, endTaskEvent) {
            class InProcess(record: TetroidRecord) : Cut(record, startTaskEvent = true)
            class Failed(record: TetroidRecord, val failure: Failure) : Cut(record, endTaskEvent = true)
            class Success(record: TetroidRecord) : Cut(record, endTaskEvent = true)
        }

        sealed class Delete(
            val record: TetroidRecord,
            startTaskEvent: Boolean = false,
            endTaskEvent: Boolean = false,
        ) : Record(startTaskEvent, endTaskEvent) {
            class InProcess(record: TetroidRecord) : Delete(record, startTaskEvent = true)
            class Failed(record: TetroidRecord, val failure: Failure) : Delete(record, endTaskEvent = true)
            class Success(record: TetroidRecord) : Delete(record, endTaskEvent = true)
        }
    }

    data class ShowRecords(
        val records: List<TetroidRecord>,
        val viewType: MainViewType,
        val dropSearch: Boolean = true,
    ) : MainEvent()

    data class RecordsFiltered(
        val query: String,
        val records: List<TetroidRecord>,
        val viewType: MainViewType,
    ) : MainEvent()

    data class UpdateRecordsList(
        val records: List<TetroidRecord>,
        val currentViewType: MainViewType,
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

    sealed class Attach : MainEvent() {
        object OpenPicker : Attach()
        sealed class Open(val  attach: TetroidFile) : Attach() {
            class RequestToEnableDecryptAttachesToTempFolder(attach: TetroidFile) : Open(attach)
            class InProcess(attach: TetroidFile) : Open(attach)
            class Failed(attach: TetroidFile, val failure: Failure) : Open(attach)
            class Success(attach: TetroidFile) : Open(attach)
        }
        sealed class Delete(val  attach: TetroidFile) : Attach() {
            class InProcess(attach: TetroidFile) : Delete(attach)
            class Failed(attach: TetroidFile, val failure: Failure) : Delete(attach)
            class Success(attach: TetroidFile) : Delete(attach)
        }
    }

    // attaches
    data class ShowAttaches(
        val attaches: List<TetroidFile>,
    ) : MainEvent()

    data class AttachesFiltered(
        val query: String,
        val attaches: List<TetroidFile>,
        val viewType: MainViewType,
    ) : MainEvent()

    object UpdateAttaches : MainEvent()

    data class ReloadAttaches(
        val attaches: List<TetroidFile>,
    ) : MainEvent()

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
        val isExistEncryptedNodes: Boolean,
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
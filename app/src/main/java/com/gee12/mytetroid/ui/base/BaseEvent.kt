package com.gee12.mytetroid.ui.base

import android.content.Intent
import android.os.Bundle
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.model.enums.TetroidPermission

abstract class BaseEvent : VMEvent() {
    // activity
    data class StartActivity(
        val intent: Intent,
    ) : BaseEvent()
    data class SetActivityResult(
        val code: Int,
        val bundle: Bundle? = null,
        var intent: Intent? = null,
    ) : BaseEvent()
    object FinishActivity : BaseEvent()
    data class FinishWithResult(
        val code: Int,
        val bundle: Bundle? = null,
        var intent: Intent? = null,
    ) : BaseEvent()

    // ui
    data class InitUI(  // TODO: в MainEvent, убрать из StorageViewModel
        val storage: TetroidStorage,
        var result: Boolean = false, // результат открытия/расшифровки
        var isLoadFavoritesOnly: Boolean, // нужно ли загружать только избранные записи,
        //  или загружены только избранные записи, т.е. в избранном нажали на не расшифрованную запись
        val isHandleReceivedIntent: Boolean, // ужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
        //  или ветку с избранным (если именно она передана в node)
        var isAllNodesLoading: Boolean = false, // загрузка всех веток после режима isLoadedFavoritesOnly
    ) : BaseEvent()
    data class UpdateToolbar(  // TODO: MainEvent
        val viewId: Int,
        val title: String?,
    ) : BaseEvent()
    object UpdateOptionsMenu : BaseEvent()
    object HandleReceivedIntent : BaseEvent()
    data class UpdateTitle(
        val title: String,
    ) : BaseEvent()
    object ShowMoreInLogs : BaseEvent()
    data class ShowHomeButton(
        val isVisible: Boolean,
    ) : BaseEvent()

    // permission
    sealed class Permission : BaseEvent() {
        data class Check(
            val permission: TetroidPermission,
            val requestCode: Int? = null,
        ) : Permission()
        data class Granted(
            val permission: TetroidPermission,
            val requestCode: Int? = null,
        ) : Permission()

        data class Canceled(
            val permission: TetroidPermission,
            val requestCode: Int,
        ) : Permission()

        data class ShowRequest(
            val permission: TetroidPermission,
            val requestCallback: () -> Unit
        ) : Permission()
    }

    // TODO: MainEvent
    // pages
    data class OpenPage(
        val pageId: Int,
    ) : BaseEvent()
    data class ShowMainView(
        val viewId: Int,
    ) : BaseEvent()
    object ClearMainView : BaseEvent()
    object CloseFoundView : BaseEvent()

    // long-term tasks
    data class TaskStarted(
        val titleResId: Int? = null,
    ) : BaseEvent()
    object TaskFinished : BaseEvent()
    data class ShowProgress(
        val isVisible: Boolean,
    ) : BaseEvent()
    data class ShowProgressText(
        val message: String,
    ) : BaseEvent()
}
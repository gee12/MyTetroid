package com.gee12.mytetroid.viewmodels

import android.content.Intent
import android.os.Bundle
import com.gee12.mytetroid.model.TetroidStorage

// TODO: BaseVMEvent - наследовать от него остальные евенты
sealed class ViewEvent : VMEvent() {
    // activity
    data class StartActivity(
        val intent: Intent,
    ) : ViewEvent()
    data class SetActivityResult(
        val code: Int,
        val bundle: Bundle? = null,
        var intent: Intent? = null,
    ) : ViewEvent()
    object FinishActivity : ViewEvent()
    data class FinishWithResult(
        val code: Int,
        val bundle: Bundle? = null,
        var intent: Intent? = null,
    ) : ViewEvent()

    // ui
    data class InitUI(  // TODO: в MainEvent, убрать из StorageViewModel
        val storage: TetroidStorage,
        var result: Boolean = false, // результат открытия/расшифровки
        var isLoadFavoritesOnly: Boolean, // нужно ли загружать только избранные записи,
        //  или загружены только избранные записи, т.е. в избранном нажали на не расшифрованную запись
        val isHandleReceivedIntent: Boolean, // ужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
        //  или ветку с избранным (если именно она передана в node)
        var isAllNodesLoading: Boolean = false, // загрузка всех веток после режима isLoadedFavoritesOnly
    ) : ViewEvent()
    data class UpdateToolbar(  // TODO: MainEvent
        val viewId: Int,
        val title: String?,
    ) : ViewEvent()
    object UpdateOptionsMenu : ViewEvent()
    object HandleReceivedIntent : ViewEvent()
    data class UpdateTitle(
        val title: String,
    ) : ViewEvent()
    object ShowMoreInLogs : ViewEvent()
    data class ShowHomeButton(
        val isVisible: Boolean,
    ) : ViewEvent()

    // permission
    object PermissionCheck : ViewEvent()
    data class PermissionGranted(
        val requestCode: Int,
    ) : ViewEvent()
    data class PermissionCanceled(
        val requestCode: Int,
    ) : ViewEvent()
    data class ShowPermissionRequest(
        val request: PermissionRequestParams,
    ) : ViewEvent()

    // TODO: MainEvent
    // pages
    data class OpenPage(
        val pageId: Int,
    ) : ViewEvent()
    data class ShowMainView(
        val viewId: Int,
    ) : ViewEvent()
    object ClearMainView : ViewEvent()
    object CloseFoundView : ViewEvent()

    // long-term tasks
    data class TaskStarted(
        val titleResId: Int? = null,
    ) : ViewEvent()
    object TaskFinished : ViewEvent()
    data class ShowProgress(
        val isVisible: Boolean,
    ) : ViewEvent()
    data class ShowProgressText(
        val message: String,
    ) : ViewEvent()
}
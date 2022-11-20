package com.gee12.mytetroid.viewmodels

import android.content.Intent
import android.os.Bundle
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage

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
    data class InitGUI(
        val storage: TetroidStorage,
        var result: Boolean = false, // результат открытия/расшифровки
        var isDecrypt: Boolean? = null, // расшифровка хранилища, а не просто открытие
        val node: TetroidNode? = null, // ветка, которую нужно открыть после расшифровки хранилища
        val isNodeOpening: Boolean = false, // если true, значит хранилище уже было загружено, и нажали на еще не расшифрованную ветку
        var isLoadFavoritesOnly: Boolean, // нужно ли загружать только избранные записи,
        //  или загружены только избранные записи, т.е. в избранном нажали на не расшифрованную запись
        val isHandleReceivedIntent: Boolean, // ужно ли после загрузки открыть ветку, сохраненную в опции getLastNodeId()
        //  или ветку с избранным (если именно она передана в node)
        var passHash: String? = null, // хеш пароля
        var fieldName: String? = null, // поле в database.ini
        var isAllNodesLoading: Boolean = false, // загрузка всех веток после режима isLoadedFavoritesOnly
    ) : ViewEvent()
    data class UpdateToolbar(
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
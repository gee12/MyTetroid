package com.gee12.mytetroid.ui.storage

import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidStorage

data class StorageParams(
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
)
package com.gee12.mytetroid

import com.gee12.mytetroid.data.xml.TetroidXml
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.interactors.FavoritesInteractor
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.FileTetroidLogger
import com.gee12.mytetroid.viewmodels.StorageViewModel

/**
 * Среда, окружающая приложение во время его работы.
 */
data class TetroidEnvironment(
    var logger: FileTetroidLogger,
    var storageData: TetroidStorageData? = null
)

class TetroidStorageData(
    var storageId: Int,
    var xmlLoader: TetroidXml,
    var storageCrypter: TetroidCrypter,

    var storageInteractor: StorageInteractor,
    var favoritesInteractor: FavoritesInteractor
) {

    constructor(storageId: Int, viewModel: StorageViewModel): this(
        storageId = storageId,
        xmlLoader = viewModel.xmlLoader,
        storageCrypter = viewModel.storageCrypter,
        storageInteractor = viewModel.storageInteractor,
        favoritesInteractor = viewModel.favoritesInteractor
    )

}
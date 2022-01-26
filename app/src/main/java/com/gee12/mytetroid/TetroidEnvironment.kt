package com.gee12.mytetroid

import com.gee12.mytetroid.data.crypt.ITetroidCrypter
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
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
    var dataProcessor: IStorageDataProcessor,
    var crypter: ITetroidCrypter,

    var storageInteractor: StorageInteractor,
    var favoritesInteractor: FavoritesInteractor
) {

    constructor(storageId: Int, viewModel: StorageViewModel): this(
        storageId = storageId,
        dataProcessor = viewModel.storageDataProcessor,
        crypter = viewModel.storageCrypter,
        storageInteractor = viewModel.storageInteractor,
        favoritesInteractor = viewModel.favoritesInteractor
    )

}
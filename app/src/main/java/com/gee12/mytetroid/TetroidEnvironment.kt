package com.gee12.mytetroid

import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.FileTetroidLogger
import com.gee12.mytetroid.repo.StoragesRepo

/**
 * Среда, окружающая приложение во время его работы.
 */
class TetroidEnvironment(
    var logger: FileTetroidLogger? = null,
    var xmlLoader: TetroidXml? = null,
    var crypter: TetroidCrypter? = null,

    var storagesRepo: StoragesRepo? = null,

    var storageInteractor: StorageInteractor? = null,
)
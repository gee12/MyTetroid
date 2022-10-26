package com.gee12.mytetroid.usecase.storage

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.model.TetroidStorage
import java.io.File
import java.io.FileInputStream

/**
 * Непосредственное чтение структуры хранилища.
 */
class ReadStorageUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val storageProvider: IStorageProvider,
    private val storageDataProcessor: IStorageDataProcessor,
    private val storagePathHelper: IStoragePathHelper,
) : UseCase<UseCase.None, ReadStorageUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
        val isDecrypt: Boolean,
        val isFavoritesOnly: Boolean,
        val isOpenLastNode: Boolean
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storage = params.storage

        // FIXME ?
        //storageProvider.resetStorage()

        return readStorage(
            storage = storage,
            isDecrypt = params.isDecrypt,
            isFavorite = params.isFavoritesOnly
        ).flatMap {
            storageProvider.getRootNode().name = resourcesProvider.getString(R.string.title_root_node)
            storageProvider.setStorage(storage)

            None.toRight()
        }
    }

    /**
     * Загрузка хранилища из файла mytetra.xml.
     */
    private suspend fun readStorage(storage: TetroidStorage, isDecrypt: Boolean, isFavorite: Boolean): Either<Failure, None> {
        return try {
            val myTetraXmlFile = File(storagePathHelper.getPathToMyTetraXml())
            if (!myTetraXmlFile.exists()) {
                storage.isLoaded = false
                return Failure.Storage.Load.XmlFileNotExist(storagePath = storage.path).toLeft()
            }

            @Suppress("BlockingMethodInNonBlockingContext")
            val fis = FileInputStream(myTetraXmlFile)
            // непосредственная обработка xml файла со структурой хранилища
            storageDataProcessor.parse(
                fis = fis,
                isNeedDecrypt = isDecrypt,
                isLoadFavoritesOnly = isFavorite
            )
            storage.isLoaded = true

            None.toRight()
        } catch (ex: Exception) {
            storage.isLoaded = false
            Failure.Storage.Load.ReadXmlFile(storagePath = storage.path, ex).toLeft()
        }
    }

}
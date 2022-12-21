package com.gee12.mytetroid.domain.usecase.storage

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import java.io.File
import java.io.FileInputStream

/**
 * Непосредственное чтение структуры хранилища.
 */
class ReadStorageUseCase(
    private val resourcesProvider: IResourcesProvider,
) : UseCase<UseCase.None, ReadStorageUseCase.Params>() {

    data class Params(
        val storageProvider: IStorageProvider,
        val storage: TetroidStorage,
        val isDecrypt: Boolean,
        val isFavoritesOnly: Boolean,
        val isOpenLastNode: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val storageProvider = params.storageProvider
        val storage = params.storage

        // FIXME ?
        //storageProvider.resetStorage()

        return readStorage(params)
            .flatMap {
                storageProvider.getRootNode().name = resourcesProvider.getString(R.string.title_root_node)
                storageProvider.setStorage(storage)

                None.toRight()
            }
    }

    /**
     * Загрузка хранилища из файла mytetra.xml.
     */
    private suspend fun readStorage(params: Params): Either<Failure, None> {
        val storage = params.storage

        val myTetraXmlFilePath = makePath(params.storage.path, Constants.MYTETRA_XML_FILE_NAME)
        return try {
            val myTetraXmlFile = File(myTetraXmlFilePath)
            if (!myTetraXmlFile.exists()) {
                storage.isLoaded = false
                return Failure.Storage.Load.XmlFileNotExist(pathToFile = myTetraXmlFile.path).toLeft()
            }

            @Suppress("BlockingMethodInNonBlockingContext")
            val fis = FileInputStream(myTetraXmlFile)
            // непосредственная обработка xml файла со структурой хранилища
            params.storageProvider.dataProcessor.parse(
                fis = fis,
                isNeedDecrypt = params.isDecrypt,
                isLoadFavoritesOnly = params.isFavoritesOnly
            )
            storage.isLoaded = true

            None.toRight()
        } catch (ex: Exception) {
            storage.isLoaded = false
            Failure.Storage.Load.ReadXmlFile(pathToFile = myTetraXmlFilePath, ex).toLeft()
        }
    }

}
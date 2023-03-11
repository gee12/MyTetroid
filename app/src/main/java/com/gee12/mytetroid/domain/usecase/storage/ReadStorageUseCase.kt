package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.openInputStream
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.model.FilePath

/**
 * Непосредственное чтение структуры хранилища.
 */
class ReadStorageUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val storageProvider: IStorageProvider,
) : UseCase<UseCase.None, ReadStorageUseCase.Params>() {

    data class Params(
        val isDecrypt: Boolean,
        val isFavoritesOnly: Boolean,
        val isOpenLastNode: Boolean,
    )

    override suspend fun run(params: Params): Either<Failure, None> {

        // FIXME ?
        //storageProvider.resetStorage()

        return readStorage(params)
            .flatMap {
                storageProvider.getRootNode().name = resourcesProvider.getString(R.string.title_root_node)

                None.toRight()
            }
    }

    /**
     * Загрузка хранилища из файла mytetra.xml.
     */
    private suspend fun readStorage(params: Params): Either<Failure, None> {
        val storage = storageProvider.storage
            ?: return Failure.Storage.Load.StorageNotInited.toLeft()
        val storageFolderUri = Uri.parse(storage.uri)
        val storageFolderPath = storageFolderUri.path.orEmpty()
        val storageFolder = storageProvider.rootFolder
            ?: return Failure.Storage.Load.StorageNotInited.toLeft()
        val xmlFilePath = FilePath.File(storageFolderPath, Constants.MYTETRA_XML_FILE_NAME)

        return try {
            val myTetraXmlFile = storageFolder.child(
                context = context,
                path = xmlFilePath.fileName,
                requiresWriteAccess = !storage.isReadOnly
            ) ?: return Failure.File.Get(xmlFilePath).toLeft()

            if (!myTetraXmlFile.exists()) {
                storage.isLoaded = false
                return Failure.File.Get(xmlFilePath).toLeft()
            }

            myTetraXmlFile.openInputStream(context)?.use { fis ->
                // непосредственная обработка xml файла со структурой хранилища
                storageProvider.dataProcessor.parse(
                    fis = fis,
                    isNeedDecrypt = params.isDecrypt,
                    isLoadFavoritesOnly = params.isFavoritesOnly
                )
            }
            storage.isLoaded = true

            None.toRight()
        } catch (ex: Exception) {
            storage.isLoaded = false

            Failure.Storage.Load.ReadXmlFile(xmlFilePath, ex).toLeft()
        }
    }

}
package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.isEmpty
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.parseUri
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.TetroidStorage

class CheckStorageFilesExistingUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
) : UseCase<CheckStorageFilesExistingUseCase.Result, CheckStorageFilesExistingUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
    )

    sealed class Result {
        object Success : Result()
        data class Error(
            val errorsString: String,
        ) : Result()
    }

    override suspend fun run(params: Params): Either<Failure, Result> {
        val storage = params.storage
        val storageFolderUri = storage.uri.parseUri()
        val storageFolder = DocumentFileCompat.fromUri(context, storageFolderUri)

        return when {
            storageFolder == null -> {
                Result.Error(
                    resourcesProvider.getString(R.string.error_check_storage_folder)
                ).toRight()
            }
            !storageFolder.exists() -> {
                Result.Error(
                    resourcesProvider.getString(R.string.error_storage_folder_is_not_exists)
                ).toRight()
            }
            storageFolder.isEmpty(context) -> {
                Result.Error(
                    resourcesProvider.getString(R.string.error_storage_folder_is_empty)
                ).toRight()
            }
            else -> {
                checkFiles(storageFolder)
            }
        }
    }

    private fun checkFiles(storageFolder: DocumentFile): Either.Right<Result> {
        val fileNames = storageFolder.listFiles().map { it.name }
        val errors = buildList {
            if (!fileNames.contains(Constants.BASE_DIR_NAME)) {
                add(resourcesProvider.getString(R.string.folder_name_mask, Constants.BASE_DIR_NAME))
            }
            if (!fileNames.contains(Constants.DATABASE_INI_FILE_NAME)) {
                add(resourcesProvider.getString(R.string.file_name_mask, Constants.DATABASE_INI_FILE_NAME))
            }
            if (!fileNames.contains(Constants.MYTETRA_XML_FILE_NAME)) {
                add(resourcesProvider.getString(R.string.file_name_mask, Constants.MYTETRA_XML_FILE_NAME))
            }
        }
        return when (errors.size) {
            1 -> {
                Result.Error(
                    resourcesProvider.getString(R.string.title_is_not_exist_mask, errors.first())
                ).toRight()
            }
            2, 3 -> {
                Result.Error(
                    resourcesProvider.getString(R.string.title_is_not_exist_plural_mask, errors.joinToString())
                ).toRight()
            }
            else -> Result.Success.toRight()
        }
    }

}
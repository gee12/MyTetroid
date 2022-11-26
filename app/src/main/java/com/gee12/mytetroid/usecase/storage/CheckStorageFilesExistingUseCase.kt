package com.gee12.mytetroid.usecase.storage

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.isDirEmpty
import com.gee12.mytetroid.common.extensions.isFileExist
import com.gee12.mytetroid.helpers.IResourcesProvider
import com.gee12.mytetroid.helpers.StoragePathHelper
import com.gee12.mytetroid.model.TetroidStorage

class CheckStorageFilesExistingUseCase(
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
        val storagePathHelper = StoragePathHelper(
            storageProvider = null,
            storage = params.storage,
        )

        return when {
            !storagePathHelper.getStoragePath().isFileExist() -> {
                Result.Error(
                    resourcesProvider.getString(R.string.error_storage_folder_is_not_exists)
                ).toRight()
            }
            storagePathHelper.getStoragePath().isDirEmpty() -> {
                Result.Error(
                    resourcesProvider.getString(R.string.error_storage_folder_is_empty)
                ).toRight()
            }
            else -> {
                val errors = buildList {
                    if (!storagePathHelper.getPathToStorageBaseFolder().isFileExist()) {
                        add(resourcesProvider.getString(R.string.folder_name_mask, Constants.BASE_DIR_NAME))
                    }
                    if (!storagePathHelper.getPathToDatabaseIniConfig().isFileExist()) {
                        add(resourcesProvider.getString(R.string.file_name_mask, Constants.DATABASE_INI_FILE_NAME))
                    }
                    if (!storagePathHelper.getPathToMyTetraXml().isFileExist()) {
                        add(resourcesProvider.getString(R.string.file_name_mask, Constants.MYTETRA_XML_FILE_NAME))
                    }
                }
                when (errors.size) {
                    1 -> {
                        Result.Error(
                            resourcesProvider.getString(R.string.title_is_not_exist_mask, errors.first())
                        ).toRight()
                    }
                    2,3 -> {
                        Result.Error(
                            resourcesProvider.getString(R.string.title_is_not_exist_plural_mask, errors.joinToString())
                        ).toRight()
                    }
                    else -> Result.Success.toRight()
                }
            }
        }
    }

}
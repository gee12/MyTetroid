package com.gee12.mytetroid.domain.usecase.file

import android.content.Context
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileType
import com.anggrayudi.storage.file.deleteRecursively
import com.anggrayudi.storage.file.isEmpty
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath

class ClearFolderUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
) : UseCase<UseCase.None, ClearFolderUseCase.Params>() {

    data class Params(
        val folderPath: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val folderPath = FilePath.FolderFull(params.folderPath)

        return try {
            val folder = DocumentFileCompat.fromFullPath(
                context = context,
                fullPath = folderPath.fullPath,
                documentType = DocumentFileType.FOLDER,
                requiresWriteAccess = true,
            ) ?: return Failure.Folder.Get(folderPath).toLeft()

            if (folder.exists() && !folder.isEmpty(context)) {
                if (folder.deleteRecursively(context, childrenOnly = true)) {
                    logger.log(resourcesProvider.getString(R.string.log_cleared_trash_dir_mask, folderPath.fullPath), show = false)
                } else {
                    return Failure.Folder.Create(folderPath).toLeft()
                }
            }

            None.toRight()
        } catch (ex: Exception) {
            Failure.Folder.Clear(folderPath, ex).toLeft()
        }
    }

}
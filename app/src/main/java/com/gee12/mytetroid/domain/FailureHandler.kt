package com.gee12.mytetroid.domain

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.extensions.ifNotEmpty
import com.gee12.mytetroid.model.NotificationData
import com.gee12.mytetroid.domain.provider.IResourcesProvider

interface IFailureHandler {
    fun getFailureMessage(failure: Failure): NotificationData
    fun getExceptionInfo(ex: Throwable): String
}

class FailureHandler(
    private val resourcesProvider: IResourcesProvider,
) : IFailureHandler {

    companion object {
        private const val CALLER_STACK_INDEX = 5
    }

    override fun getFailureMessage(failure: Failure): NotificationData {
        return when (failure) {
            is Failure.RequiredApiVersion -> {
                NotificationData.Error(title = getString(R.string.error_required_api_version_mask, failure.minApiVersion))
            }
            is Failure.UnknownError -> {
                NotificationData.Error(title = "Unknown error")
            }
            is Failure.ArgumentIsEmpty -> {
                NotificationData.Error(
                    title = buildString {
                        append("Required argument ")
                        failure.clazz.ifNotEmpty { append("$it.") }
                        failure.method.ifNotEmpty { append("$it(): ") }
                        failure.argument.ifNotEmpty { append("$it ") }
                        append("is null !")
                    }
                )
            }
            is Failure.Storage -> {
                getStorageFailureMessage(failure)
            }
            is Failure.Node -> {
                getNodeFailureMessage(failure)
            }
            is Failure.Record -> {
                getRecordFailureMessage(failure)
            }
            is Failure.Attach -> {
                getAttachFailureMessage(failure)
            }
            is Failure.Tag -> {
                getTagsFailureMessage(failure)
            }
            is Failure.Favorites -> {
                getFavoritesFailureMessage(failure)
            }
            is Failure.Image -> {
                getImageFailureMessage(failure)
            }
            is Failure.Encrypt -> {
                getEncryptFailureMessage(failure)
            }
            is Failure.Decrypt -> {
                getDecryptFailureMessage(failure)
            }
            is Failure.File -> {
                getFileFailureMessage(failure)
            }
            is Failure.Folder -> {
                getFolderFailureMessage(failure)
            }
            is Failure.Network.DownloadWebPageError -> {
                NotificationData.Error(
                    title = getString(R.string.log_error_download_web_page_mask, failure.ex?.message.orEmpty()),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Network.DownloadImageError -> {
                NotificationData.Error(
                    title = getString(R.string.log_error_download_image_mask, failure.ex?.message.orEmpty()),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Network.DownloadFileError -> {
                NotificationData.Error(
                    title = getString(R.string.log_error_download_file_mask, failure.ex?.message.orEmpty()),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getStorageFailureMessage(failure: Failure.Storage): NotificationData {
        return when (failure) {
            is Failure.Storage.StorageNotInited -> {
                NotificationData.Error(
                    title = getString(R.string.error_storage_not_initialized)
                )
            }
            // create
            is Failure.Storage.Create.FilesError -> {
                NotificationData.Error(
                    title = getString(R.string.error_create_storage_files_in_path_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Storage.Create.FolderNotEmpty -> {
                NotificationData.Error(
                    title = getString(R.string.error_storage_folder_not_empty_mask, failure.path.fullPath),
                )
            }
            is Failure.Storage.Create.FolderIsMissing -> {
                NotificationData.Error(
                    title = getString(R.string.error_storage_folder_is_missing_mask, failure.path.fullPath),
                )
            }

            // init
            is Failure.Storage.Init -> {
                NotificationData.Error(
                    title = getString(R.string.log_failed_storage_init_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            // load
            is Failure.Storage.Load.ReadXmlFile -> {
                NotificationData.Error(
                    title = getString(R.string.log_failed_storage_load_tree_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            // save
            is Failure.Storage.Save.RenameXmlFileFromTempName -> {
                NotificationData.Error(
                    title = getString(R.string.error_rename_file_from_to_mask, failure.from, failure.to)
                )
            }
            Failure.Storage.Save.UpdateInBase -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_storage_in_database)
                )
            }
            is Failure.Storage.Save.PasswordHash -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_pass_hash_salt),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Storage.Save.PasswordCheckData -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_pass_check_data),
                    message = failure.ex?.getInfo()
                )
            }
            // delete
            is Failure.Storage.Delete.FromDb -> {
                NotificationData.Error(
                    title = getString(R.string.error_delete_storage_in_database),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getNodeFailureMessage(failure: Failure.Node): NotificationData {
        return when (failure) {
            is Failure.Node.NotFound -> {
                NotificationData.Error(
                    title = getString(R.string.error_node_not_found_with_id_mask, failure.nodeId),
                )
            }
            Failure.Node.NameIsEmpty -> {
                NotificationData.Error(
                    title = getString(R.string.error_node_name_is_empty),
                )
            }
        }
    }

    private fun getRecordFailureMessage(failure: Failure.Record): NotificationData {
        return when (failure) {
            is Failure.Record.NotFoundInNode -> {
                NotificationData.Error(
                    title = getString(R.string.error_record_not_exist_in_node_mask, failure.recordId),
                )
            }
            is Failure.Record.NameIsEmpty -> {
                NotificationData.Error(
                    title = getString(R.string.error_record_name_is_empty),
                )
            }
            is Failure.Record.CloneRecordToNode -> {
                NotificationData.Error(
                    title = getString(R.string.error_clone_record_to_node),
                )
            }
            is Failure.Record.Read.NotDecrypted -> {
                NotificationData.Error(
                    title = getString(R.string.error_record_not_decrypted),
                )
            }
            is Failure.Record.Read.ParseFromHtml -> {
                NotificationData.Error(
                    title = getString(R.string.error_parse_text_from_html),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Record.NotFound -> {
                NotificationData.Error(
                    title = getString(R.string.error_record_not_found_with_id_mask, failure.recordId),
                )
            }
            is Failure.Record.ExportToPdf -> {
                NotificationData.Error(
                    title = getString(R.string.error_export_to_pdf),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getAttachFailureMessage(failure: Failure.Attach): NotificationData {
        return when (failure) {
            is Failure.Attach.NameIsEmpty -> {
                NotificationData.Error(
                    title = getString(R.string.error_attach_name_is_empty),
                )
            }
            is Failure.Attach.NotFoundInRecord -> {
                NotificationData.Error(
                    title = getString(R.string.error_attach_not_found_in_record),
                )
            }
        }
    }

    private fun getTagsFailureMessage(failure: Failure.Tag): NotificationData {
        return when (failure) {
            Failure.Tag.NameIsEmpty -> {
                NotificationData.Error(
                    title = getString(R.string.error_tag_name_is_empty),
                )
            }
            is Failure.Tag.NotFoundByName -> {
                NotificationData.Error(
                    title = getString(R.string.error_tag_not_found_by_name_mask, failure.name),
                )
            }
        }
    }

    private fun getFavoritesFailureMessage(failure: Failure.Favorites): NotificationData {
        return when (failure) {
            is Failure.Favorites.UnknownError -> {
                NotificationData.Error(
                    title = "Unknown error",
                    message = failure.ex?.getInfo(),
                )
            }
        }
    }

    private fun getImageFailureMessage(failure: Failure.Image): NotificationData {
        return when (failure) {
            is Failure.Image.LoadFromFile -> {
                NotificationData.Error(
                    title = getString(R.string.error_load_image_from_file_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Image.SaveToFile -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_image_to_ile_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Image.UnknownType -> {
                NotificationData.Error(
                    title = getString(R.string.error_image_file_unknown_type_mask, failure.type),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getEncryptFailureMessage(failure: Failure.Encrypt): NotificationData {
        return when (failure) {
            is Failure.Encrypt.File -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_encrypt_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getDecryptFailureMessage(failure: Failure.Decrypt): NotificationData {
        return when (failure) {
            is Failure.Decrypt.File -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_decrypt_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getFileFailureMessage(failure: Failure.File): NotificationData {
        return when (failure) {
            is Failure.File.NotExist -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_is_missing_mask, failure.path.fullPath)
                )
            }
            is Failure.File.AccessDenied -> {
                NotificationData.Error(
                    title = getString(R.string.error_denied_read_file_access_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Get -> {
                NotificationData.Error(
                    title = getString(R.string.error_get_file_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.GetFileSize -> {
                NotificationData.Error(
                    title = getString(R.string.error_get_file_size_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Read -> {
                NotificationData.Error(
                    title = getString(R.string.error_read_file_in_path_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Write -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_file_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Create -> {
                NotificationData.Error(
                    title = getString(R.string.error_create_new_file, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Move -> {
                NotificationData.Error(
                    title = getString(R.string.error_move_file_to_folder_mask, failure.from.fullPath, failure.to.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Rename -> {
                NotificationData.Error(
                    title = getString(R.string.error_rename_file_mask, failure.path.fullPath, failure.newName),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Copy -> {
                NotificationData.Error(
                    title = getString(R.string.error_copy_file_mask, failure.from.fullPath, failure.to.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Delete -> {
                NotificationData.Error(
                    title = getString(R.string.error_delete_file_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.GetContentUri -> {
                NotificationData.Error(
                    title = getString(R.string.log_file_sharing_error_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.UnknownExtension -> {
                NotificationData.Error(
                    title = getString(R.string.error_get_file_extension_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.UnknownMimeType -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_unknown_mime_type_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.File.Unknown -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_unknown_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getFolderFailureMessage(failure: Failure.Folder): NotificationData {
        return when (failure) {
            is Failure.Folder.NotExist -> {
                NotificationData.Error(
                    title = getString(R.string.error_folder_is_missing_mask, failure.path.fullPath)
                )
            }
            is Failure.Folder.Get -> {
                NotificationData.Error(
                    title = getString(R.string.error_get_folder_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Folder.GetFolderSize -> {
                NotificationData.Error(
                    title = getString(R.string.error_get_folder_size_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Folder.Create -> {
                NotificationData.Error(
                    title = getString(R.string.error_create_folder_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Folder.Move -> {
                NotificationData.Error(
                    title = getString(R.string.error_move_folder_mask, failure.from.fullPath, failure.to.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Folder.Copy -> {
                NotificationData.Error(
                    title = getString(R.string.error_copy_folder_mask, failure.from.fullPath, failure.to.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Folder.Clear -> {
                NotificationData.Error(
                    title = getString(R.string.error_clear_folder_mask, failure.path.fullPath)
                )
            }
            is Failure.Folder.Delete -> {
                NotificationData.Error(
                    title = getString(R.string.error_delete_folder_mask, failure.path.fullPath)
                )
            }
            is Failure.Folder.Unknown -> {
                NotificationData.Error(
                    title = getString(R.string.error_folder_unknown_mask, failure.path.fullPath),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    override fun getExceptionInfo(ex: Throwable): String {
        val stackTrace = ex.cause?.stackTrace ?: ex.stackTrace

        return buildString {
            append("Exception: ")
            appendLine(ex.toString())
            append("StackTrace: ")
            stackTrace
                .forEach {
                    appendLine("\t$it;")
                }
        }
    }

    private fun getString(id: Int) = resourcesProvider.getString(id)

    private fun getString(id: Int, vararg args: Any) = resourcesProvider.getString(id, *args)

    private fun Throwable.getInfo() = getExceptionInfo(this)

}

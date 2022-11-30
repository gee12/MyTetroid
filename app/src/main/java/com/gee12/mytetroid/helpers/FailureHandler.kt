package com.gee12.mytetroid.helpers

import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.extensions.ifNotEmpty
import com.gee12.mytetroid.model.NotificationData

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
            is Failure.Tags -> {
                getTagsFailureMessage(failure)
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
            else -> NotificationData.Empty()
        }
    }

    private fun getStorageFailureMessage(failure: Failure.Storage): NotificationData {
        return when (failure) {

            // create
            is Failure.Storage.Create.FilesError -> {
                NotificationData.Error(
                    title = getString(R.string.error_create_storage_files_in_path_mask, failure.pathToFolder),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Storage.Create.FolderNotEmpty -> {
                NotificationData.Error(
                    title = getString(R.string.error_storage_folder_not_empty_mask, failure.pathToFolder),
                )
            }
            is Failure.Storage.Create.FolderIsMissing -> {
                NotificationData.Error(
                    title = getString(R.string.error_storage_folder_is_missing_mask, failure.pathToFolder),
                )
            }
            is Failure.Storage.Create.BaseFolder -> {
                NotificationData.Error(
                    title = getString(R.string.error_create_folder_in_path_mask, Constants.BASE_DIR_NAME, failure.pathToFolder),
                )
            }

            // init
            is Failure.Storage.Init -> {
                NotificationData.Error(
                    title = getString(R.string.log_failed_storage_init_mask, failure.storagePath),
                    message = failure.ex?.getInfo()
                )
            }
            // load
            is Failure.Storage.Load.XmlFileNotExist -> {
                NotificationData.Error(
                    title = getString(R.string.log_file_is_absent_in_path_mask, Constants.MYTETRA_XML_FILE_NAME, failure.pathToFile)
                )
            }
            is Failure.Storage.Load.ReadXmlFile -> {
                NotificationData.Error(
                    title = getString(R.string.error_read_file_in_path_mask, Constants.MYTETRA_XML_FILE_NAME, failure.pathToFile),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Storage.DatabaseConfig.Load -> {
                NotificationData.Error(
                    title = getString(R.string.error_load_file_in_path_mask, Constants.DATABASE_INI_FILE_NAME, failure.pathToFile)
                )
            }
            is Failure.Storage.DatabaseConfig.Save -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_file_in_path_mask, Constants.DATABASE_INI_FILE_NAME, failure.pathToFile)
                )
            }
            // save
            is Failure.Storage.Save.SaveXmlFile -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_file_in_path_mask, Constants.MYTETRA_XML_FILE_NAME, failure.pathToFile),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Storage.Save.RenameXmlFileFromTempName -> {
                NotificationData.Error(
                    title = getString(R.string.errorrename_file_from_to_mask, failure.from, failure.to)
                )
            }
            is Failure.Storage.Save.RemoveOldXmlFile -> {
                NotificationData.Error(
                    title = getString(R.string.error_delete_file_by_path_mask, Constants.MYTETRA_XML_FILE_NAME, failure.pathToFile)
                )
            }
            Failure.Storage.Save.UpdateInBase -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_storage_in_database)
                )
            }
            is Failure.Storage.Save.PassCheckData -> {
                NotificationData.Error(
                    title = getString(R.string.error_save_pass_check_data),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getNodeFailureMessage(failure: Failure.Node): NotificationData {
        return when (failure) {
            else -> NotificationData.Empty()
        }
    }

    private fun getRecordFailureMessage(failure: Failure.Record): NotificationData {
        return when (failure) {
            is Failure.Record.Create.NameIsEmpty -> {
                NotificationData.Error(
                    title = getString(R.string.error_record_name_is_empty),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Record.CloneRecordToNode -> {
                NotificationData.Error(
                    title = getString(R.string.error_clone_record_to_node),
                )
            }
        }
    }

    private fun getTagsFailureMessage(failure: Failure.Tags): NotificationData {
        return when (failure) {
            else -> NotificationData.Empty()
        }
    }

    private fun getEncryptFailureMessage(failure: Failure.Encrypt): NotificationData {
        return when (failure) {
            is Failure.Encrypt.File -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_encrypt_mask, failure.fileName),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getDecryptFailureMessage(failure: Failure.Decrypt): NotificationData {
        return when (failure) {
            is Failure.Decrypt.File -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_decrypt_mask, failure.fileName),
                    message = failure.ex?.getInfo()
                )
            }
        }
    }

    private fun getFileFailureMessage(failure: Failure.File): NotificationData {
        return when (failure) {
            is Failure.File.IsMissing -> {
                NotificationData.Error(
                    title = getString(R.string.error_file_is_missing_mask, failure.path)
                )
            }
            is Failure.File.AccessDenied -> {
                NotificationData.Error(
                    title = getString(R.string.error_denied_read_file_access_mask, failure.path)
                )
            }
            is Failure.File.GetFileSize -> {
                NotificationData.Error(
                    title = getString(R.string.error_get_file_size_mask, failure.path)
                )
            }
        }
    }

    private fun getFolderFailureMessage(failure: Failure.Folder): NotificationData {
        return when (failure) {
            is Failure.Folder.IsMissing -> {
                NotificationData.Error(
                    title = getString(R.string.error_folder_is_missing_mask, failure.path)
                )
            }
            is Failure.Folder.GetFolderSize -> {
                NotificationData.Error(
                    title = getString(R.string.error_get_folder_size_mask, failure.path)
                )
            }
        }
    }

    override fun getExceptionInfo(ex: Throwable): String {
        val caller = Thread.currentThread().stackTrace[CALLER_STACK_INDEX]
        val fullClassName = caller.className
        val className = fullClassName.substring(fullClassName.lastIndexOf('.') + 1)
        val methodName = caller.methodName
        val lineNumber = caller.lineNumber
        return "%s.%s():%d\n%s".format(className, methodName, lineNumber, ex.message)
    }

    private fun getString(id: Int) = resourcesProvider.getString(id)

    private fun getString(id: Int, vararg args: Any) = resourcesProvider.getString(id, *args)

    private fun Throwable.getInfo() = getExceptionInfo(this)

}

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

    // TODO
    override fun getFailureMessage(failure: Failure): NotificationData {
        return when (failure) {
            is Failure.UnknownError -> {
                NotificationData(title = "Unknown error")
            }
            is Failure.ArgumentIsEmpty -> {
                NotificationData(
                    title = buildString {
                        append("Required argument ")
                        failure.clazz.ifNotEmpty { append("$it.") }
                        failure.method.ifNotEmpty { append("$it(): ") }
                        failure.argument.ifNotEmpty { append("$it ") }
                        append("is null !")
                    }
                )
            }
            is Failure.Record -> {
                getRecordFailureMessage(failure)
            }
            is Failure.Storage -> {
                getStorageFailureMessage(failure)
            }
            is Failure.File -> {
                getFileFailureMessage(failure)
            }
            else -> NotificationData.Empty
        }
    }

    private fun getRecordFailureMessage(failure: Failure.Record): NotificationData {
        when (failure) {
            is Failure.Record.Create.NameIsEmpty -> {
                NotificationData(
                    title = getString(R.string.error_record_name_is_empty),
                    message = failure.ex?.getInfo()
                )
            }
//            is Failure.Record.CloneRecordToNode -> {
//                NotificationData(
//                    title = getString(R.string.log_failed_storage_create_mask, failure.storageName),
//                    message = failure.ex?.getInfo()
//                )
//            }
            // TODO
            else -> NotificationData.Empty
        }
    }

    private fun getStorageFailureMessage(failure: Failure.Storage): NotificationData {
        return when (failure) {
            is Failure.Storage.Create -> {
                NotificationData(
                    title = getString(R.string.log_failed_storage_create_mask, failure.storageName),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Storage.Init -> {
                NotificationData(
                    title = getString(R.string.log_failed_storage_init_mask, failure.storagePath),
                    message = failure.ex?.getInfo()
                )
            }
            // load
            is Failure.Storage.Load.XmlFileNotExist -> {
                NotificationData(
                    // TODO: заюзать storagePath
                    title = getString(R.string.log_file_is_absent) + Constants.MYTETRA_XML_FILE_NAME
                )
            }
            is Failure.Storage.Load.ReadXmlFile -> {
                NotificationData(
                    title = getString(R.string.log_failed_storage_read_mask, failure.storagePath),
                    message = failure.ex?.getInfo()
                )
            }
            is Failure.Storage.DatabaseConfig.Load -> {
                NotificationData(
                    title = getString(R.string.log_failed_database_config_load_mask, failure.storagePath)
                )
            }
            // save
            is Failure.Storage.Save.SaveXmlFile -> {
                NotificationData(
                    title = getString(R.string.log_failed_storage_save_mask, failure.storagePath),
                    message = failure.ex?.getInfo()
                )
            }
            Failure.Storage.Save.RenameXmlFileFromTempName -> {
                TODO()
//                NotificationData(
//                    title = getString(R.string.)
//                )
            }
            Failure.Storage.Save.RemoveOldXmlFile -> {
                TODO()
//                NotificationData(
//                    title = getString(R.string.)
//                )
            }
        }
    }

    private fun getFileFailureMessage(failure: Failure.File): NotificationData {
        return when (failure) {
            is Failure.File.IsMissing -> {
                NotificationData(
                    title = getString(R.string.error_file_is_missing_mask, failure.path)
                )
            }
            is Failure.File.AccessDenied -> {
                NotificationData(
                    title = getString(R.string.error_denied_read_file_access_mask, failure.path)
                )
            }
            is Failure.File.GetFileSize -> {
                NotificationData(
                    title = getString(R.string.error_get_file_size_mask, failure.path)
                )
            }
            is Failure.File.GetFolderSize -> {
                NotificationData(
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
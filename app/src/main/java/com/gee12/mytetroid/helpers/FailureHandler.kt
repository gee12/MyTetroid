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
            is Failure.Storage -> {
                getStorageFailureMessage(failure)
            }
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

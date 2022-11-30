package com.gee12.mytetroid.common

sealed class Failure(val ex: Throwable? = null) {

    data class ArgumentIsEmpty(
        val clazz: String = "",
        val method: String = "",
        val argument: String = ""
    ) : Failure()

    class UnknownError(ex: Throwable? = null) : Failure(ex)

    sealed class Storage(ex: Throwable? = null) : Failure(ex) {
        sealed class Create(ex: Throwable? = null) : Storage(ex) {
            class FilesError(val pathToFolder: String, ex: Throwable) : Create(ex)
            class FolderNotEmpty(val pathToFolder: String) : Create()
            class FolderIsMissing(val pathToFolder: String) : Create()
            class BaseFolder(val pathToFolder: String) : Create()
        }
        class Init(val storagePath: String, ex: Throwable?) : Storage(ex)

        sealed class Load(ex: Throwable? = null) : Storage(ex) {
            class ReadXmlFile(val pathToFile: String, ex: Throwable?) : Load(ex)
            data class XmlFileNotExist(val pathToFile: String) : Load()
        }
        sealed class Save(ex: Throwable? = null) : Storage(ex) {
            object UpdateInBase : Save()
            class PassCheckData(ex: Throwable) : Save(ex)
            class SaveXmlFile(val pathToFile: String, ex: Throwable? = null) : Save(ex)
            data class RemoveOldXmlFile(
                val pathToFile: String,
            ) : Save()
            data class RenameXmlFileFromTempName(
                val from: String,
                val to: String,
            ) : Save()
        }

        sealed class DatabaseConfig(ex: Throwable? = null) : Storage(ex) {
            class Load(val pathToFile: String, ex: Throwable? = null) : DatabaseConfig(ex)
            class Save(val pathToFile: String, ex: Throwable? = null) : DatabaseConfig(ex)
        }

    }

    sealed class Node(ex: Throwable? = null) : Failure(ex) {
        sealed class Create(ex: Throwable? = null) : Node(ex) {
            object NameIsEmpty : Create()
        }
    }

    sealed class Record(ex: Throwable? = null) : Failure(ex) {
        sealed class Create(ex: Throwable? = null) : Record(ex) {
            object NameIsEmpty : Create()
        }
        object CloneRecordToNode : Record()
    }

    sealed class Tags(ex: Throwable? = null) : Failure(ex) {
        sealed class Parse(ex: Throwable? = null) : Node(ex) {
            object TagIsEmpty : Create()
        }
    }

    sealed class Encrypt(ex: Throwable? = null) : Failure(ex) {
        class File(val fileName: String, ex: Throwable? = null) : Encrypt(ex)
    }

    sealed class Decrypt(ex: Throwable? = null) : Failure(ex) {
        class File(val fileName: String, ex: Throwable? = null) : Decrypt(ex)
    }

    sealed class File(val path: String, ex: Throwable? = null) : Failure(ex) {
        class IsMissing(path: String) : File(path)
        class AccessDenied(path: String, ex: Throwable? = null) : File(path, ex)
        class GetFileSize(path: String, ex: Throwable? = null) : File(path, ex)
        class GetFolderSize(path: String, ex: Throwable? = null) : File(path, ex)
    }

}
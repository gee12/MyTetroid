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
        object NameIsEmpty : Node()
        class NotFound(val nodeId: String) : Node()
    }

    sealed class Record(ex: Throwable? = null) : Failure(ex) {
        object NameIsEmpty : Record()
        class NotFound(val recordId: String) : Record()
        class NotFoundInNode(val recordId: String) : Record()
        object CloneRecordToNode : Record()
        sealed class CheckFolder(ex: Throwable? = null) : Record(ex) {
            class FolderNotExist(val folderPath: String) : CheckFolder()
            class CreateFolderError(val folderPath: String) : CheckFolder()
            class UnknownError(val folderPath: String, ex: Exception) : CheckFolder(ex)
        }
        sealed class Read(ex: Throwable? = null) : Record(ex) {
            class NotDecrypted : Read()
            class ParseFromHtml(ex: Throwable) : Read(ex)
        }

    }

    sealed class Attach(ex: Throwable? = null) : Failure(ex) {
        object NameIsEmpty : Attach()
        class NotFoundInRecord(val attachId: String) : Attach()
    }

    sealed class Tags(ex: Throwable? = null) : Failure(ex) {
    }

    sealed class Favorites(ex: Throwable? = null) : Failure(ex) {
        class UnknownError(ex: Exception) : Favorites(ex)
    }

    sealed class Image(ex: Throwable? = null) : Failure(ex) {
        class SaveFile(val fileName: String, ex: Throwable? = null) : Image(ex)
    }

    sealed class Encrypt(ex: Throwable? = null) : Failure(ex) {
        class File(val fileName: String, ex: Throwable? = null) : Encrypt(ex)
    }

    sealed class Decrypt(ex: Throwable? = null) : Failure(ex) {
        class File(val fileName: String, ex: Throwable? = null) : Decrypt(ex)
    }

    sealed class File(ex: Throwable? = null) : Failure(ex) {
        class NotExist(val path: String) : File()
        class AccessDenied(val path: String, ex: Throwable? = null) : File(ex)
        class GetFileSize(val path: String, ex: Throwable? = null) : File(ex)
        class Read(val path: String, ex: Throwable? = null) : File(ex)
        class Write(val fileName: String, val path: String, ex: Throwable? = null) : File(ex)
        class Create(val filePath: String, ex: Throwable? = null) : File(ex)
        class Delete(val filePath: String, ex: Throwable? = null) : File(ex)
        class MoveToFolder(val filePath: String, val folderPath: String) : File()
        class RenameTo(val filePath: String, val newName: String) : File()
        class Copy(val filePath: String, val newPath: String) : File()
        class CreateUriPath(val path: String) : File()
//        class UnknownError(ex: Exception) : File(ex)
    }

    sealed class Folder(ex: Throwable? = null) : Failure(ex) {
        class NotExist(val path: String) : Folder()
        class GetFolderSize(val path: String, ex: Throwable? = null) : Folder(ex)
        class Delete(val path: String) : Folder()
        class Copy(val path: String, val newPath: String) : Folder()
    }

}
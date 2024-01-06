package com.gee12.mytetroid.common

import com.gee12.mytetroid.model.FilePath


sealed class Failure(val ex: Throwable? = null) {

    data class ArgumentIsEmpty(
        val clazz: String = "",
        val method: String = "",
        val argument: String = ""
    ) : Failure()

    class RequiredApiVersion(val minApiVersion: Int, ex: Throwable? = null) : Failure(ex)
    class UnknownError(ex: Throwable? = null) : Failure(ex)

    sealed class Storage(ex: Throwable? = null) : Failure(ex) {
        object StorageNotInited : Load()
        sealed class Create(ex: Throwable? = null) : Storage(ex) {
            class FilesError(val path: FilePath, ex: Throwable) : Create(ex)
            class FolderNotEmpty(val path: FilePath) : Create()
            class FolderIsMissing(val path: FilePath) : Create()
        }
        class Init(val path: FilePath, ex: Throwable?) : Storage(ex)

        sealed class Load(ex: Throwable? = null) : Storage(ex) {
            class ReadXmlFile(val path: FilePath, ex: Throwable?) : Load(ex)
        }
        sealed class Save(ex: Throwable? = null) : Storage(ex) {
            object UpdateInBase : Save()
            class PasswordHash(ex: Throwable) : Save(ex)
            class PasswordCheckData(ex: Throwable? = null) : Save(ex)
            data class RenameXmlFileFromTempName(
                val from: String,
                val to: String,
            ) : Save()
        }
        sealed class Delete(ex: Throwable? = null) : Storage(ex) {
            object FromDb : Delete()
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
        sealed class Read(ex: Throwable? = null) : Record(ex) {
            class NotDecrypted : Read()
            class ParseFromHtml(ex: Throwable) : Read(ex)
        }
        class ExportToPdf(ex: Throwable? = null) : Record(ex)
    }

    sealed class Attach(ex: Throwable? = null) : Failure(ex) {
        object NameIsEmpty : Attach()
        class NotFoundInRecord(val attachId: String) : Attach()
    }

    sealed class Tag(ex: Throwable? = null) : Failure(ex) {
        object NameIsEmpty : Tag()
        class NotFoundByName(val name: String) : Tag()
    }

    sealed class Favorites(ex: Throwable? = null) : Failure(ex) {
        class UnknownError(ex: Exception) : Favorites(ex)
    }

    sealed class Image(ex: Throwable? = null) : Failure(ex) {
        class LoadFromFile(val path: FilePath, ex: Throwable? = null) : Image(ex)
        class SaveToFile(val path: FilePath, ex: Throwable? = null) : Image(ex)
        data class UnknownType(val type: String) : Image()
    }

    sealed class Encrypt(ex: Throwable? = null) : Failure(ex) {
        class File(val path: FilePath, ex: Throwable? = null) : Encrypt(ex)
    }

    sealed class Decrypt(ex: Throwable? = null) : Failure(ex) {
        class File(val path: FilePath, ex: Throwable? = null) : Decrypt(ex)
    }

    sealed class File(ex: Throwable? = null) : Failure(ex) {
        class NotExist(val path: FilePath) : File()
        class AccessDenied(val path: FilePath, ex: Throwable? = null) : File(ex)
        class GetFileSize(val path: FilePath, ex: Throwable? = null) : File(ex)
        class Get(val path: FilePath, ex: Throwable? = null) : File(ex)
        class Read(val path: FilePath, ex: Throwable? = null) : File(ex)
        class Write(val path: FilePath, ex: Throwable? = null) : File(ex)
        class Create(val path: FilePath, ex: Throwable? = null) : File(ex)
        class Move(val from: FilePath, val to: FilePath) : File()
        class Rename(val path: FilePath, val newName: String) : File()
        class Copy(val from: FilePath, val to: FilePath, ex: Throwable? = null) : File(ex)
        class Delete(val path: FilePath, ex: Throwable? = null) : File(ex)
        class GetContentUri(val path: FilePath, ex: Throwable? = null) : File(ex)
        class Unknown(val path: FilePath, ex: Exception) : File(ex)
    }

    sealed class Folder(ex: Throwable? = null) : Failure(ex) {
        class NotExist(val path: FilePath) : Folder()
        class Create(val path: FilePath, ex: Throwable? = null) : Folder(ex)
        class Get(val path: FilePath, ex: Throwable? = null) : Folder(ex)
        class GetFolderSize(val path: FilePath, ex: Throwable? = null) : Folder(ex)
        class Move(val from: FilePath, val to: FilePath) : Folder()
        class Copy(val from: FilePath, val to: FilePath) : Folder()
        class Clear(val path: FilePath, ex: Throwable? = null) : Folder(ex)
        class Delete(val path: FilePath, ex: Throwable? = null) : Folder(ex)
        class Unknown(val path: FilePath, ex: Throwable) : Folder(ex)
    }

    sealed class Network(ex: Throwable? = null) : Failure(ex) {
        class DownloadWebPageError(ex: Throwable) : Network(ex)
        class DownloadImageError(ex: Throwable) : Network(ex)
        class DownloadFileError(ex: Throwable) : Network(ex)
    }

}
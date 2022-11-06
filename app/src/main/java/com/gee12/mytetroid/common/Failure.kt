package com.gee12.mytetroid.common

sealed class Failure(val ex: Throwable? = null) {

    data class ArgumentIsEmpty(
        val clazz: String = "",
        val method: String = "",
        val argument: String = ""
    ) : Failure()

    class UnknownError(ex: Throwable? = null) : Failure(ex)

    sealed class Storage(ex: Throwable? = null) : Failure(ex) {
        class Create(val storageName: String, ex: Throwable? = null) : Storage(ex)
        class Init(val storagePath: String, ex: Throwable?) : Storage(ex)

        sealed class Load(ex: Throwable? = null) : Storage(ex) {
            class ReadXmlFile(val storagePath: String, ex: Throwable?) : Load(ex)
            data class XmlFileNotExist(val storagePath: String) : Load()
        }
        sealed class Save(ex: Throwable? = null) : Storage(ex) {
            class SaveXmlFile(val storagePath: String, ex: Throwable? = null) : Save(ex)
            object RemoveOldXmlFile : Save()
            object RenameXmlFileFromTempName : Save()
        }

        sealed class DatabaseConfig(ex: Throwable? = null) : Storage(ex) {
            class Load(val storagePath: String, ex: Throwable? = null) : DatabaseConfig(ex)
        }

    }

    sealed class Node(ex: Throwable? = null) : Failure(ex) {
        sealed class Create(ex: Throwable? = null) : Node(ex) {
            object NameIsEmpty : Create()
        }
    }

    sealed class Tags(ex: Throwable? = null) : Failure(ex) {
        sealed class Parse(ex: Throwable? = null) : Node(ex) {
            object TagIsEmpty : Create()
        }
    }

    sealed class Encrypt(ex: Throwable? = null) : Failure(ex) {
        class File(ex: Throwable? = null) : Encrypt(ex)
    }

    sealed class Decrypt(ex: Throwable? = null) : Failure(ex) {
        class File(ex: Throwable? = null) : Decrypt(ex)
    }

    sealed class File(val path: String, ex: Throwable? = null) : Failure(ex) {
        class IsMissing(path: String) : File(path)
        class AccessDenied(path: String, ex: Throwable? = null) : File(path, ex)
        class GetFileSize(path: String, ex: Throwable? = null) : File(path, ex)
        class GetFolderSize(path: String, ex: Throwable? = null) : File(path, ex)
    }

}
package com.gee12.mytetroid.usecase.attach

import android.net.Uri
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.FileUtils
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.providers.IRecordPathProvider
import com.gee12.mytetroid.providers.IResourcesProvider
import com.gee12.mytetroid.providers.IDataNameProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.usecase.record.CheckRecordFolderUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.ArrayList

class CreateAttachToRecordUseCase(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val recordPathProvider: IRecordPathProvider,
    private val storageCrypter: IStorageCrypter,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<TetroidFile, CreateAttachToRecordUseCase.Params>() {

    data class Params(
        val fullName: String,
        val record: TetroidRecord,
        val deleteSrcFile: Boolean = false,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidFile> {
        val fullName = params.fullName
        val record = params.record

        if (fullName.isEmpty()) {
            return Failure.Attach.NameIsEmpty.toLeft()
        }
        logger.logOperStart(LogObj.FILE, LogOper.ATTACH, ": $fullName")
        val id = dataNameProvider.createUniqueId()
        // проверка исходного файла
        val srcFile = File(fullName)
        if (!srcFile.exists()) {
            return Failure.File.NotExist(path = srcFile.path).toLeft()
        }

        val fileDisplayName = srcFile.name
        val ext = FileUtils.getExtensionWithComma(fileDisplayName)
        val fileIdName = id + ext
        // создание объекта хранилища
        val isCrypted = record.isCrypted
        val attach = TetroidFile(
            isCrypted,
            id,
            encryptFieldIfNeed(fileDisplayName, isCrypted),
            TetroidFile.DEF_FILE_TYPE,
            record
        )
        if (isCrypted) {
            attach.setDecryptedName(fileDisplayName)
            attach.setIsDecrypted(true)
        }
        // проверка каталога записи
        val folderPath = recordPathProvider.getPathToRecordFolder(record)
        checkRecordFolderUseCase.run(
            CheckRecordFolderUseCase.Params(
                folderPath = folderPath,
                isCreate = true,
                showMessage = true,
            )
        ).onFailure {
            return it.toLeft()
        }
        // формируем путь к файлу назначения в каталоге записи
        val destFilePath = makePath(folderPath, fileIdName)
        val destFileUri = try {
            Uri.parse(destFilePath)
        } catch (ex: Exception) {
//            logger.logError(resourcesProvider.getString(R.string.log_error_generate_file_path) + destFilePath, ex)
            return Failure.File.CreateUriPath(path = destFilePath).toLeft()
        }
        // копирование файла в каталог записи, зашифровуя при необходимости
        val destFile = File(destFileUri.path.orEmpty())
//        val fromTo = resourcesProvider.getStringFromTo(fullName, destFilePath)
        if (record.isCrypted) {
            logger.logOperStart(LogObj.FILE, LogOper.ENCRYPT)
            try {
                if (!storageCrypter.encryptDecryptFile(
                        srcFile = srcFile,
                        destFile = destFile,
                        encrypt = true
                    )
                ) {
//                    logger.logOperError(LogObj.FILE, LogOper.ENCRYPT, fromTo, false, false)
                    return Failure.Encrypt.File(fileName = srcFile.path).toLeft()
                }
            } catch (ex: IOException) {
                return Failure.Encrypt.File(fileName = srcFile.path).toLeft()
            }
        } else {
            logger.logOperStart(LogObj.FILE, LogOper.COPY)
            try {
                if (!FileUtils.copyFile(srcFile, destFile)) {
//                    logger.logOperError(LogObj.FILE, LogOper.COPY, fromTo, false, false)
                    return Failure.File.Copy(filePath = srcFile.path, newPath = destFile.path).toLeft()
                }
            } catch (ex: IOException) {
                return Failure.File.Copy(filePath = srcFile.path, newPath = destFile.path).toLeft()
            }
        }

        // добавляем файл к записи (и соответственно, в дерево)
        var files = record.attachedFiles
        if (files == null) {
            files = ArrayList()
            record.attachedFiles = files
        }
        files.add(attach)
        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                if (params.deleteSrcFile) {
                    val srcFile = File(fullName)
                    if (!FileUtils.deleteRecursive(srcFile)) {
                        logger.logError(resourcesProvider.getString(R.string.error_delete_file_by_path_mask, srcFile.name, fullName))
                    }
                }
                attach.toRight()
            }
            .onFailure {
                logger.logOperCancel(LogObj.FILE, LogOper.ATTACH)
                // удаляем файл из записи
                files.remove(attach)
                // удаляем файл
                destFile.delete()
            }
    }

    private fun encryptFieldIfNeed(fieldValue: String, isEncrypt: Boolean): String? {
        return if (isEncrypt) storageCrypter.encryptTextBase64(fieldValue) else fieldValue
    }

}
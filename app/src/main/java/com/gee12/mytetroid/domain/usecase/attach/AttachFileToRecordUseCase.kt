package com.gee12.mytetroid.domain.usecase.attach

import android.content.Context
import android.net.Uri
import com.anggrayudi.storage.file.*
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.common.extensions.withExtension
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.domain.usecase.file.CopyFileWithCryptUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordFolderUseCase
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidRecord

class AttachFileToRecordUseCase(
    private val context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger,
    private val dataNameProvider: IDataNameProvider,
    private val cryptManager: IStorageCryptManager,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
    private val copyFileWithCryptUseCase: CopyFileWithCryptUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<TetroidFile, AttachFileToRecordUseCase.Params>() {

    data class Params(
        val fileUri: Uri,
        val record: TetroidRecord,
        val deleteSrcFile: Boolean = false,
    )

    override suspend fun run(params: Params): Either<Failure, TetroidFile> {
        val record = params.record
        val srcFileUri = params.fileUri
        val srcFileFullPath = FilePath.FileFull(srcFileUri.path.orEmpty())

        logger.logOperStart(LogObj.FILE, LogOper.ATTACH, ": $srcFileUri")
        val id = dataNameProvider.createUniqueId()

        val srcFile = DocumentFileCompat.fromUri(
            context = context,
            uri = srcFileUri,
        ) ?: return Failure.File.Get(srcFileFullPath).toLeft()

        // проверка exists почему-то не проходит, например с таким uri:
        // content://com.miui.gallery.open/raw/%2Fstorage%2F3B01-1206%2FDCIM%2FCamera%2FIMG_20231102_092915.jpg
        /*if (!srcFile.exists()) {
            return Failure.File.NotExist(srcFileFullPath).toLeft()
        }*/

        val fileDisplayName = srcFile.name ?: "unknown"
        val ext = fileDisplayName.getExtensionWithoutComma()
        val fileIdName = id.withExtension(ext)

        // создание объекта хранилища
        val isEncrypted = record.isCrypted
        val attach = TetroidFile(
            isEncrypted,
            id,
            encryptFieldIfNeed(fileDisplayName, isEncrypted),
            TetroidFile.DEF_FILE_TYPE,
            record
        )
        if (isEncrypted) {
            attach.setDecryptedName(fileDisplayName)
            attach.setIsDecrypted(true)
        }

        // формируем путь к файлу назначения в каталоге записи
        val recordFolder = getRecordFolderUseCase.run(
            GetRecordFolderUseCase.Params(
                record = record,
                createIfNeed = true,
                inTrash = record.isTemp,
                showMessage = true,
            )
        ).foldResult(
            onLeft = {
                return it.toLeft()
            },
            onRight = { it }
        )
        val destFilePath = FilePath.File(recordFolder.getAbsolutePath(context), fileIdName)

        val destFile = recordFolder.makeFile(
            context = context,
            name = fileIdName,
            mimeType = MimeType.UNKNOWN,
            mode = CreateMode.REPLACE,
        ) ?: return Failure.File.Create(destFilePath).toLeft()

        // копирование файла в каталог записи, зашифровуя при необходимости
        copyFileWithCryptUseCase.run(
            CopyFileWithCryptUseCase.Params(
                srcFile = srcFile,
                destFile = destFile,
                isEncrypt = attach.isCrypted,
                isDecrypt = false,
            )
        ).onFailure {
            return it.toLeft()
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
                    if (!srcFile.delete()) {
                        logger.logWarning(resourcesProvider.getString(R.string.error_delete_file_mask, srcFileFullPath.fullPath), show = true)
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
        return if (isEncrypt) cryptManager.encryptTextBase64(fieldValue) else fieldValue
    }

}
package com.gee12.mytetroid.domain.usecase.script

import android.content.Context
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.MimeType
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.openOutputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.usecase.file.GetFolderUseCase
import com.gee12.mytetroid.model.FilePath
import java.io.PrintStream

class SaveScriptTextToFileUseCase(
    private val context: Context,
    private val storagePathProvider: IStoragePathProvider,
    private val getFolderUseCase: GetFolderUseCase,
) : UseCase<UseCase.None, SaveScriptTextToFileUseCase.Params>() {

    data class Params(
        val scriptFileName: String,
        val scriptText: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val scriptFileName = params.scriptFileName
        val scriptText = params.scriptText
        val scriptsFolderPath = storagePathProvider.getPathToScriptsFolder()

        val scriptsFolder = getFolderUseCase.run(
            GetFolderUseCase.Params(
                path = scriptsFolderPath,
                isCreateIfNotExist = true,
            )
        ).foldResult(
            onLeft = { failure ->
                return failure.toLeft()
            },
            onRight = { it }
        )

        val scriptFilePath = FilePath.File(scriptsFolderPath, scriptFileName)
        val scriptFile = scriptsFolder.makeFile(
            context = context,
            name = scriptFileName,
            mimeType = MimeType.TEXT,
            mode = CreateMode.REPLACE,
        ) ?: return Failure.File.Get(scriptFilePath).toLeft()

        return try {
            scriptFile.openOutputStream(context, append = false)?.use { outputStream ->
                PrintStream(outputStream).use { printStream ->
                    printStream.print(scriptText)

                    None.toRight()
                }
            } ?: return Failure.File.Write(scriptFilePath).toLeft()
        } catch (ex: Exception) {
            Failure.File.Write(scriptFilePath, ex).toLeft()
        }
    }

}
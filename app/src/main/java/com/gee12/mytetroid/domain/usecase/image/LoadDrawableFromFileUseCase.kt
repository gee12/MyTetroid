package com.gee12.mytetroid.domain.usecase.image

import android.content.Context
import android.graphics.drawable.Drawable
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.extension
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.openInputStream
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makeFolderPath
import com.gee12.mytetroid.domain.provider.IStoragePathProvider
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.FilePath
import com.gee12.mytetroid.model.ImageFileType
import com.larvalabs.svgandroid.SVGParser
import java.io.InputStream
import java.lang.Exception

class LoadDrawableFromFileUseCase(
    private val context: Context,
    private val logger: ITetroidLogger,
    private val storageProvider: IStorageProvider,
    private val storagePathProvider: IStoragePathProvider,
) : UseCase<Drawable, LoadDrawableFromFileUseCase.Params>() {

    data class Params(
        val relativeIconPath: String,
    )

    override suspend fun run(params: Params): Either<Failure, Drawable> {
        val iconPath = params.relativeIconPath
        val storageFolder = storageProvider.rootFolder
        val storageFolderPath = storageFolder?.getAbsolutePath(context).orEmpty()
        val iconFileRelativePath = makeFolderPath(storagePathProvider.getRelativePathToIconsFolder(), iconPath)
        val iconFilePath = FilePath.Folder(storageFolderPath, iconFileRelativePath)

        val iconFile = storageFolder?.child(
            context = context,
            path = iconFileRelativePath,
            requiresWriteAccess = false,
        ) ?: return Failure.Folder.Get(iconFilePath).toLeft()

        return ImageFileType.fromExtension(extension = iconFile.extension)?.let { iconType ->
            iconFile.openInputStream(context)?.use { inputStream ->
                loadIcon(
                    filePath = iconFilePath,
                    iconType = iconType,
                    inputStream = inputStream,
                )
            } ?: Failure.File.Read(iconFilePath).toLeft()
        } ?: Failure.Image.UnknownType(type = iconFile.extension).toLeft()
    }

    private fun loadIcon(
        filePath: FilePath,
        iconType: ImageFileType,
        inputStream: InputStream
    ): Either<Failure, Drawable> {
        return try {
            when (iconType) {
                ImageFileType.SVG -> {
                    SVGParser.getSVGFromInputStream(inputStream).createPictureDrawable().toRight()
                }
                ImageFileType.PNG -> {
                    Drawable.createFromStream(inputStream, "src")?.toRight()
                        ?: Failure.Image.LoadFromFile(filePath).toLeft()
                }
            }
        } catch (ex: Exception) {
            logger.logError(ex, show = false)
            Failure.Image.LoadFromFile(filePath, ex).toLeft()
        }
    }

}
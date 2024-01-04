package com.gee12.mytetroid.domain.usecase.network

import android.net.Uri
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.utils.UriUtils
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IDataNameProvider
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL

/**
 * Загрузка файла по URL в каталог кэша на устройстве.
 */
class DownloadFileFromWebUseCase(
    private val appPathProvider: IAppPathProvider,
    private val dataNameProvider: IDataNameProvider,
) : UseCase<Uri, DownloadFileFromWebUseCase.Params>() {

    data class Params(
        val url: String,
    )

    override suspend fun run(params: Params): Either<Failure, Uri> {
        val fileName = UriUtils.getFileName(params.url)
            .ifEmpty { dataNameProvider.createDateTimePrefix() }

        val pathToCacheFolder = appPathProvider.getPathToCacheFolder().fullPath
        val outputFileName = makePath(pathToCacheFolder, fileName)

        return try {
            val url = URL(params.url)
            url.openStream().use { inputStream ->
                DataInputStream(inputStream).use { dis ->
                    FileOutputStream(outputFileName).use { fos ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (dis.read(buffer).also { length = it } > 0) {
                            fos.write(buffer, 0, length)
                        }
                        fos.flush()
                    }
                }
            }
            Uri.fromFile(File(outputFileName)).toRight()
        } catch (ex: Exception) {
            Failure.Network.DownloadFileError(ex).toLeft()
        }
    }

}
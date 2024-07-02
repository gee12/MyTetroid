package com.gee12.mytetroid.domain.usecase.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.common.network.NetworkHelper


/**
 * Загрузка содержимого изображения по URL.
 */
class DownloadImageFromWebUseCase : UseCase<Bitmap, DownloadImageFromWebUseCase.Params>() {

    data class Params(
        val url: String,
        val connectTimeoutMillis: Int = 5000,
    )

    override suspend fun run(params: Params): Either<Failure, Bitmap> {
        return try {
            val connection = NetworkHelper.createURLConnection(
                url = params.url,
                connectTimeout = params.connectTimeoutMillis,
            )
            val bitmap = connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
            bitmap.toRight()
        } catch (ex: Exception) {
            Failure.Network.DownloadImageError(ex).toLeft()
        }
    }

}
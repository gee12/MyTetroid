package com.gee12.mytetroid.domain.usecase.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gee12.mytetroid.common.*
import java.lang.Exception
import java.net.URL

/**
 * Загрузка содержимого изображения по URL.
 */
class DownloadImageFromWebUseCase : UseCase<Bitmap, DownloadImageFromWebUseCase.Params>() {

    data class Params(
        val url: String,
    )

    override suspend fun run(params: Params): Either<Failure, Bitmap> {
        return try {
            val bitmap = URL(params.url).openStream().use { input ->
                BitmapFactory.decodeStream(input)
            }
            bitmap.toRight()
        } catch (ex: Exception) {
            Failure.Network.DownloadImageError(ex).toLeft()
        }
    }

}
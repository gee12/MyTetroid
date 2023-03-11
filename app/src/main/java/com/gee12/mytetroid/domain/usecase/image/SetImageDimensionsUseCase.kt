package com.gee12.mytetroid.domain.usecase.image

import android.graphics.BitmapFactory
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.model.TetroidImage
import java.io.InputStream

class SetImageDimensionsUseCase(
) : UseCase<UseCase.None, SetImageDimensionsUseCase.Params>() {

    data class Params(
        val image: TetroidImage,
        val inputStream: InputStream,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val image = params.image
        val inputStream = params.inputStream

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        image.apply {
            width = options.outWidth
            height = options.outHeight
        }

        return None.toRight()
    }

}
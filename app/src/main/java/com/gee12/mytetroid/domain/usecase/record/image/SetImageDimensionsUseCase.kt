package com.gee12.mytetroid.domain.usecase.record.image

import android.graphics.BitmapFactory
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.extensions.makePath
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.model.TetroidImage

class SetImageDimensionsUseCase(
) : UseCase<UseCase.None, SetImageDimensionsUseCase.Params>() {

    data class Params(
        val image: TetroidImage,
        val recordFolderPath: String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val image = params.image

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val imageFullName = makePath(params.recordFolderPath, image.name)
        BitmapFactory.decodeFile(imageFullName, options)
        image.width = options.outWidth
        image.height = options.outHeight

        return None.toRight()
    }

}
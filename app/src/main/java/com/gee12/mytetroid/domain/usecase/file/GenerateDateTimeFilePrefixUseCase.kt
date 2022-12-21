package com.gee12.mytetroid.domain.usecase.file

// FIXME: или не нужен кейс ?
//class GenerateDateTimeFilePrefixUseCase(
//
//) : UseCase<String, GenerateDateTimeFilePrefixUseCase.Params>() {
//
//    companion object {
//        const val PREFIX_DATE_TIME_FORMAT = "yyyyMMddHHmmssSSS"
//    }
//
//    object Params
//
//    suspend fun run(): Either<Failure, String> {
//        return run(Params)
//    }
//
//    override suspend fun run(params: Params): Either<Failure, String> {
//        return try {
//            SimpleDateFormat(PREFIX_DATE_TIME_FORMAT, Locale.getDefault()).format(Date()).toRight()
//        } catch (ex: Exception) {
//            Failure.File.UnknownError(ex).toLeft()
//        }
//    }
//
//}
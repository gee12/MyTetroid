//package com.gee12.mytetroid.domain.usecase.record
package android.print

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import com.gee12.mytetroid.common.*

/**
 * Экспорт документа в файл.
 * Запускать в Main потоке.
 * Указан неверный package, т.к. PrintDocumentAdapter.LayoutResultCallback - package private.
 */
class PrintDocumentToFileUseCase(
    private val context: Context,
) : UseCase<UseCase.None, PrintDocumentToFileUseCase.Params>() {

    data class Params(
        val printAdapter: PrintDocumentAdapter,
        val printAttributes: PrintAttributes,
        val uri: Uri,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            var result: Either<Failure, None> = None.toRight()
            val printAdapter = params.printAdapter

            printAdapter.onLayout(
                null,
                params.printAttributes,
                null,
                object : PrintDocumentAdapter.LayoutResultCallback() {

                    override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                        result = getOutputFile(uri = params.uri).flatMap { fileDescriptor ->
                            writeToFile(printAdapter, fileDescriptor)
                        }
                    }
                },
                null
            )
            result
        } else {
            return Failure.RequiredApiVersion(minApiVersion = Build.VERSION_CODES.KITKAT).toLeft()
        }
    }

    private fun getOutputFile(uri: Uri): Either<Failure, ParcelFileDescriptor?> {
        return try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "rw")
            Either.Right(fileDescriptor)
        } catch (ex: Exception) {
            Failure.Record.ExportToPdf(ex).toLeft()
        }
    }

    private fun writeToFile(
        printAdapter: PrintDocumentAdapter,
        fileDescriptor: ParcelFileDescriptor?,
    ): Either<Failure, None> {
        var result: Either<Failure, None> = None.toRight()

        printAdapter.onWrite(
            arrayOf(PageRange.ALL_PAGES),
            fileDescriptor,
            CancellationSignal(),
            object : PrintDocumentAdapter.WriteResultCallback() {

                override fun onWriteFinished(pages: Array<PageRange>) {
                    super.onWriteFinished(pages)
                    result = if (pages.isNotEmpty()) {
                        None.toRight()
                    } else {
                        Failure.Record.ExportToPdf().toLeft()
                    }
                }

            }
        )
        return result
    }

}
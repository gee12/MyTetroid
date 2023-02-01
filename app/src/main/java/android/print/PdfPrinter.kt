package android.print

import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.File

class PdfPrinter(
    private val printAttributes: PrintAttributes,
    private val onSuccess: () -> Unit,
    private val onFailure: (ex: Throwable?) -> Unit,
) {

    fun print(
        printAdapter: PrintDocumentAdapter,
        folder: File,
        fileName: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            printAdapter.onLayout(
                null,
                printAttributes,
                null,
                object : PrintDocumentAdapter.LayoutResultCallback() {

                    override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            printAdapter.onWrite(
                                arrayOf(PageRange.ALL_PAGES),
                                getOutputFile(folder, fileName),
                                CancellationSignal(),
                                object : PrintDocumentAdapter.WriteResultCallback() {

                                    override fun onWriteFinished(pages: Array<PageRange>) {
                                        super.onWriteFinished(pages)
                                        if (pages.isNotEmpty()) {
                                            onSuccess()
                                        } else {
                                            onFailure(null)
                                        }

                                    }

                                }
                            )
                        }
                    }
                },
                null
            )
        }
    }

    private fun getOutputFile(path: File, fileName: String): ParcelFileDescriptor? {
        return try {
            if (!path.exists()) {
                path.mkdirs()
            }
            val file = File(path, fileName)
            file.createNewFile()
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        } catch (ex: Exception) {
            onFailure(ex)
            null
        }
    }

}
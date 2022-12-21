package com.gee12.mytetroid.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL

object NetworkHelper {

    suspend fun downloadWebPageContentAsync(url: String?, isTextOnly: Boolean, callback: IWebPageContentResult?) {
        withContext(Dispatchers.IO) {
            try {
                val conn = Jsoup.connect(url)
                val doc = conn.get()
                var content = ""
                val body = doc.body()
                if (body != null) {
                    content = if (isTextOnly) {
                        HtmlHelper.elementToText(body)
                    } else {
                        body.html()
                    }
                }
                callback?.onSuccess(content, isTextOnly)
            } catch (ex: Exception) {
                callback?.onError(ex)
            }
        }
    }

    suspend fun downloadImageAsync(url: String?, callback: IWebImageResult?) {
        withContext(Dispatchers.IO) {
            try {
                val input = URL(url).openStream()
                val bitmap = BitmapFactory.decodeStream(input)
                callback?.onSuccess(bitmap)
            } catch (ex: Exception) {
                callback?.onError(ex)
            }
        }
    }

    suspend fun downloadFileAsync(url: String?, outputFile: String?, callback: IWebFileResult?) {
        withContext(Dispatchers.IO) {
            try {
                val u = URL(url)
                val `is` = u.openStream()
                val dis = DataInputStream(`is`)
                val fos = FileOutputStream(outputFile)
                val buffer = ByteArray(1024)
                var length: Int
                while (dis.read(buffer).also { length = it } > 0) {
                    fos.write(buffer, 0, length)
                }
                fos.flush()
                fos.close()
                dis.close()
                callback?.onSuccess()
            } catch (ex: Exception) {
                callback?.onError(ex)
            }
        }
    }

    interface IWebPageContentResult {
        fun onSuccess(content: String, isTextOnly: Boolean)
        fun onError(ex: Exception)
    }

    interface IWebImageResult {
        fun onSuccess(bitmap: Bitmap)
        fun onError(ex: Exception)
    }

    interface IWebFileResult {
        fun onSuccess()
        fun onError(ex: Exception)
    }
}
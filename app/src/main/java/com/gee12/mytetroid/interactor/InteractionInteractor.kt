package com.gee12.mytetroid.interactor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.ILogger
import com.gee12.mytetroid.logs.LogManager
import com.gee12.mytetroid.utils.FileUtils
import org.jsoup.internal.StringUtil
import java.io.File

class InteractionInteractor {

    /**
     * Отправка текста в стороннее приложение.
     * @param context
     * @param subject
     * @param text
     * @return
     */
    fun shareText(context: Context?, subject: String?, text: String?): Boolean {
        if (context == null) return false
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        //        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_using)));
        // всегда отображать диалог выбора приложения (не использовать выбор по-умолчанию)
        val chooser = Intent.createChooser(intent, context.getString(R.string.title_send_to))
        try {
            // проверить, есть ли подходящее приложение для открытия файла
            if (intent.resolveActivity(context.packageManager) != null) {
//                    context.startActivity(intent);
                context.startActivity(chooser)
            } else {
                LogManager.log(context, context.getString(R.string.log_no_app_found_for_share_text), Toast.LENGTH_LONG)
                return false
            }
        } catch (ex: ActivityNotFoundException) {
            LogManager.log(context, context.getString(R.string.log_no_app_found_for_share_text), Toast.LENGTH_LONG)
            return false
        }
        return true
    }

    /**
     * Открытие файла/каталога сторонним приложением.
     * @param context
     * @param file
     * @return
     */
    fun openFile(context: Context, file: File?): Boolean {
        if (file == null) {
            LogManager.emptyParams(context, "DataManager.openFile()")
            return false
        }
        val fullFileName = file.absolutePath

        // Начиная с API 24 (Android 7), для предоставления доступа к файлам, который
        // ассоциируется с приложением (для открытия файла другими приложениями с помощью Intent, короче),
        // нужно использовать механизм FileProvider.
        // Путь к файлу должен быть сформирован так: content://<Uri for a file>
        val fileUri: Uri
        fileUri = try {
            if (Build.VERSION.SDK_INT >= 24) {
                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
            } else {
                Uri.fromFile(file)
            }
        } catch (ex: Exception) {
            LogManager.log(
                context, context.getString(R.string.log_file_sharing_error) + fullFileName,
                ILogger.Types.ERROR, Toast.LENGTH_LONG
            )
            LogManager.log(context, ex)
            return false
        }
        // grant permision for app with package "packageName", eg. before starting other app via intent
        context.grantUriPermission(context.packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        //revoke permisions
//            context.revokeUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        val intent = Intent(Intent.ACTION_VIEW)
        var mimeType: String?
        return if (file.isDirectory) {
            //            intent = new Intent(Intent.ACTION_GET_CONTENT); // открывается com.android.documentsui, но без каталога
            //            mimeType = "*/*";   // отображается список приложений, но не для открытия каталога
            //            mimeType = "application/*"; // тоже самое
            mimeType = "resource/folder"
            //            mimeType = DocumentsContract.Document.MIME_TYPE_DIR; // открывается com.android.documentsui

            //            Uri selectedUri = Uri.fromFile(file.getAbsoluteFile());
            //            String fileExtension =  MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
            //            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            intent.setDataAndType(fileUri, mimeType)
            if (!openFile(context, file, intent, false)) {
                mimeType = "*/*"
                //                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setDataAndType(fileUri, mimeType)
                return openFile(context, file, intent, true)
            }
            true
        } else {
            val ext = FileUtils.getExtensionWithComma(fullFileName)
            // определяем тип файла по расширению, если оно есть
            mimeType = if (!StringUtil.isBlank(ext) && ext.length > 1) MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.substring(1)) else "text/plain"
            intent.setDataAndType(fileUri, mimeType)
            openFile(context, file, intent, true)
        }
    }

    /**
     * Открытие файла/каталога сторонним приложением.
     * @param context
     * @param file
     * @param intent
     * @return
     */
    fun openFile(context: Context?, file: File?, intent: Intent?, needLog: Boolean): Boolean {
        if (context == null || file == null || intent == null) {
            LogManager.emptyParams(context, "DataManager.openFile()")
            return false
        }
        val fileFullName = file.absolutePath
        // устанавливаем флаг для того, чтобы дать внешнему приложению пользоваться нашим FileProvider
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // всегда отображать диалог выбора приложения (не использовать выбор по-умолчанию)
        val chooser = Intent.createChooser(intent, context.getString(R.string.title_open_with))
        try {
            // проверить, есть ли подходящее приложение для открытия файла
            if (intent.resolveActivity(context.packageManager) != null) {
//                    context.startActivity(intent);
                context.startActivity(chooser)
            } else {
                if (needLog) {
                    LogManager.log(context, context.getString(R.string.log_no_app_found_for_open_file) + fileFullName, Toast.LENGTH_LONG)
                }
                return false
            }
        } //        catch (ActivityNotFoundException ex) {
        catch (ex: Exception) {
            if (needLog) {
                LogManager.log(context, context.getString(R.string.log_error_file_open) + fileFullName, Toast.LENGTH_LONG)
            }
            return false
        }
        return true
    }

}
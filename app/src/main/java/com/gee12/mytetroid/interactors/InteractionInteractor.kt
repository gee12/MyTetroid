package com.gee12.mytetroid.interactors

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.gee12.mytetroid.BuildConfig
import com.gee12.mytetroid.R
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.common.utils.FileUtils
import org.jsoup.internal.StringUtil
import java.io.File

/**
 * Не зависит от конкретного хранилища, может быть Singleton.
 */
class InteractionInteractor(
    private val logger: ITetroidLogger
) {

    /**
     * Отправка текста в стороннее приложение.
     * @param context
     * @param subject
     * @param text
     * @return
     */
    fun shareText(context: Context, subject: String?, text: String?): Boolean {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        // всегда отображать диалог выбора приложения (не использовать выбор по-умолчанию)
        val chooser = Intent.createChooser(intent, context.getString(R.string.title_send_to))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            // проверить, есть ли подходящее приложение для открытия файла
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            } else {
                logger.logWarning(context.getString(R.string.log_no_app_found_for_share_text), true)
                return false
            }
        } catch (ex: Exception) {
            logger.logWarning(context.getString(R.string.log_no_app_found_for_share_text), true)
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
            logger.logEmptyParams("DataManager.openFile()")
            return false
        }
        val fullFileName = file.absolutePath

        // Начиная с API 24 (Android 7), для предоставления доступа к файлам, который
        // ассоциируется с приложением (для открытия файла другими приложениями с помощью Intent, короче),
        // нужно использовать механизм FileProvider.
        // Путь к файлу должен быть сформирован так: content://<Uri for a file>
        val fileUri: Uri = try {
            if (Build.VERSION.SDK_INT >= 24) {
                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
            } else {
                Uri.fromFile(file)
            }
        } catch (ex: Exception) {
            logger.logError(context.getString(R.string.log_file_sharing_error) + fullFileName, true)
            logger.logError(ex, false)
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
            logger.logEmptyParams("DataManager.openFile()")
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
//                context.startActivity(intent);
                context.startActivity(chooser)
            } else {
                if (needLog) {
                    logger.log(context.getString(R.string.log_no_app_found_for_open_file) + fileFullName, true)
                }
                return false
            }
        }
//        catch (ActivityNotFoundException ex) {
        catch (ex: Exception) {
            if (needLog) {
                logger.log(context.getString(R.string.log_error_file_open) + fileFullName, true)
            }
            return false
        }
        return true
    }

}
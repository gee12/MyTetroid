package com.gee12.mytetroid.domain.interactor

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
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import java.io.File

/**
 * Не зависит от конкретного хранилища, может быть Singleton.
 */
class InteractionInteractor(
    private val resourcesProvider: IResourcesProvider,
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
        val chooser = Intent.createChooser(intent, resourcesProvider.getString(R.string.title_send_to))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            // проверить, есть ли подходящее приложение для открытия файла
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            } else {
                logger.logWarning(resourcesProvider.getString(R.string.log_no_app_found_for_share_text), true)
                return false
            }
        } catch (ex: Exception) {
            logger.logWarning(resourcesProvider.getString(R.string.log_no_app_found_for_share_text), true)
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
    fun openFile(context: Context, file: File): Boolean {
        val fileName = file.absolutePath

        // Начиная с API 24 (Android 7), для предоставления доступа к файлам, который
        // ассоциируется с приложением (для открытия файла другими приложениями с помощью Intent, короче),
        // нужно использовать механизм FileProvider.
        // Путь к файлу должен быть сформирован так: content://<Uri for a file>
        val fileUri = try {
            if (Build.VERSION.SDK_INT >= 24) {
                FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
            } else {
                Uri.fromFile(file)
            }
        } catch (ex: Exception) {
            logger.logError(resourcesProvider.getString(R.string.log_file_sharing_error) + fileName, true)
            logger.logError(ex, false)
            return false
        }
        // grant permision for app with package "packageName", eg. before starting other app via intent
//        context.grantUriPermission(context.packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        //revoke permisions
//        context.revokeUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

//        return if (file.isDirectory) {
//            openFolder(context, file, fileUri)
//        } else {
//            openFile(context, file, fileUri)
//        }
        val intent = Intent(Intent.ACTION_VIEW)
//        intent.setDataAndType(fileUri, "resource/folder")
//        intent.putExtra("org.openintents.extra.ABSOLUTE_PATH", fileName)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
//        if (intent.resolveActivity(context.packageManager) != null) {
//            context.startActivity(intent)
//        } else {
            // Try a second way to find a compatible file explorer app.
            intent.setDataAndType(fileUri, "application/*")
            val chooserIntent = Intent.createChooser(intent, resourcesProvider.getString(R.string.title_open_with))
            if (chooserIntent != null) {
                context.startActivity(chooserIntent)
            } else {
//                Toast.makeText(mContext, R.string.toast_no_file_manager, Toast.LENGTH_SHORT).show()
            }
//        }
        return true
    }

    private fun openFolder(context: Context, file: File, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        // open with OpenIntents "oi file manager"
        intent.setDataAndType(uri, "resource/folder")
        intent.putExtra("org.openintents.extra.ABSOLUTE_PATH", file.path)

        if (!openFileWithIntent(context, file, intent, createChooser = false, writeLog = false)) {
            // try a second way to find a compatible file explorer app
            intent.setDataAndType(uri, "application/*")
            return openFileWithIntent(context, file, intent, createChooser = true, writeLog = true)
        }
        return true
    }

    private fun openFile(context: Context, file: File, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        val ext = FileUtils.getExtensionWithComma(file.absolutePath)
        // определяем тип файла по расширению, если оно есть
        val mimeType = if (ext.isNotBlank() && ext.length > 1)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.substring(1))
        else "text/plain"
        intent.setDataAndType(uri, mimeType)
        return openFileWithIntent(context, file, intent, createChooser = true, writeLog = true)
    }

    /**
     * Открытие файла/каталога сторонним приложением.
     * @param context
     * @param file
     * @param intent
     * @return
     */
    private fun openFileWithIntent(context: Context, file: File, intent: Intent, createChooser: Boolean, writeLog: Boolean): Boolean {
        val fileName = file.absolutePath
        // устанавливаем флаг для того, чтобы дать внешнему приложению пользоваться нашим FileProvider
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // всегда отображать диалог выбора приложения (не использовать выбор по-умолчанию)
        val destIntent = if (createChooser) {
            Intent.createChooser(intent, resourcesProvider.getString(R.string.title_open_with))
        } else {
            intent
        }

        return try {
            // проверить, есть ли подходящее приложение для открытия файла
            if (destIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(destIntent)
                true
            } else {
                if (writeLog) {
                    logger.log(resourcesProvider.getString(R.string.log_no_app_found_for_open_file) + fileName, true)
                }
                false
            }
        }
        catch (ex: Exception) { // ActivityNotFoundException
            if (writeLog) {
                logger.log(resourcesProvider.getString(R.string.log_error_file_open) + fileName, true)
            }
            false
        }
    }

}
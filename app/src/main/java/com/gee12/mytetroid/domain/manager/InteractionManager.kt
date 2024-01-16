package com.gee12.mytetroid.domain.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.getExtensionWithoutComma
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger


class InteractionManager(
    private val resourcesProvider: IResourcesProvider,
    private val logger: ITetroidLogger
) {

    /**
     * Отправка текста в стороннее приложение.
     */
    fun shareText(
        context: Context,
        subject: String?,
        text: String?
    ): Boolean {
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
                logger.logWarning(resourcesProvider.getString(R.string.log_no_app_found_for_share_text), show = true)
                return false
            }
        } catch (ex: Exception) {
            logger.logWarning(resourcesProvider.getString(R.string.log_no_app_found_for_share_text), show = true)
            return false
        }
        return true
    }

    /**
     * Открытие файла сторонним приложением.
     */
    fun openFile(
        activity: Activity,
        uri: Uri,
        mimeType: String
    ): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        // даем внешнему приложению пользоваться нашим FileProvider
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.setDataAndType(uri, mimeType)
        val chooserIntent = Intent.createChooser(intent, resourcesProvider.getString(R.string.title_open_with))
        if (chooserIntent != null) {
            activity.startActivity(chooserIntent)
        } else {
            logger.log(resourcesProvider.getString(R.string.log_no_app_found_for_open_file_mask, uri.path.orEmpty()), show = true)
        }
        return true
    }

    fun openFile(
        activity: Activity,
        uri: Uri
    ): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        val ext = uri.path?.getExtensionWithoutComma().orEmpty()
        // определяем тип файла по расширению, если оно есть
        val mimeType = if (ext.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        } else {
            "text/plain" // "application/*"
        }
        intent.setDataAndType(uri, mimeType)
        return openFileWithIntent(activity, uri, intent, createChooser = true, writeLog = true)
    }

    /**
     * Открытие каталога сторонним приложением.
     */
    fun openFolder(
        activity: Activity,
        uri: Uri
    ): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        // open with OpenIntents "oi file manager"
        intent.setDataAndType(uri, "resource/folder")
        intent.putExtra("org.openintents.extra.ABSOLUTE_PATH", uri.path)

        if (!openFileWithIntent(activity, uri, intent, createChooser = false, writeLog = false)) {
            // try a second way to find a compatible file explorer app
            intent.setDataAndType(uri, "application/*")
            return openFileWithIntent(activity, uri, intent, createChooser = true, writeLog = true)
        }
        return true
    }

    /**
     * Открытие файла/каталога сторонним приложением.
     */
    private fun openFileWithIntent(
        activity: Activity,
        uri: Uri,
        intent: Intent,
        createChooser: Boolean,
        writeLog: Boolean
    ): Boolean {
        // даем внешнему приложению пользоваться нашим FileProvider
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // всегда отображать диалог выбора приложения (не использовать выбор по-умолчанию)
        val destIntent = if (createChooser) {
            Intent.createChooser(intent, resourcesProvider.getString(R.string.title_open_with))
        } else {
            intent
        }

        return try {
            // проверить, есть ли подходящее приложение для открытия файла
            if (destIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(destIntent)
                true
            } else {
                if (writeLog) {
                    logger.log(resourcesProvider.getString(R.string.log_no_app_found_for_open_file_mask, uri.toString()), show = true)
                }
                false
            }
        }
        catch (ex: Exception) {
            // если использовать Context вместо Activity, то будет следующее:
            // ActivityNotFoundException:
            // Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?
            if (writeLog) {
                logger.log(resourcesProvider.getString(R.string.log_error_file_open_mask, uri.toString()), show = true)
            }
            false
        }
    }

}
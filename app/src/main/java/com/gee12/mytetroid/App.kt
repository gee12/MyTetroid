package com.gee12.mytetroid

import android.app.Activity
import android.content.Context
import androidx.annotation.ColorInt
import com.gee12.mytetroid.data.SettingsManager
import com.gee12.mytetroid.data.TetroidXml
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.utils.Utils
import com.gee12.mytetroid.utils.ViewUtils
import java.util.*

object App {
    var IsInited = false
    @JvmField
    var IsHighlightAttach = false
    @JvmField
    var IsHighlightCryptedNodes = false
    @ColorInt
    @JvmField
    var HighlightAttachColor = 0
    lateinit var DateFormatString: String
    lateinit var RecordFieldsInList: RecordFieldsSelector

    // FIXME: использовать di
    lateinit var storagesRepo: StoragesRepo
    lateinit var storageInteractor: StorageInteractor
    lateinit var xmlLoader: TetroidXml
    lateinit var logger: TetroidLogger

    fun isFullVersion() = BuildConfig.FLAVOR == "pro"

    fun isFreeVersion() = BuildConfig.FLAVOR == "free"

    fun isRusLanguage() = Locale.getDefault().language == "ru"

    /**
     * Переключатель блокировки выключения экрана.
     * @param activity
     */
    @JvmStatic
    fun checkKeepScreenOn(activity: Activity?) {
        ViewUtils.setKeepScreenOn(activity, SettingsManager.isKeepScreenOn(activity))
    }

    /**
     * Первоначальная инициализация компонентов приложения.
     */
    @JvmStatic
    fun init(context: Context, logger: TetroidLogger, xmlLoader: TetroidXml, storageInteractor: StorageInteractor) {
        if (IsInited) return

        App.logger = logger
        App.xmlLoader = xmlLoader
        App.storageInteractor = storageInteractor

        SettingsManager.init(context)
        logger.init(SettingsManager.getLogPath(context), SettingsManager.isWriteLogToFile(context))
        logger.log(context.getString(R.string.log_app_start_mask).format(Utils.getVersionName(context)))
        if (SettingsManager.isCopiedFromFree()) {
            logger.log(R.string.log_settings_copied_from_free)
        }
        TetroidXml.ROOT_NODE.name = context.getString(R.string.title_root_node)
        IsInited = true
    }
}
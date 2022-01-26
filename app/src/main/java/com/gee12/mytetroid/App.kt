package com.gee12.mytetroid

import android.app.Activity
import android.content.Context
import androidx.annotation.ColorInt
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.common.utils.Utils
import com.gee12.mytetroid.common.utils.ViewUtils
import java.util.*

object App {

    // TODO: перенести в TetroidEnvironment
    @JvmField
    var IsHighlightAttach = false
    @JvmField
    var IsHighlightCryptedNodes = false
    @ColorInt
    @JvmField
    var HighlightAttachColor = 0
    lateinit var DateFormatString: String
    lateinit var RecordFieldsInList: RecordFieldsSelector

    /**
     * Это простая реализация DI.
     * Да, это глобальный синглтон, что является плохим тоном.
     * Но при таком подходе:
     *  1) все зависимости расположены в 1 месте
     *  2) не нужно подключать дополнительную библиотеку DI
     */
    var current: TetroidEnvironment? = null
        private set

    fun isFullVersion() = BuildConfig.FLAVOR == "pro"

    fun isFreeVersion() = BuildConfig.FLAVOR == "free"

    fun isRusLanguage() = Locale.getDefault().language == "ru"

    /**
     * Переключатель блокировки выключения экрана.
     * @param activity
     */
    @JvmStatic
    fun checkKeepScreenOn(activity: Activity?) {
        ViewUtils.setKeepScreenOn(activity, CommonSettings.isKeepScreenOn(activity))
    }

    /**
     * Первоначальная инициализация компонентов приложения.
     */
    @JvmStatic
    fun init(
        context: Context,
        logger: TetroidLogger
    ) {
        if (current != null) return

        current = TetroidEnvironment(
            logger = logger
        )

        logger.writeRawString("************************************************************")
        logger.log(context.getString(R.string.log_app_start_mask).format(Utils.getVersionName(logger, context)), false)
        if (CommonSettings.isCopiedFromFree()) {
            logger.log(R.string.log_settings_copied_from_free, true)
        }
    }

    @JvmStatic
    fun initStorageData(storageData: TetroidStorageData) {
        current?.storageData = storageData
    }

    @JvmStatic
    fun resetStorageData() {
        current?.storageData = null
    }

    @JvmStatic
    fun destruct() {
        current?.apply {
//            logger = null
            storageData = null
        }
        current = null
    }
}
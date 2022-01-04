package com.gee12.mytetroid

import android.app.Activity
import android.content.Context
import androidx.annotation.ColorInt
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.data.xml.TetroidXml
import com.gee12.mytetroid.data.crypt.TetroidCrypter
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.repo.CommonSettingsRepo
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
    var CurrentStorageId: Int? = null
    /**
     * Это простая реализация DI.
     * Да, это глобальный синглтон, что является плохим тоном.
     * Но при таком подходе:
     *  1) все зависимости расположены в 1 месте
     *  2) не нужно подключать дополнительную библиотеку DI
     */
    var current = TetroidEnvironment()

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
        logger: TetroidLogger,
        xmlLoader: TetroidXml,
        storagesRepo: StoragesRepo,
        storageInteractor: StorageInteractor,
        settingsRepo: CommonSettingsRepo,
        crypter: TetroidCrypter
    ) {
        if (IsInited) return

        current.apply {
            this.logger = logger
            this.xmlLoader = xmlLoader
            this.crypter = crypter
            this.storagesRepo = storagesRepo
            this.settingsRepo = settingsRepo
            this.storageInteractor = storageInteractor
        }

        logger.writeRawString("************************************************************")
        logger.log(context.getString(R.string.log_app_start_mask).format(Utils.getVersionName(logger, context)), false)
        if (CommonSettings.isCopiedFromFree()) {
            logger.log(R.string.log_settings_copied_from_free, true)
        }
        TetroidXml.ROOT_NODE.name = context.getString(R.string.title_root_node)
        IsInited = true
    }

    @JvmStatic
    fun destruct() {
        current.apply {
            logger = null
            xmlLoader = null
            crypter = null
            storagesRepo = null
            settingsRepo = null
            storageInteractor = null
        }
        IsInited = false
    }
}
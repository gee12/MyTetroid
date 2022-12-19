package com.gee12.mytetroid

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.annotation.ColorInt
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.common.utils.ViewUtils
import com.gee12.mytetroid.di.modules.*
//import com.gee12.mytetroid.ui.main.MainActivity
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class AppKoin : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        MultiDex.install(this)

        startKoin {
            androidContext(this@AppKoin)
            modules(
                RepositoriesModule.repositoriesModule,
                UseCasesModule.useCasesModule,
                ViewModelsModule.viewModelsModule,
                ManagersModule.managersModule,
                StorageModule.storageModule,
            )
        }

    }
}

object App {

    // TODO: убрать
    @JvmField
    var IsHighlightAttach = false
    @JvmField
    var IsHighlightCryptedNodes = false
    @ColorInt
    @JvmField
    var HighlightAttachColor = 0
    lateinit var DateFormatString: String
    lateinit var RecordFieldsInList: RecordFieldsSelector

    fun isFullVersion() = BuildConfig.FLAVOR == "pro"

    fun isFreeVersion() = BuildConfig.FLAVOR == "free"

}
package com.gee12.mytetroid

import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.gee12.mytetroid.di.modules.*
import com.gee12.mytetroid.domain.AppThemeHelper
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.android.ext.android.inject


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

        setTheme()
    }

    private fun setTheme() {
        val settingsManager: CommonSettingsManager by inject()
        AppThemeHelper.setTheme(settingsManager.getTheme())
    }
}

object App {

    // признак того, что приложение было запущено как полагается
    // и нужные переменные были проинициализированы
    var isInitialized = false

    fun isFullVersion() = BuildConfig.FLAVOR == "pro"

    fun isFreeVersion() = BuildConfig.FLAVOR == "free"

}
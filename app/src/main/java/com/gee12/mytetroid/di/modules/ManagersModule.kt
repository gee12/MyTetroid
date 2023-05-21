package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.domain.FailureHandler
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.Notificator
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.manager.FileStorageManager
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.TetroidLogger
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object ManagersModule {
    val managersModule = module {

        single {
            BuildInfoProvider(
                context = androidContext()
            )
        }

        single<IResourcesProvider> {
            ResourcesProvider(
                context = androidContext()
            )
        }

        single<ILocaleProvider> {
            LocaleProvider(
                context = androidContext()
            )
        }

        single<INotificator> {
            Notificator()
        }

        single<ITetroidLogger> {
            TetroidLogger(
                failureHandler = get(),
                localeProvider = get(),
                resourcesProvider = get(),
                notificator = get(),
            )
        }

        single<IFailureHandler> {
            FailureHandler(
                resourcesProvider = get(),
            )
        }

        single {
            CommonSettingsManager(
                context = androidContext(),
                resourcesProvider = get(),
                logger = get(),
                buildInfoProvider = get()
            )
        }

        single<IDataNameProvider> {
            DataNameProvider()
        }

        single<IAppPathProvider> {
            AppPathProvider(
                context = androidContext(),
            )
        }

        single {
            FileStorageManager(
                context = androidContext(),
                folderPickerCallback = null,
            )
        }

    }
}
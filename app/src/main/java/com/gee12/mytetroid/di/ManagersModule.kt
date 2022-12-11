package com.gee12.mytetroid.di

import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.TetroidLogger
import com.gee12.mytetroid.providers.BuildInfoProvider
import com.gee12.mytetroid.providers.CommonSettingsProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object ManagersModule {
    val managersModule = module {

        single {
            BuildInfoProvider(
                context = androidApplication()
            )
        }

        single<IResourcesProvider> {
            ResourcesProvider(
                context = androidApplication()
            )
        }

        single<ILocaleProvider> {
            LocaleProvider(
                context = androidApplication()
            )
        }

        single<INotificator> {
            Notificator()
        }

        single<ITetroidLogger> {
            TetroidLogger(
                failureHandler = get(),
                localeHelper = get(),
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
            CommonSettingsProvider(
                context = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                buildInfoProvider = get()
            )
        }

    }
}
package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.domain.FailureHandler
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.INotificator
import com.gee12.mytetroid.domain.Notificator
import com.gee12.mytetroid.domain.provider.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.TetroidLogger
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
            CommonSettingsProvider(
                context = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                buildInfoProvider = get()
            )
        }

        single<IDataNameProvider> {
            DataNameProvider()
        }

    }
}
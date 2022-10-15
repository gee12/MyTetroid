package com.gee12.mytetroid.di

import com.gee12.mytetroid.helpers.*
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object ManagersModule {
    val managersModule = module {

        single {
            EnvironmentProvider()
        }

        single<IStorageProvider> {
            StorageProvider(
                environmentProvider = get()
            )
        }

        single<ISensitiveDataProvider> {
            SensitiveDataProvider()
        }

        single {
            CommonSettingsProvider(
                context = androidApplication(),
                appBuildHelper = get()
            )
        }

    }
}
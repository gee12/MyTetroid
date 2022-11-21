package com.gee12.mytetroid.di

import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.crypt.IEncryptHelper
import com.gee12.mytetroid.data.crypt.EncryptHelper
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.TetroidLogger
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object HelpersModule {
    val helpersModule = module {

        single {
            AppBuildHelper(
                context = androidApplication()
            )
        }

        single<IResourcesProvider> {
            ResourcesProvider(
                context = androidApplication()
            )
        }

        single<ILocaleHelper> {
            LocaleHelper(
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

        single<IStoragePathHelper> {
            StoragePathHelper(
                storageProvider = get()
            )
        }

        single<IRecordPathHelper> {
            RecordPathHelper(
                storagePathHelper = get()
            )
        }

        single<IStorageDataProcessor> {
            StorageDataXmlProcessor(
                logger = get(),
                encryptHelper = get(),
                favoritesInteractor = get(),
                parseRecordTagsUseCase = get(),
                loadNodeIconUseCase = get(),
            )
        }

        single {
            Crypter(
                get()
            )
        }

        single<IEncryptHelper> {
            EncryptHelper(
                logger = get(),
                crypter = get(),
                cryptRecordFilesUseCase = get(),
                parseRecordTagsUseCase = get(),
            )
        }

    }
}
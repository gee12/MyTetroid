package com.gee12.mytetroid.di

import com.gee12.mytetroid.data.crypt.ITetroidCrypter
import com.gee12.mytetroid.data.crypt.TetroidCrypter
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

        single {
            LocaleHelper(
                context = androidApplication()
            )
        }

        single<ITetroidLogger> {
            TetroidLogger(
                localeHelper = get(),
                getStringCallback = null,
                getStringArrayCallback = null,
                showMessageCallback = null,
                showSnackMoreInLogsCallback = null,
            )
        }

        single<IStoragePathHelper> {
            StoragePathHelper(
                storageProvider = get()
            )
        }

        single<IStorageLoadHelper> {
            StorageLoadHelper(
                storageCrypter = get(),
                loadNodeIconUseCase = get(),
                favoritesInteractor = get(),
            )
        }

        single<ITagsParseHelper> {
            TagsParseHelper(
                tagsInteractor = get(),
                storageDataProcessor = get(),
            )
        }

        single<IStorageHelper> {
            StorageHelper(
                context = androidApplication(),
                nodesInteractor = get(),
                storageDataProcessor = get(),
            )
        }

        single<IStorageDataProcessor> {
            StorageDataXmlProcessor(
                loadHelper = get(),
                tagsParseHelper = get(),
                iconLoader = get(),
            )
        }

        single<ITetroidCrypter> {
            TetroidCrypter(
                logger = get(),
                tagsParseHelper = get(),
                cryptRecordFilesUseCase = get(),
            )
        }

    }
}
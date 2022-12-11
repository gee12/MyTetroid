package com.gee12.mytetroid.di

import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.data.crypt.StorageCrypter
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.providers.IStorageSettingsProvider
import com.gee12.mytetroid.providers.StorageSettingsProvider
import org.koin.dsl.module

object StorageModule {
    val helpersModule = module {

        scope<DependencyScope> {

            scoped<IStorageProvider> {
                StorageProvider(
                    logger = get(),
                    crypter = get(),
                    dataProcessor = get(),
                )
            }

            scoped<IStorageSettingsProvider> {
                StorageSettingsProvider(
                    storageProvider = get(),
                )
            }

            scoped<ISensitiveDataProvider> {
                SensitiveDataProvider()
            }

            scoped<IStoragePathProvider> {
                StoragePathProvider(
                    storageProvider = get()
                )
            }

            scoped<IRecordPathProvider> {
                RecordPathProvider(
                    storagePathProvider = get()
                )
            }

            scoped<IStorageDataProcessor> {
                StorageDataXmlProcessor(
                    logger = get(),
                    encryptHelper = get(),
                    favoritesInteractor = get(),
                    parseRecordTagsUseCase = get(),
                    loadNodeIconUseCase = get(),
                )
            }

            scoped {
                Crypter(
                    get()
                )
            }

            scoped<IStorageCrypter> {
                StorageCrypter(
                    logger = get(),
                    crypter = get(),
                    cryptRecordFilesUseCase = get(),
                    parseRecordTagsUseCase = get(),
                )
            }

        }

    }
}
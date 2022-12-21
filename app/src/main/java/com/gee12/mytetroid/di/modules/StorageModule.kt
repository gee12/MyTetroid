package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.domain.IStorageCrypter
import com.gee12.mytetroid.domain.StorageCrypter
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.FavoritesManager
import com.gee12.mytetroid.domain.provider.*
import org.koin.dsl.module

object StorageModule {
    val storageModule = module {

        scope<ScopeSource> {

            scoped<IStorageProvider> {
                StorageProvider(
                    logger = get(),
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
                    storageCrypter = get(),
                    favoritesManager = get(),
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
                )
            }

            scoped {
                FavoritesManager(
                    favoritesRepo = get(),
                    storageProvider = get(),
                )
            }

        }

    }
}
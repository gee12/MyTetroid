package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.data.crypt.Crypter
import com.gee12.mytetroid.domain.manager.IStorageCryptManager
import com.gee12.mytetroid.domain.manager.StorageCryptManager
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.provider.*
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object StorageModule {
    val storageModule = module {

        scope<ScopeSource> {

            scoped<IStorageProvider> {
                StorageProvider(
                    context = androidContext(),
                    logger = get(),
                    appPathProvider = get(),
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
                    context = androidContext(),
                    storageProvider = get(),
                    appPathProvider = get(),
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
                    cryptManager = get(),
                    favoritesManager = get(),
                    parseRecordTagsUseCase = get(),
                    loadNodeIconUseCase = get(),
                )
            }

            scoped<IStorageCryptManager> {
                StorageCryptManager(
                    logger = get(),
                )
            }

            scoped {
                FavoritesManager(
                    favoritesRepo = get(),
                    storageProvider = get(),
                    swapFavoriteRecordsUseCase = get(),
                )
            }

        }

    }
}
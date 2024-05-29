package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.data.xml.StorageDataXmlProcessor
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.manager.*
import com.gee12.mytetroid.domain.provider.*
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

            scoped {
                ScriptsManager(
                    storageProvider = get(),
                    scriptsRepo = get(),
                    scriptsToObjectsRepo = get(),
                    getRecordByIdUseCase = get(),
                    getNodeByIdUseCase = get(),
                )
            }

            scoped {
                HistoryManager(
                    storageProvider = get(),
                    historyDbRepo = get(),
                )
            }

        }

    }
}
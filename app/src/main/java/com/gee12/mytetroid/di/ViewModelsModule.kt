package com.gee12.mytetroid.di

import com.gee12.mytetroid.viewmodels.*
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object ViewModelsModule {
    val viewModelsModule = module {

        viewModel {
            CommonSettingsViewModel(
                app = androidApplication(),
                logger = get(),
                appBuildHelper = get(),
                commonSettingsProvider = get(),
            )
        }

        viewModel {
            StorageSettingsViewModel(
                app = androidApplication(),
                logger = get(),
                appBuildHelper = get(),
                storageProvider = get(),
                storageInteractor = get(),
                favoritesInteractor = get(),
                sensitiveDataProvider = get(),
                passInteractor = get(),
                storageCrypter = get(),
                cryptInteractor = get(),
                recordsInteractor = get(),
                nodesInteractor = get(),
                tagsInteractor = get(),
                attachesInteractor = get(),
                storagesRepo = get(),
                storageDataProcessor = get(),
                storagePathHelper = get(),
                commonSettingsInteractor = get(),
                dataInteractor = get(),
                settingsInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                environmentProvider = get(),
                initAppUseCase = get(),
            )
        }

        viewModel {
            StorageViewModel(
                app = androidApplication(),
                logger = get(),
                appBuildHelper = get(),
                storageProvider = get(),
                storageInteractor = get(),
                favoritesInteractor = get(),
                sensitiveDataProvider = get(),
                passInteractor = get(),
                storageCrypter = get(),
                cryptInteractor = get(),
                recordsInteractor = get(),
                nodesInteractor = get(),
                tagsInteractor = get(),
                attachesInteractor = get(),
                storagesRepo = get(),
                storageDataProcessor = get(),
                storagePathHelper = get(),
                commonSettingsInteractor = get(),
                dataInteractor = get(),
                settingsInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                environmentProvider = get(),
                initAppUseCase = get(),
            )
        }

        viewModel {
            MainViewModel(
                app = androidApplication(),
                logger = get(),
                appBuildHelper = get(),
                storageProvider = get(),
                storageInteractor = get(),
                favoritesInteractor = get(),
                sensitiveDataProvider = get(),
                passInteractor = get(),
                storageCrypter = get(),
                cryptInteractor = get(),
                recordsInteractor = get(),
                nodesInteractor = get(),
                tagsInteractor = get(),
                attachesInteractor = get(),
                storagesRepo = get(),
                storageDataProcessor = get(),
                storagePathHelper = get(),
                commonSettingsInteractor = get(),
                dataInteractor = get(),
                settingsInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                environmentProvider = get(),
                initAppUseCase = get(),
                storagesInteractor = get(),
                migrationInteractor = get(),
                storageTreeInteractor = get(),
                commonSettingsProvider = get(),
            )
        }

        viewModel {
            RecordViewModel(
                app = androidApplication(),
                logger = get(),
                appBuildHelper = get(),
                storageProvider = get(),
                storageInteractor = get(),
                favoritesInteractor = get(),
                sensitiveDataProvider = get(),
                passInteractor = get(),
                storageCrypter = get(),
                cryptInteractor = get(),
                recordsInteractor = get(),
                nodesInteractor = get(),
                tagsInteractor = get(),
                attachesInteractor = get(),
                storagesRepo = get(),
                storageDataProcessor = get(),
                storagePathHelper = get(),
                commonSettingsInteractor = get(),
                dataInteractor = get(),
                settingsInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                environmentProvider = get(),
                initAppUseCase = get(),
                imagesInteractor = get(),
            )
        }

        viewModel {
            StoragesViewModel(
                app = androidApplication(),
                logger = get(),
                appBuildHelper = get(),
                storageProvider = get(),
                storagePathHelper = get(),
            )
        }

        viewModel {
            IconsViewModel(
                app = androidApplication(),
                logger = get(),
                environmentProvider = get(),
                storageInteractor = get(),
            )
        }

        viewModel {
            LogsViewModel(
                app = androidApplication(),
                logger = get(),
            )
        }

        viewModel {
            StorageInfoViewModel(
                app = androidApplication(),
                logger = get(),
                appBuildHelper = get(),
                storageProvider = get(),
                storageInteractor = get(),
                favoritesInteractor = get(),
                sensitiveDataProvider = get(),
                passInteractor = get(),
                storageCrypter = get(),
                cryptInteractor = get(),
                recordsInteractor = get(),
                nodesInteractor = get(),
                tagsInteractor = get(),
                attachesInteractor = get(),
                storagesRepo = get(),
                storageDataProcessor = get(),
                storagePathHelper = get(),
                commonSettingsInteractor = get(),
                dataInteractor = get(),
                settingsInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                environmentProvider = get(),
                initAppUseCase = get(),
            )
        }

    }
}
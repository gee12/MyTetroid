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
                notificator = get(),
                appBuildHelper = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
            )
        }

        viewModel {
            StorageSettingsViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
                appBuildHelper = get(),
                storageProvider = get(),
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
                storagePathHelper = get(),
                recordPathHelper = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                initAppUseCase = get(),
                initOrCreateStorageUseCase = get(),
                readStorageUseCase = get(),
                saveStorageUseCase = get(),
                checkStoragePasswordUseCase = get(),
                changePasswordUseCase = get(),
            )
        }

        viewModel {
            StorageViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
                appBuildHelper = get(),
                storageProvider = get(),
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
                storagePathHelper = get(),
                recordPathHelper = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                initAppUseCase = get(),
                initOrCreateStorageUseCase = get(),
                readStorageUseCase = get(),
                saveStorageUseCase = get(),
                checkStoragePasswordUseCase = get(),
                changePasswordUseCase = get(),
            )
        }

        viewModel {
            MainViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                appBuildHelper = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
                storageProvider = get(),
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
                recordPathHelper = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                migrationInteractor = get(),
                storageTreeInteractor = get(),
                initAppUseCase = get(),
                initOrCreateStorageUseCase = get(),
                readStorageUseCase = get(),
                createNodeUseCase = get(),
                saveStorageUseCase = get(),
                checkStoragePasswordUseCase = get(),
                changePasswordUseCase = get(),
                insertNodeUseCase = get(),
            )
        }

        viewModel {
            RecordViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                appBuildHelper = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
                storageProvider = get(),
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
                storagePathHelper = get(),
                recordPathHelper = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                imagesInteractor = get(),
                initAppUseCase = get(),
                initOrCreateStorageUseCase = get(),
                readStorageUseCase = get(),
                saveStorageUseCase = get(),
                checkStoragePasswordUseCase = get(),
                changePasswordUseCase = get(),
            )
        }

        viewModel {
            StoragesViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                appBuildHelper = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
                storageProvider = get(),
                storagePathHelper = get(),
                storagesInteractor = get(),
            )
        }

        viewModel {
            IconsViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
                iconsInteractor = get(),
            )
        }

        viewModel {
            LogsViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
            )
        }

        viewModel {
            StorageInfoViewModel(
                app = androidApplication(),
                logger = get(),
                notificator = get(),
                appBuildHelper = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
                storageProvider = get(),
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
                recordPathHelper = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                syncInteractor = get(),
                trashInteractor = get(),
                initAppUseCase = get(),
                initOrCreateStorageUseCase = get(),
                readStorageUseCase = get(),
                saveStorageUseCase = get(),
                checkStoragePasswordUseCase = get(),
                changePasswordUseCase = get(),
                getFolderSizeUseCase = get(),
                getFileModifiedDateUseCase = get(),
            )
        }

    }
}
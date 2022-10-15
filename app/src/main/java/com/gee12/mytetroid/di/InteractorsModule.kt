package com.gee12.mytetroid.di

import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.usecase.CryptRecordFilesUseCase
import com.gee12.mytetroid.usecase.InitAppUseCase
import com.gee12.mytetroid.usecase.LoadNodeIconUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object InteractorsModule {
    val interactorsModule = module {

        single {
            DataInteractor(
                logger = get()
            )
        }

        single {
            CommonSettingsInteractor(
                logger = get()
            )
        }

        single {
            InteractionInteractor(
                logger = get()
            )
        }

        single {
            PermissionInteractor(
                logger = get()
            )
        }

        single {
            StoragesInteractor(
                storagesRepo = get()
            )
        }

        single {
            StorageTreeInteractor(
                app = androidApplication(),
                logger = get(),
            )
        }

        single {
            TrashInteractor(
                logger = get(),
                storagesRepo = get(),
            )
        }

        single {
            SyncInteractor(
                logger = get()
            )
        }

        single {
            MigrationInteractor(
                logger = get(),
                appBuildHelper = get(),
                commonSettingsRepo = get(),
                storagesInteractor = get(),
                favoritesInteractor = get(),
            )
        }

        single {
            IconsInteractor(
                logger = get(),
                pathToIcons = get(),
            )
        }

        single {
            ImagesInteractor(
                logger = get(),
                dataInteractor = get(),
                recordsInteractor = get(),
            )
        }

        single {
            EncryptionInteractor(
                logger = get(),
                crypter = get(),
                storageDataProcessor = get(),
                loadNodeIconUseCase = get(),
                cryptRecordFilesUseCase = get(),
            )
        }

        single {
            AttachesInteractor(
                logger = get(),
                storageInteractor = get(),
                cryptInteractor = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                recordsInteractor = get(),
            )
        }

        single {
            FavoritesInteractor(
                logger = get(),
                favoritesRepo = get(),
                environmentProvider = get(),
            )
        }

        single {
            StorageInteractor(
                logger = get(),
                storagePathHelper = get(),
                storageDataProcessor = get(),
                dataInteractor = get(),
            )
        }

        single {
            NodesInteractor(
                logger = get(),
                storageInteractor = get(),
                cryptInteractor = get(),
                dataInteractor = get(),
                recordsInteractor = get(),
                favoritesInteractor = get(),
                storagePathHelper = get(),
                storageDataProcessor = get(),
                nodeIconLoader = get(),
                tagsParseHelper = get(),
            )
        }

        single {
            RecordsInteractor(
                logger = get(),
                appBuildHelper = get(),
                storageInteractor = get(),
                storageDataProcessor = get(),
                cryptInteractor = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                tagsParseHelper = get(),
                favoritesInteractor = get(),
            )
        }

        single {
            TagsInteractor(
                logger = get(),
                storageInteractor = get(),
                storageDataProcessor = get(),
            )
        }

        single {
            PasswordInteractor(
                logger = get(),
                databaseConfig = get(),
                cryptInteractor = get(),
                nodesInteractor = get(),
                sensitiveDataProvider = get(),
            )
        }

        single {
            InitAppUseCase(
                context = androidApplication(),
                logger = get(),
                commonSettingsProvider = get(),
            )
        }

        single {
            LoadNodeIconUseCase(
                nodesInteractor = get(),
            )
        }

        single {
            CryptRecordFilesUseCase(
                context = get(),
                logger = get(),
                nodesInteractor = get(),
                recordsInteractor = get(),
                cryptInteractor = get(),
            )
        }

    }
}
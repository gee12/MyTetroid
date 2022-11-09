package com.gee12.mytetroid.di

import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.usecase.*
import com.gee12.mytetroid.usecase.crypt.ChangePasswordUseCase
import com.gee12.mytetroid.usecase.crypt.CheckStoragePasswordUseCase
import com.gee12.mytetroid.usecase.crypt.CryptRecordFilesUseCase
import com.gee12.mytetroid.usecase.crypt.EncryptOrDecryptFileUseCase
import com.gee12.mytetroid.usecase.node.CreateNodeUseCase
import com.gee12.mytetroid.usecase.node.InsertNodeUseCase
import com.gee12.mytetroid.usecase.storage.InitOrCreateStorageUseCase
import com.gee12.mytetroid.usecase.storage.ReadStorageUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
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
                commonSettingsProvider = get(),
                storagesInteractor = get(),
                favoritesInteractor = get(),
            )
        }

        single {
            IconsInteractor(
                logger = get(),
                storagePathHelper = get(),
            )
        }

        single {
            ImagesInteractor(
                logger = get(),
                dataInteractor = get(),
                recordsInteractor = get(),
                recordPathHelper = get(),
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
                cryptInteractor = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                recordsInteractor = get(),
                recordPathHelper = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            FavoritesInteractor(
                logger = get(),
                favoritesRepo = get(),
                storageProvider = get(),
            )
        }

        single {
            StorageInteractor(
                logger = get(),
                resourcesProvider = get(),
                storagePathHelper = get(),
                storageDataProcessor = get(),
                createNodeUseCase = get(),
            )
        }

        single {
            NodesInteractor(
                logger = get(),
                resourcesProvider = get(),
                cryptInteractor = get(),
                dataInteractor = get(),
                recordsInteractor = get(),
                favoritesInteractor = get(),
                storagePathHelper = get(),
                recordPathHelper = get(),
                storageProvider = get(),
                deleteRecordTagsUseCase = get(),
                loadNodeIconUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            RecordsInteractor(
                logger = get(),
                appBuildHelper = get(),
                storagePathHelper = get(),
                recordPathHelper = get(),
                storageDataProcessor = get(),
                cryptInteractor = get(),
                dataInteractor = get(),
                interactionInteractor = get(),
                favoritesInteractor = get(),
                parseRecordTagsUseCase = get(),
                deleteRecordTagsUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            TagsInteractor(
                logger = get(),
                storageDataProcessor = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            PasswordInteractor(
                logger = get(),
                storageProvider = get(),
                cryptInteractor = get(),
                nodesInteractor = get(),
                sensitiveDataProvider = get(),
            )
        }

        single {
            GetFolderSizeUseCase(
                context = androidApplication(),
            )
        }

        single {
            GetFileModifiedDateUseCase()
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
                logger = get(),
                storagePathHelper = get(),
            )
        }

        single {
            CryptRecordFilesUseCase(
                logger = get(),
                resourcesProvider = get(),
                recordPathHelper = get(),
                encryptOrDecryptFileUseCase = get(),
            )
        }

        single {
            ParseRecordTagsUseCase(
                //storageDataProcessor = get(),
            )
        }

        single {
            DeleteRecordTagsUseCase(
                storageProvider = get(),
                tagsInteractor = get(),
            )
        }

        single {
            InitOrCreateStorageUseCase(
                logger = get(),
                resourcesProvider = get(),
                storageProvider = get(),
                storageInteractor = get(),
                favoritesInteractor = get(),
            )
        }

        single {
            ReadStorageUseCase(
                resourcesProvider = get(),
                storageProvider = get(),
                storageDataProcessor = get(),
                storagePathHelper = get(),
            )
        }

        single {
            SaveStorageUseCase(
                context = androidApplication(),
                logger = get(),
                storagePathHelper = get(),
                storageDataProcessor = get(),
                dataInteractor = get(),
                storageTreeInteractor = get(),
            )
        }

        single {
            CreateNodeUseCase(
                logger = get(),
                dataInteractor = get(),
                cryptInteractor = get(),
                storageProvider = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            InsertNodeUseCase(
                context = androidApplication(),
                logger = get(),
                dataInteractor = get(),
                loadNodeIconUseCase = get(),
                cryptInteractor = get(),
                recordsInteractor = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            CheckStoragePasswordUseCase(
                logger = get(),
                cryptInteractor = get(),
                passInteractor = get(),
                sensitiveDataProvider = get(),
            )
        }

        single {
            ChangePasswordUseCase(
                context = get(),
                logger = get(),
                cryptInteractor = get(),
                passInteractor = get(),
                storagesRepo = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            EncryptOrDecryptFileUseCase(
                context = get(),
                logger = get(),
                crypter = get(),
            )
        }

    }
}
package com.gee12.mytetroid.di

import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.usecase.*
import com.gee12.mytetroid.usecase.attach.CloneAttachesToRecordUseCase
import com.gee12.mytetroid.usecase.crypt.*
import com.gee12.mytetroid.usecase.node.CreateNodeUseCase
import com.gee12.mytetroid.usecase.node.InsertNodeUseCase
import com.gee12.mytetroid.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.usecase.node.icon.GetIconsFoldersUseCase
import com.gee12.mytetroid.usecase.node.icon.GetIconsFromFolderUseCase
import com.gee12.mytetroid.usecase.record.CloneRecordToNodeUseCase
import com.gee12.mytetroid.usecase.storage.*
import com.gee12.mytetroid.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object InteractorsModule {
    val interactorsModule = module {

        single {
            DataInteractor(
                resourcesProvider = get(),
                logger = get()
            )
        }

        single {
            InteractionInteractor(
                resourcesProvider = get(),
                logger = get(),
            )
        }

        single {
            PermissionInteractor(
                logger = get(),
                resourcesProvider = get(),
            )
        }

        single {
            StoragesInteractor(
                context = androidApplication(),
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
                resourcesProvider = get(),
                logger = get(),
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
            ImagesInteractor(
                resourcesProvider = get(),
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
                resourcesProvider = get(),
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
                resourcesProvider = get(),
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
                cloneAttachesToRecordUseCase = get(),
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
                resourcesProvider = get(),
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
            ParseRecordTagsUseCase()
        }

        single {
            DeleteRecordTagsUseCase(
                tagsInteractor = get(),
            )
        }

        single {
            InitOrCreateStorageUseCase(
                createStorageUseCase = get(),
                initStorageUseCase = get(),
            )
        }

        single {
            CreateStorageUseCase(
                resourcesProvider = get(),
                logger = get(),
                storageDataProcessor = get(),
                favoritesInteractor = get(),
                createNodeUseCase = get(),
            )
        }

        single {
            InitStorageUseCase(
                favoritesInteractor = get(),
            )
        }

        single {
            ReadStorageUseCase(
                resourcesProvider = get(),
            )
        }

        single {
            CheckStorageFilesExistingUseCase(
                resourcesProvider = get(),
            )
        }

        single {
            SaveStorageUseCase(
                resourcesProvider = get(),
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
                saveStorageUseCase = get(),
            )
        }

        single {
            InsertNodeUseCase(
                logger = get(),
                dataInteractor = get(),
                loadNodeIconUseCase = get(),
                cryptInteractor = get(),
                recordsInteractor = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            CheckStoragePasswordAndAskUseCase(
                logger = get(),
                cryptInteractor = get(),
                passInteractor = get(),
                sensitiveDataProvider = get(),
            )
        }

        single {
            ChangePasswordUseCase(
                cryptInteractor = get(),
                saveStorageUseCase = get(),
                decryptStorageUseCase = get(),
                initPasswordUseCase = get(),
                savePasswordCheckDataUseCase = get(),
            )
        }

        single {
            SetupPasswordUseCase(
                initPasswordUseCase = get(),
                savePasswordCheckDataUseCase = get(),
            )
        }

        single {
            InitPasswordUseCase(
                storagesRepo = get(),
                cryptInteractor = get(),
                sensitiveDataProvider = get(),
            )
        }

        single {
            SavePasswordCheckDataUseCase()
        }

        single {
            DecryptStorageUseCase(
                logger = get(),
                crypter = get(),
                storageDataProcessor = get(),
                loadNodeIconUseCase = get(),
            )
        }

        single {
            CheckStoragePasswordAndDecryptUseCase(
                logger = get(),
                sensitiveDataProvider = get(),
                commonSettingsProvider = get(),
                cryptInteractor = get(),
                passInteractor = get(),
            )
        }

        single {
            EncryptOrDecryptFileUseCase(
                crypter = get(),
            )
        }

    }
}
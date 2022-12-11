package com.gee12.mytetroid.di

import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.providers.DataNameProvider
import com.gee12.mytetroid.providers.IDataNameProvider
import com.gee12.mytetroid.usecase.*
import com.gee12.mytetroid.usecase.attach.*
import com.gee12.mytetroid.usecase.file.GetFileModifiedDateUseCase
import com.gee12.mytetroid.usecase.file.GetFolderSizeUseCase
import com.gee12.mytetroid.usecase.crypt.*
import com.gee12.mytetroid.usecase.node.CreateNodeUseCase
import com.gee12.mytetroid.usecase.node.InsertNodeUseCase
import com.gee12.mytetroid.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.usecase.file.MoveFileUseCase
import com.gee12.mytetroid.usecase.node.DeleteNodeUseCase
import com.gee12.mytetroid.usecase.node.icon.GetIconsFoldersUseCase
import com.gee12.mytetroid.usecase.node.icon.GetIconsFromFolderUseCase
import com.gee12.mytetroid.usecase.node.icon.LoadNodeIconUseCase
import com.gee12.mytetroid.usecase.node.icon.SetNodeIconUseCase
import com.gee12.mytetroid.usecase.record.*
import com.gee12.mytetroid.usecase.record.image.SaveImageFromBitmapUseCase
import com.gee12.mytetroid.usecase.record.image.SaveImageFromUriUseCase
import com.gee12.mytetroid.usecase.storage.*
import com.gee12.mytetroid.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.usecase.tag.ParseRecordTagsUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object InteractorsModule {
    val interactorsModule = module {

        single<IDataNameProvider> {
            DataNameProvider()
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
                buildInfoProvider = get(),
                commonSettingsProvider = get(),
                storagesInteractor = get(),
                favoritesInteractor = get(),
                initStorageFromDefaultSettingsUseCase = get(),
            )
        }

        single {
            FavoritesInteractor(
                favoritesRepo = get(),
                storageProvider = get(),
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
                crypter = get(),
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

//        single {
//            GenerateDateTimeFilePrefixUseCase()
//        }

        single {
            MoveFileUseCase(
                resourcesProvider = get(),
                logger = get(),
            )
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
                dataNameProvider = get(),
                storagePathProvider = get(),
                storageDataProcessor = get(),
                storageTreeInteractor = get(),
                moveFileUseCase = get(),
            )
        }

        single {
            InitStorageFromDefaultSettingsUseCase(
                context = androidApplication()
            )
        }

        //region Node

        single {
            CreateNodeUseCase(
                logger = get(),
                dataNameProvider = get(),
                storageProvider = get(),
                crypter = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            InsertNodeUseCase(
                logger = get(),
                dataNameProvider = get(),
                loadNodeIconUseCase = get(),
                crypter = get(),
                saveStorageUseCase = get(),
                cloneRecordToNodeUseCase = get(),
            )
        }

        single {
            LoadNodeIconUseCase(
                logger = get(),
                storagePathProvider = get(),
            )
        }

        single {
            SetNodeIconUseCase(
                logger = get(),
                crypter = get(),
                loadNodeIconUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            DeleteNodeUseCase(
                logger = get(),
                favoritesInteractor = get(),
                deleteRecordTagsUseCase = get(),
                checkRecordFolderUseCase = get(),
                moveOrDeleteRecordFolder = get(),
                saveStorageUseCase = get(),
            )
        }

        //endregion Node

        //region Node icon

        single {
            GetIconsFoldersUseCase()
        }

        single {
            GetIconsFromFolderUseCase()
        }

        //region Node icon

        //region Record

        factory {
            CutOrDeleteRecordUseCase(
                logger = get(),
                recordPathProvider = get(),
                favoritesInteractor = get(),
                checkRecordFolderUseCase = get(),
                deleteRecordTagsUseCase = get(),
                moveOrDeleteRecordFolderUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            CryptRecordFilesUseCase(
                logger = get(),
                resourcesProvider = get(),
                recordPathProvider = get(),
                encryptOrDecryptFileUseCase = get(),
            )
        }

        single {
            ParseRecordTagsUseCase(
                storageProvider = get(),
            )
        }

        single {
            DeleteRecordTagsUseCase(
                storageProvider = get(),
                tagsInteractor = get(),
            )
        }

        single {
            CloneRecordToNodeUseCase(
                resourcesProvider = get(),
                logger = get(),
                favoritesInteractor = get(),
                dataNameProvider = get(),
                recordPathProvider = get(),
                storagePathProvider = get(),
                crypter = get(),
                checkRecordFolderUseCase = get(),
                cloneAttachesToRecordUseCase = get(),
                parseRecordTagsUseCase = get(),
                cryptRecordFilesUseCase = get(),
                renameRecordAttachesUseCase = get(),
                moveFileUseCase = get(),
            )
        }

        single {
            CheckRecordFolderUseCase(
                resourcesProvider = get(),
                logger = get(),
            )
        }

        single {
            InsertRecordUseCase(
                resourcesProvider = get(),
                logger = get(),
                storagePathProvider = get(),
                recordPathProvider = get(),
                dataNameProvider = get(),
                crypter = get(),
                favoritesInteractor = get(),
                checkRecordFolderUseCase = get(),
                cloneAttachesToRecordUseCase = get(),
                renameRecordAttachesUseCase = get(),
                moveFileUseCase = get(),
                parseRecordTagsUseCase = get(),
                cryptRecordFilesUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            CreateRecordUseCase(
                logger = get(),
                dataNameProvider = get(),
                recordPathProvider = get(),
                crypter = get(),
                favoritesInteractor = get(),
                checkRecordFolderUseCase = get(),
                parseRecordTagsUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            GetRecordHtmlTextUseCase(
                resourcesProvider = get(),
                logger = get(),
                checkRecordFolderUseCase = get(),
            )
        }

        single {
            GetRecordParsedTextUseCase(
                getRecordHtmlTextDecryptedUseCase = get(),
            )
        }

        single {
            SaveRecordHtmlTextUseCase(
                recordPathProvider = get(),
                crypter = get(),
                checkRecordFolderUseCase = get(),
            )
        }

        single {
            MoveOrDeleteRecordFolderUseCase(
                logger = get(),
                moveFileUseCase = get(),
//                generateDateTimeFilePrefixUseCase = get(),
                dataNameProvider = get(),
            )
        }

        //endregion Record

        //region Attach

        single {
            CreateAttachToRecordUseCase(
                resourcesProvider = get(),
                logger = get(),
                dataNameProvider = get(),
                recordPathProvider = get(),
                crypter = get(),
                checkRecordFolderUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            SaveAttachUseCase(
                resourcesProvider = get(),
                logger = get(),
                recordPathProvider = get(),
                crypter = get(),
            )
        }

        single {
            CloneAttachesToRecordUseCase(
                dataNameProvider = get(),
                crypter = get(),
            )
        }

        single {
            EditAttachFieldsUseCase(
                resourcesProvider = get(),
                logger = get(),
                recordPathProvider = get(),
                crypter = get(),
                checkRecordFolderUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        single {
            RenameRecordAttachesUseCase(
                resourcesProvider = get(),
                logger = get(),
                recordPathProvider = get(),
            )
        }

        single {
            DeleteAttachUseCase(
                logger = get(),
                recordPathProvider = get(),
                checkRecordFolderUseCase = get(),
                saveStorageUseCase = get(),
            )
        }

        //endregion Attach

        //region Image

        single {
            SaveImageFromUriUseCase(
                context = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                dataNameProvider = get(),
                recordPathProvider = get(),
                checkRecordFolderUseCase = get(),
            )
        }

        single {
            SaveImageFromBitmapUseCase(
                resourcesProvider = get(),
                logger = get(),
                dataNameProvider = get(),
                recordPathProvider = get(),
                checkRecordFolderUseCase = get(),
            )
        }

        //endregion Image

        single {
            CheckStoragePasswordAndAskUseCase(
                logger = get(),
                crypter = get(),
                passInteractor = get(),
                sensitiveDataProvider = get(),
            )
        }

        single {
            ChangePasswordUseCase(
                storageProvider = get(),
                crypter = get(),
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
                crypter = get(),
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
                crypter = get(),
                passInteractor = get(),
            )
        }

        single {
            EncryptOrDecryptFileUseCase(
                crypter = get(),
            )
        }

        single {
            GlobalSearchUseCase(
                logger = get(),
                getNodeByIdUseCase = get(),
                getRecordParsedTextUseCase = get(),
            )
        }

    }
}
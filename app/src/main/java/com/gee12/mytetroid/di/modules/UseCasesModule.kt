package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.InteractionManager
import com.gee12.mytetroid.domain.manager.PasswordManager
import com.gee12.mytetroid.domain.manager.PermissionManager
import com.gee12.mytetroid.domain.usecase.*
import com.gee12.mytetroid.domain.usecase.attach.*
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.*
import com.gee12.mytetroid.domain.usecase.image.LoadDrawableFromFileUseCase
import com.gee12.mytetroid.domain.usecase.node.*
import com.gee12.mytetroid.domain.usecase.node.icon.*
import com.gee12.mytetroid.domain.usecase.record.*
import com.gee12.mytetroid.domain.usecase.image.SaveImageFromBitmapUseCase
import com.gee12.mytetroid.domain.usecase.image.SaveImageFromUriUseCase
import com.gee12.mytetroid.domain.usecase.image.SetImageDimensionsUseCase
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.domain.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.GetTagByNameUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.RenameTagInRecordsUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object UseCasesModule {
    val useCasesModule = module {

        single {
            InteractionManager(
                resourcesProvider = get(),
                logger = get(),
            )
        }

        single {
            PermissionManager(
                logger = get(),
                resourcesProvider = get(),
            )
        }

        single {
            StorageTreeObserver(
                app = androidApplication(),
                logger = get(),
            )
        }

        single {
            SyncInteractor(
                resourcesProvider = get(),
                logger = get(),
            )
        }

        single {
            SwapFavoriteRecordsUseCase(
                favoritesRepo = get(),
            )
        }

        single {
            ClearAllStoragesTrashFolderUseCase(
                appPathProvider = get(),
                clearFolderUseCase = get(),
            )
        }

        single {
            ClearFolderUseCase(
                context = androidContext(),
                resourcesProvider = get(),
                logger = get(),
            )
        }

        scope<ScopeSource> {

            //region Interactors

            scoped {
                MigrationInteractor(
                    logger = get(),
                    buildInfoProvider = get(),
                    settingsManager = get(),
                    storagesRepo = get(),
                    favoritesManager = get(),
                    initStorageFromDefaultSettingsUseCase = get(),
                )
            }

            scoped {
                PasswordManager(
                    storageProvider = get(),
                    cryptManager = get(),
                )
            }

            //endregion Interactors

            //region App

            scoped {
                InitAppUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    settingsManager = get(),
                    appPathProvider = get(),
                )
            }

            scoped {
                GlobalSearchUseCase(
                    logger = get(),
                    storageProvider = get(),
                    getNodeByIdUseCase = get(),
                    getRecordParsedTextUseCase = get(),
                )
            }

            scoped {
                SwapObjectsInListUseCase()
            }

            //endregion App

            //region File

            scoped {
                GetFolderSizeInStorageUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                )
            }

            scoped {
                GetFileModifiedDateInStorageUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                )
            }

            scoped {
                MoveFileOrFolderUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                )
            }

            scoped {
                CopyFileOrFolderUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                )
            }

            scoped {
                CopyFileWithCryptUseCase(
                    context = androidContext(),
                    logger = get(),
                    encryptOrDecryptFileIfNeedUseCase = get(),
                )
            }

            //endregion File

            //region Storage

            scoped {
                InitOrCreateStorageUseCase(
                    createStorageUseCase = get(),
                    initStorageUseCase = get(),
                )
            }

            scoped {
                CreateStorageUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    storageProvider = get(),
                    storageDataProcessor = get(),
                    favoritesManager = get(),
                    createNodeUseCase = get(),
                    getStorageTrashFolderUseCase = get(),
                )
            }

            scoped {
                GetStorageTrashFolderUseCase(
                    context = androidContext(),
                    appPathProvider = get(),
                )
            }

            scoped {
                InitStorageUseCase(
                    context = androidContext(),
                    favoritesManager = get(),
                )
            }

            scoped {
                ReadStorageUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    storageProvider = get(),
                )
            }

            scoped {
                CheckStorageFilesExistingUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                )
            }

            scoped {
                SaveStorageUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    dataNameProvider = get(),
                    storagePathProvider = get(),
                    storageProvider = get(),
                    storageTreeInteractor = get(),
                    moveFileUseCase = get(),
                    getStorageTrashFolderUseCase = get(),
                )
            }

            scoped {
                InitStorageFromDefaultSettingsUseCase(
                    context = androidContext()
                )
            }

            scoped {
                CheckStoragePasswordAndAskUseCase(
                    logger = get(),
                    cryptManager = get(),
                    passwordManager = get(),
                    sensitiveDataProvider = get(),
                )
            }

            scoped {
                ChangePasswordUseCase(
                    storageProvider = get(),
                    cryptManager = get(),
                    saveStorageUseCase = get(),
                    decryptStorageUseCase = get(),
                    initPasswordUseCase = get(),
                    savePasswordCheckDataUseCase = get(),
                )
            }

            scoped {
                SetupPasswordUseCase(
                    initPasswordUseCase = get(),
                    savePasswordCheckDataUseCase = get(),
                )
            }

            scoped {
                InitPasswordUseCase(
                    context = androidContext(),
                    logger = get(),
                    storageProvider = get(),
                    storagesRepo = get(),
                    cryptManager = get(),
                    sensitiveDataProvider = get(),
                )
            }

            scoped {
                SavePasswordCheckDataUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                )
            }

            scoped {
                DecryptStorageUseCase(
                    logger = get(),
                    cryptManager = get(),
                    storageDataProcessor = get(),
                    loadNodeIconUseCase = get(),
                )
            }

            scoped {
                CheckStoragePasswordAndDecryptUseCase(
                    logger = get(),
                    sensitiveDataProvider = get(),
                    settingsManager = get(),
                    cryptManager = get(),
                    passwordManager = get(),
                )
            }

            scoped {
                EncryptOrDecryptFileIfNeedUseCase(
                    context = androidContext(),
                    logger = get(),
                    cryptManager = get(),
                )
            }

            scoped {
                ClearStorageTrashFolderUseCase(
                    storagePathProvider = get(),
                    clearFolderUseCase = get(),
                )
            }

            scoped {
                DeleteStorageUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    appPathProvider = get(),
                    storagesRepo = get(),
                )
            }

            //endregion Storage

            //region Node

            scoped {
                GetNodeByIdUseCase(
                    storageProvider = get(),
                )
            }

            scoped {
                CreateNodeUseCase(
                    logger = get(),
                    dataNameProvider = get(),
                    storageProvider = get(),
                    cryptManager = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                InsertNodeUseCase(
                    logger = get(),
                    dataNameProvider = get(),
                    loadNodeIconUseCase = get(),
                    cryptManager = get(),
                    saveStorageUseCase = get(),
                    cloneRecordToNodeUseCase = get(),
                )
            }

            scoped {
                LoadNodeIconUseCase(
                    loadDrawableFromFileUseCase = get(),
                )
            }

            scoped {
                SetNodeIconUseCase(
                    logger = get(),
                    cryptManager = get(),
                    loadNodeIconUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                CutOrDeleteNodeUseCase(
                    context = androidContext(),
                    logger = get(),
                    storageProvider = get(),
                    recordPathProvider = get(),
                    favoritesManager = get(),
                    deleteRecordTagsUseCase = get(),
                    getRecordFolderUseCase = get(),
                    moveOrDeleteRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                EditNodeFieldsUseCase(
                    logger = get(),
                    cryptManager = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                GetNodesAndRecordsCountUseCase()
            }

            //endregion Node

            //region Node icon

            scoped {
                GetIconsFolderNamesUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                )
            }

            scoped {
                GetNodesIconsFromFolderUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                )
            }

            //endregion Node icon

            //region Record


            scoped {
                GetRecordByIdUseCase(
                    storageProvider = get(),
                    favoritesManager = get(),
                )
            }

            scoped {
                CutOrDeleteRecordUseCase(
                    context = androidContext(),
                    logger = get(),
                    recordPathProvider = get(),
                    favoritesManager = get(),
                    getRecordFolderUseCase = get(),
                    deleteRecordTagsUseCase = get(),
                    moveOrDeleteRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                CryptRecordFilesIfNeedUseCase(
                    context = androidContext(),
                    logger = get(),
                    storageProvider = get(),
                    resourcesProvider = get(),
                    recordPathProvider = get(),
                    encryptOrDecryptFileUseCase = get(),
                )
            }

            scoped {
                CloneRecordToNodeUseCase(
                    logger = get(),
                    favoritesManager = get(),
                    dataNameProvider = get(),
                    cryptManager = get(),
                    cloneAttachesToRecordUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    cryptRecordFilesIfNeedUseCase = get(),
                    moveOrCopyRecordFolderUseCase = get(),
                )
            }

            scoped {
                GetRecordFolderUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    storageProvider = get(),
                )
            }

            scoped {
                InsertRecordUseCase(
                    context = androidContext(),
                    logger = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),
                    cryptManager = get(),
                    favoritesManager = get(),
                    getRecordFolderUseCase = get(),
                    cloneAttachesToRecordUseCase = get(),
                    moveFileOrFolderUseCase = get(),
                    moveOrCopyRecordFolderUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    cryptRecordFilesIfNeedUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                CreateRecordUseCase(
                    context = androidContext(),
                    logger = get(),
                    dataNameProvider = get(),
                    cryptManager = get(),
                    favoritesManager = get(),
                    getRecordFolderUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                CreateTempRecordUseCase(
                    context = androidContext(),
                    logger = get(),
                    dataNameProvider = get(),
                    recordPathProvider = get(),
                    getRecordFolderUseCase = get(),
                    saveRecordHtmlTextUseCase = get(),
                )
            }

            scoped {
                GetRecordHtmlTextUseCase(
                    context = get(),
                    resourcesProvider = get(),
                    logger = get(),
                    storageProvider = get(),
                    recordPathProvider = get(),
                    cryptManager = get(),
                    getRecordFolderUseCase = get(),
                )
            }

            scoped {
                GetRecordParsedTextUseCase(
                    getRecordHtmlTextDecryptedUseCase = get(),
                )
            }

            scoped {
                SaveRecordHtmlTextUseCase(
                    context = androidContext(),
                    cryptManager = get(),
                    getRecordFolderUseCase = get(),
                )
            }

            scoped {
                MoveOrCopyRecordFolderUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    getRecordFolderUseCase = get(),
                    renameRecordAttachesUseCase = get(),
                    moveFileOrFolderUseCase = get(),
                    copyFileOrFolderUseCase = get(),
                )
            }

            scoped {
                MoveOrDeleteRecordFolderUseCase(
                    context = androidContext(),
                    logger = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                    moveFileUseCase = get(),
                    dataNameProvider = get(),
                )
            }

            scoped {
                EditRecordFieldsUseCase(
                    context = androidContext(),
                    logger = get(),
                    storageProvider = get(),
                    buildInfoProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    favoritesManager = get(),
                    cryptManager = get(),
                    moveFileUseCase = get(),
                    deleteRecordTagsUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    cryptRecordFilesIfNeedUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            //endregion Record

            //region Attach

            scoped {
                AttachFileToRecordUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    dataNameProvider = get(),
                    cryptManager = get(),
                    getRecordFolderUseCase = get(),
                    copyFileWithCryptUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                SaveAttachToFileUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    storageProvider = get(),
                    recordPathProvider = get(),
                    copyFileWithCryptUseCase = get(),
                )
            }

            scoped {
                CloneAttachesToRecordUseCase(
                    dataNameProvider = get(),
                    cryptManager = get(),
                )
            }

            scoped {
                EditAttachFieldsUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    cryptManager = get(),
                    getRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                RenameRecordAttachesUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    storageProvider = get(),
                    recordPathProvider = get(),
                )
            }

            scoped {
                DeleteAttachUseCase(
                    context = androidContext(),
                    logger = get(),
                    getRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                PrepareAttachForOpenUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    cryptManager = get(),
                    storageProvider = get(),
                    recordPathProvider = get(),
                    storageSettingsProvider = get(),
                    getRecordFolderUseCase = get(),
                )
            }

            //endregion Attach

            //region Tag

            scoped {
                ParseRecordTagsUseCase(
                    storageProvider = get(),
                )
            }

            scoped {
                DeleteRecordTagsUseCase(
                    storageProvider = get(),
                    getTagByNameUseCase = get(),
                )
            }

            scoped {
                RenameTagInRecordsUseCase(
                    storageProvider = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                GetTagByNameUseCase(
                    storageProvider = get(),
                )
            }

            //endregion Tag

            //region Image

            scoped {
                LoadDrawableFromFileUseCase(
                    context = androidContext(),
                    logger = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                )
            }

            scoped {
                SaveImageFromUriUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    saveImageFromBitmapUseCase = get(),
                )
            }

            scoped {
                SaveImageFromBitmapUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    dataNameProvider = get(),
                    getRecordFolderUseCase = get(),
                )
            }

            scoped {
                SetImageDimensionsUseCase()
            }

            //endregion Image
            
        }
    }
}
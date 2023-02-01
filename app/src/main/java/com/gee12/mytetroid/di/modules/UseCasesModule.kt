package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.interactor.*
import com.gee12.mytetroid.domain.manager.PasswordManager
import com.gee12.mytetroid.domain.usecase.*
import com.gee12.mytetroid.domain.usecase.attach.*
import com.gee12.mytetroid.domain.usecase.file.GetFileModifiedDateUseCase
import com.gee12.mytetroid.domain.usecase.file.GetFolderSizeUseCase
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.MoveFileUseCase
import com.gee12.mytetroid.domain.usecase.node.*
import com.gee12.mytetroid.domain.usecase.node.icon.GetIconsFoldersUseCase
import com.gee12.mytetroid.domain.usecase.node.icon.GetIconsFromFolderUseCase
import com.gee12.mytetroid.domain.usecase.node.icon.LoadNodeIconUseCase
import com.gee12.mytetroid.domain.usecase.node.icon.SetNodeIconUseCase
import com.gee12.mytetroid.domain.usecase.record.*
import com.gee12.mytetroid.domain.usecase.record.image.SaveImageFromBitmapUseCase
import com.gee12.mytetroid.domain.usecase.record.image.SaveImageFromUriUseCase
import com.gee12.mytetroid.domain.usecase.record.image.SetImageDimensionsUseCase
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.domain.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.GetTagByNameUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.RenameTagInRecordsUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

object UseCasesModule {
    val useCasesModule = module {

        single {
            InteractionInteractor(
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
            SwapFavoriteRecordsUseCase(
                favoritesRepo = get(),
            )
        }

        scope<ScopeSource> {

            //region Interactors

            scoped {
                MigrationInteractor(
                    logger = get(),
                    buildInfoProvider = get(),
                    settingsManager = get(),
                    storagesInteractor = get(),
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
                    context = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    settingsManager = get(),
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
                SwapTetroidObjectsUseCase(
                    saveStorageUseCase = get(),
                )
            }

            //endregion App

            //region File

            scoped {
                GetFolderSizeUseCase(
                    context = androidApplication(),
                )
            }

            scoped {
                GetFileModifiedDateUseCase()
            }

//        scoped {
//            GenerateDateTimeFilePrefixUseCase()
//        }

            scoped {
                MoveFileUseCase(
                    resourcesProvider = get(),
                    logger = get(),
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
                    resourcesProvider = get(),
                    logger = get(),
                    storageDataProcessor = get(),
                    favoritesManager = get(),
                    createNodeUseCase = get(),
                )
            }

            scoped {
                InitStorageUseCase(
                    favoritesManager = get(),
                )
            }

            scoped {
                ReadStorageUseCase(
                    resourcesProvider = get(),
                )
            }

            scoped {
                CheckStorageFilesExistingUseCase(
                    resourcesProvider = get(),
                )
            }

            scoped {
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

            scoped {
                InitStorageFromDefaultSettingsUseCase(
                    context = androidApplication()
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
                    storagesRepo = get(),
                    cryptManager = get(),
                    sensitiveDataProvider = get(),
                )
            }

            scoped {
                SavePasswordCheckDataUseCase()
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
                EncryptOrDecryptFileUseCase(
                    crypter = get(),
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
                    logger = get(),
                    storagePathProvider = get(),
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
                DeleteNodeUseCase(
                    logger = get(),
                    storageProvider = get(),
                    recordPathProvider = get(),
                    favoritesManager = get(),
                    deleteRecordTagsUseCase = get(),
                    checkRecordFolderUseCase = get(),
                    moveOrDeleteRecordFolder = get(),
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
                GetIconsFoldersUseCase(
                    storagePathProvider = get(),
                )
            }

            scoped {
                GetIconsFromFolderUseCase(
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
                    logger = get(),
                    recordPathProvider = get(),
                    favoritesManager = get(),
                    checkRecordFolderUseCase = get(),
                    deleteRecordTagsUseCase = get(),
                    moveOrDeleteRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                CryptRecordFilesUseCase(
                    logger = get(),
                    resourcesProvider = get(),
                    recordPathProvider = get(),
                    encryptOrDecryptFileUseCase = get(),
                )
            }

            scoped {
                CloneRecordToNodeUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    favoritesManager = get(),
                    dataNameProvider = get(),
                    recordPathProvider = get(),
                    storagePathProvider = get(),
                    cryptManager = get(),
                    checkRecordFolderUseCase = get(),
                    cloneAttachesToRecordUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    cryptRecordFilesUseCase = get(),
                    renameRecordAttachesUseCase = get(),
                    moveFileUseCase = get(),
                )
            }

            scoped {
                CheckRecordFolderUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                )
            }

            scoped {
                InsertRecordUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),
                    cryptManager = get(),
                    favoritesManager = get(),
                    checkRecordFolderUseCase = get(),
                    cloneAttachesToRecordUseCase = get(),
                    renameRecordAttachesUseCase = get(),
                    moveFileUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    cryptRecordFilesUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                CreateRecordUseCase(
                    logger = get(),
                    dataNameProvider = get(),
                    recordPathProvider = get(),
                    cryptManager = get(),
                    favoritesManager = get(),
                    checkRecordFolderUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                CreateTempRecordUseCase(
                    logger = get(),
                    dataNameProvider = get(),
                    recordPathProvider = get(),
                    checkRecordFolderUseCase = get(),
                    saveRecordHtmlTextUseCase = get(),
                )
            }

            scoped {
                GetRecordHtmlTextUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    recordPathProvider = get(),
                    cryptManager = get(),
                    checkRecordFolderUseCase = get(),
                )
            }

            scoped {
                GetRecordParsedTextUseCase(
                    getRecordHtmlTextDecryptedUseCase = get(),
                )
            }

            scoped {
                SaveRecordHtmlTextUseCase(
                    recordPathProvider = get(),
                    cryptManager = get(),
                    checkRecordFolderUseCase = get(),
                )
            }

            scoped {
                MoveOrDeleteRecordFolderUseCase(
                    logger = get(),
                    moveFileUseCase = get(),
//                generateDateTimeFilePrefixUseCase = get(),
                    dataNameProvider = get(),
                )
            }

            scoped {
                EditRecordFieldsUseCase(
                    logger = get(),
                    buildInfoProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    favoritesManager = get(),
                    cryptManager = get(),
                    moveFileUseCase = get(),
                    deleteRecordTagsUseCase = get(),
                    parseRecordTagsUseCase = get(),
                    cryptRecordFilesUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            //endregion Record

            //region Attach

            scoped {
                CreateAttachToRecordUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    dataNameProvider = get(),
                    recordPathProvider = get(),
                    cryptManager = get(),
                    checkRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                SaveAttachUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    recordPathProvider = get(),
                    cryptManager = get(),
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
                    resourcesProvider = get(),
                    logger = get(),
                    recordPathProvider = get(),
                    cryptManager = get(),
                    checkRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                RenameRecordAttachesUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    recordPathProvider = get(),
                )
            }

            scoped {
                DeleteAttachUseCase(
                    logger = get(),
                    recordPathProvider = get(),
                    checkRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                GetFileFromAttachUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    cryptManager = get(),
                    recordPathProvider = get(),
                    storageSettingsProvider = get(),
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
                SaveImageFromUriUseCase(
                    context = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    dataNameProvider = get(),
                    recordPathProvider = get(),
                    checkRecordFolderUseCase = get(),
                )
            }

            scoped {
                SaveImageFromBitmapUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    dataNameProvider = get(),
                    recordPathProvider = get(),
                    checkRecordFolderUseCase = get(),
                )
            }

            scoped {
                SetImageDimensionsUseCase()
            }

            //endregion Image
            
        }
    }
}
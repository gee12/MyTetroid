package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.interactors.*
import com.gee12.mytetroid.usecase.*
import com.gee12.mytetroid.usecase.attach.*
import com.gee12.mytetroid.usecase.file.GetFileModifiedDateUseCase
import com.gee12.mytetroid.usecase.file.GetFolderSizeUseCase
import com.gee12.mytetroid.usecase.crypt.*
import com.gee12.mytetroid.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.usecase.file.MoveFileUseCase
import com.gee12.mytetroid.usecase.node.*
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

object UseCasesModule {
    val useCasesModule = module {

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

        scope<ScopeSource> {

            //region Interactors

            scoped {
                MigrationInteractor(
                    logger = get(),
                    buildInfoProvider = get(),
                    commonSettingsProvider = get(),
                    storagesInteractor = get(),
                    favoritesManager = get(),
                    initStorageFromDefaultSettingsUseCase = get(),
                )
            }

            scoped {
                TagsInteractor(
                    logger = get(),
                    storageDataProcessor = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                PasswordInteractor(
                    storageProvider = get(),
                    storageCrypter = get(),
                    sensitiveDataProvider = get(),
                )
            }

            //endregion Interactors

            //region App

            scoped {
                InitAppUseCase(
                    context = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    commonSettingsProvider = get(),
                )
            }

            scoped {
                GlobalSearchUseCase(
                    logger = get(),
                    getNodeByIdUseCase = get(),
                    getRecordParsedTextUseCase = get(),
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
                    storageCrypter = get(),
                    passInteractor = get(),
                    sensitiveDataProvider = get(),
                )
            }

            scoped {
                ChangePasswordUseCase(
                    storageProvider = get(),
                    storageCrypter = get(),
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
                    storageCrypter = get(),
                    sensitiveDataProvider = get(),
                )
            }

            scoped {
                SavePasswordCheckDataUseCase()
            }

            scoped {
                DecryptStorageUseCase(
                    logger = get(),
                    storageCrypter = get(),
                    storageDataProcessor = get(),
                    loadNodeIconUseCase = get(),
                )
            }

            scoped {
                CheckStoragePasswordAndDecryptUseCase(
                    logger = get(),
                    sensitiveDataProvider = get(),
                    commonSettingsProvider = get(),
                    storageCrypter = get(),
                    passInteractor = get(),
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
                GetNodeByIdUseCase()
            }

            scoped {
                CreateNodeUseCase(
                    logger = get(),
                    dataNameProvider = get(),
                    storageProvider = get(),
                    storageCrypter = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                InsertNodeUseCase(
                    logger = get(),
                    dataNameProvider = get(),
                    loadNodeIconUseCase = get(),
                    storageCrypter = get(),
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
                    storageCrypter = get(),
                    loadNodeIconUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                DeleteNodeUseCase(
                    logger = get(),
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
                    storageCrypter = get(),
                    saveStorageUseCase = get(),
                )
            }

            //endregion Node

            //region Node icon

            scoped {
                GetIconsFoldersUseCase()
            }

            scoped {
                GetIconsFromFolderUseCase()
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
                ParseRecordTagsUseCase(
                    storageProvider = get(),
                )
            }

            scoped {
                DeleteRecordTagsUseCase(
                    storageProvider = get(),
                    tagsInteractor = get(),
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
                    storageCrypter = get(),
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
                    storageCrypter = get(),
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
                    storageCrypter = get(),
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
                    storageCrypter = get(),
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
                    storageCrypter = get(),
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
                    storageCrypter = get(),
                    checkRecordFolderUseCase = get(),
                    saveStorageUseCase = get(),
                )
            }

            scoped {
                SaveAttachUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    recordPathProvider = get(),
                    storageCrypter = get(),
                )
            }

            scoped {
                CloneAttachesToRecordUseCase(
                    dataNameProvider = get(),
                    storageCrypter = get(),
                )
            }

            scoped {
                EditAttachFieldsUseCase(
                    resourcesProvider = get(),
                    logger = get(),
                    recordPathProvider = get(),
                    storageCrypter = get(),
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
                    storageCrypter = get(),
                    recordPathProvider = get(),
                    storageSettingsProvider = get(),
                )
            }

            //endregion Attach

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

            //endregion Image
            
        }
    }
}
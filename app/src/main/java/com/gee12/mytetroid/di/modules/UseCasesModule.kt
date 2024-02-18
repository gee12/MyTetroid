package com.gee12.mytetroid.di.modules

import android.print.PrintDocumentToFileUseCase
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.usecase.*
import com.gee12.mytetroid.domain.usecase.attach.*
import com.gee12.mytetroid.domain.usecase.crypt.*
import com.gee12.mytetroid.domain.usecase.crypt.DecryptStorageUseCase
import com.gee12.mytetroid.domain.usecase.file.*
import com.gee12.mytetroid.domain.usecase.html.CreateTagsHtmlStringUseCase
import com.gee12.mytetroid.domain.usecase.html.HtmlElementToTextUseCase
import com.gee12.mytetroid.domain.usecase.image.*
import com.gee12.mytetroid.domain.usecase.node.*
import com.gee12.mytetroid.domain.usecase.node.icon.*
import com.gee12.mytetroid.domain.usecase.record.*
import com.gee12.mytetroid.domain.usecase.network.DownloadFileFromWebUseCase
import com.gee12.mytetroid.domain.usecase.network.DownloadImageFromWebUseCase
import com.gee12.mytetroid.domain.usecase.network.DownloadWebPageContentUseCase
import com.gee12.mytetroid.domain.usecase.script.*
import com.gee12.mytetroid.domain.usecase.storage.*
import com.gee12.mytetroid.domain.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.GetTagByNameUseCase
import com.gee12.mytetroid.domain.usecase.tag.ParseRecordTagsUseCase
import com.gee12.mytetroid.domain.usecase.tag.RenameTagInRecordsUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object UseCasesModule {
    val useCasesModule = module {

        factory {
            InitAppUseCase(
                context = androidContext(),
                resourcesProvider = get(),
                logger = get(),
                settingsManager = get(),
                appPathProvider = get(),
            )
        }

        factory {
            SwapFavoriteRecordsUseCase(
                favoritesRepo = get(),
            )
        }

        factory {
            FillStorageFieldsFromDefaultSettingsUseCase(
                context = androidContext()
            )
        }

        factory {
            ClearAllStoragesTrashFolderUseCase(
                appPathProvider = get(),
                clearFolderUseCase = get(),
            )
        }

        factory {
            GetFolderUseCase(
                context = androidContext(),
            )
        }

        factory {
            ClearFolderUseCase(
                context = androidContext(),
                resourcesProvider = get(),
                logger = get(),
            )
        }

        factory {
            ReadTextBlocksFromFileUseCase()
        }

        factory {
            ReadTextBlocksFromStringUseCase()
        }

        factory {
            ReadTextFileUseCase(
                context = androidContext(),
            )
        }

        scope<ScopeSource> {

            //region App

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

            scoped {
                GetObjectByTypeAndIdUseCase(
                    getRecordByIdUseCase = get(),
                    getNodeByIdUseCase = get(),
                )
            }

            //endregion App

            //region File

            scoped {
                GetContentUriFromFileUseCase(
                    context = get(),
                    appBuildInfoProvider = get(),
                )
            }

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
                    appPathProvider = get(),
                    getFolderUseCase = get(),
                )
            }

            scoped {
                InitStorageUseCase(
                    context = androidContext(),
                    favoritesManager = get(),
                )
            }

            scoped {
                ReadStorageTreeUseCase(
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
                SaveStorageTreeUseCase(
                    context = androidContext(),
                    resourcesProvider = get(),
                    logger = get(),
                    dataNameProvider = get(),
                    storagePathProvider = get(),
                    storageProvider = get(),
                    storageTreeObserver = get(),
                    moveFileUseCase = get(),
                    getStorageTrashFolderUseCase = get(),
                )
            }

            scoped {
                CheckPasswordOrPinAndAskUseCase(
                    logger = get(),
                    cryptManager = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                )
            }

            scoped {
                ChangePasswordUseCase(
                    storageProvider = get(),
                    cryptManager = get(),
                    saveStorageTreeUseCase = get(),
                    decryptStorageUseCase = get(),
                    initPasswordUseCase = get(),
                    savePasswordInConfigUseCase = get(),
                )
            }

            scoped {
                SetupPasswordUseCase(
                    initPasswordUseCase = get(),
                    savePasswordInConfigUseCase = get(),
                )
            }

            scoped {
                InitPasswordUseCase(
                    storageProvider = get(),
                    cryptManager = get(),
                    sensitiveDataProvider = get(),
                    saveMiddlePasswordHashUseCase = get(),
                )
            }

            scoped {
                SavePasswordInConfigUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                    cryptManager = get(),
                )
            }

            scoped {
                SaveMiddlePasswordHashUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                    storagesRepo = get(),
                    cryptManager = get(),
                )
            }

            scoped {
                CheckPasswordUseCase(
                    logger = get(),
                    storageProvider = get(),
                    cryptManager = get(),
                )
            }

            scoped {
                ClearSavedPasswordHashUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                    storagesRepo = get(),
                )
            }

            scoped {
                DropAllPasswordDataUseCase(
                    context = androidContext(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagesRepo = get(),
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
                CheckPasswordOrPinAndDecryptUseCase(
                    logger = get(),
                    sensitiveDataProvider = get(),
                    settingsManager = get(),
                    cryptManager = get(),
                    storageProvider = get(),
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
                    saveStorageTreeUseCase = get(),
                )
            }

            scoped {
                InsertNodeUseCase(
                    logger = get(),
                    dataNameProvider = get(),
                    loadNodeIconUseCase = get(),
                    cryptManager = get(),
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
                )
            }

            scoped {
                EditNodeFieldsUseCase(
                    logger = get(),
                    cryptManager = get(),
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
                )
            }

            scoped {
                CreateTempRecordUseCase(
                    context = androidContext(),
                    logger = get(),
                    dataNameProvider = get(),
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
                PrintDocumentToFileUseCase(
                    context = androidContext(),
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
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
                )
            }

            scoped {
                PrepareAttachForOpenUseCase(
                    context = androidContext(),
                    appBuildInfoProvider = get(),
                    resourcesProvider = get(),
                    logger = get(),
                    storageProvider = get(),
                    recordPathProvider = get(),
                    storageSettingsProvider = get(),
                    getRecordFolderUseCase = get(),
                    encryptOrDecryptFileIfNeedUseCase = get(),
                    getContentUriFromFileUseCase = get(),
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
                    saveStorageTreeUseCase = get(),
                )
            }

            scoped {
                GetTagByNameUseCase(
                    storageProvider = get(),
                )
            }

            //endregion Tag

            // region Script

            scoped {
                GetScriptsUseCase(
                    context = androidContext(),
                    storagePathProvider = get(),
                    storageProvider = get(),
                    scriptsManager = get(),
                )
            }

            scoped {
                SaveScriptTextToFileUseCase(
                    context = androidContext(),
                    storagePathProvider = get(),
                    getFolderUseCase = get(),
                )
            }

            scoped {
                SaveScriptUseCase(
                    storageProvider = get(),
                    scriptsManager = get(),
                    saveScriptTextToFileUseCase = get(),
                )
            }

            scoped {
                EditScriptUseCase(
                    context = androidContext(),
                    storagePathProvider = get(),
                    storageProvider = get(),
                    scriptsManager = get(),
                    saveScriptTextToFileUseCase = get(),
                )
            }

            scoped {
                GetScriptTextUseCase(
                    context = androidContext(),
                    storagePathProvider = get(),
                    storageProvider = get(),
                    readTextFileUseCase = get(),
                )
            }

            scoped {
                DeleteScriptFileUseCase(
                    context = androidContext(),
                    storagePathProvider = get(),
                    storageProvider = get(),
                    scriptsManager = get(),
                )
            }

            scoped {
                SetScriptIsEnabledUseCase(
                    scriptsManager = get(),
                )
            }

            scoped {
                SetScriptToObjectIsEnabledUseCase(
                    scriptsManager = get(),
                )
            }

            // endregion Script

            //region Image

            scoped {
                PrepareFileForOpenUseCase(
                    getContentUriFromFileUseCase = get(),
                )
            }

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
                GetImageDimensionsUseCase(
                    context = androidContext(),
                )
            }

            //endregion Image

            // region Html

            scoped {
                CreateTagsHtmlStringUseCase()
            }

            scoped {
                HtmlElementToTextUseCase()
            }

            // endregion Html

            // region Network

            scoped {
                DownloadWebPageContentUseCase(
                    htmlElementToTextUseCase = get(),
                )
            }

            scoped {
                DownloadImageFromWebUseCase()
            }

            scoped {
                DownloadFileFromWebUseCase(
                    appPathProvider = get(),
                    dataNameProvider = get(),
                )
            }

            // endregion Network

        }
    }
}
package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.ui.logs.LogsViewModel
import com.gee12.mytetroid.ui.main.MainViewModel
import com.gee12.mytetroid.ui.node.icon.IconsViewModel
import com.gee12.mytetroid.ui.record.RecordViewModel
import com.gee12.mytetroid.ui.settings.CommonSettingsViewModel
import com.gee12.mytetroid.ui.settings.storage.StorageSettingsViewModel
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.ui.storage.info.StorageInfoViewModel
import com.gee12.mytetroid.ui.storages.StoragesViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object ViewModelsModule {
    val viewModelsModule = module {

        scope<ScopeSource> {

            viewModel {
                StorageViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),

                    settingsManager = get(),
                    appPathProvider = get(),
                    buildInfoProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    cryptManager = get(),

                    passwordManager = get(),
                    favoritesManager = get(),
                    interactionManager = get(),
                    syncInteractor = get(),

                    initAppUseCase = get(),
                    initOrCreateStorageUseCase = get(),
                    readStorageUseCase = get(),
                    saveStorageUseCase = get(),
                    checkStoragePasswordUseCase = get(),
                    changePasswordUseCase = get(),
                    decryptStorageUseCase = get(),
                    checkStoragePasswordAndDecryptUseCase = get(),
                    checkStorageFilesExistingUseCase = get(),
                    setupPasswordUseCase = get(),
                    initPasswordUseCase = get(),
                    clearStorageTrashFolderUseCase = get(),

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),
                )
            }

            viewModel {
                MainViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    buildInfoProvider = get(),
                    failureHandler = get(),

                    settingsManager = get(),
                    appPathProvider = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                    sensitiveDataProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    cryptManager = get(),
                    storageDataProcessor = get(),

                    favoritesManager = get(),
                    passwordManager = get(),
                    interactionManager = get(),
                    syncInteractor = get(),
                    migrationInteractor = get(),
                    storageTreeInteractor = get(),

                    initAppUseCase = get(),
                    initOrCreateStorageUseCase = get(),
                    readStorageUseCase = get(),
                    saveStorageUseCase = get(),
                    checkStoragePasswordUseCase = get(),
                    changePasswordUseCase = get(),
                    decryptStorageUseCase = get(),
                    checkStoragePasswordAndDecryptUseCase = get(),
                    checkStorageFilesExistingUseCase = get(),
                    setupPasswordUseCase = get(),
                    initPasswordUseCase = get(),
                    clearStorageTrashFolderUseCase = get(),

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),
                    swapTetroidObjectsUseCase = get(),

                    globalSearchUseCase = get(),
                    createNodeUseCase = get(),
                    insertNodeUseCase = get(),
                    cutOrDeleteNodeUseCase = get(),
                    editNodeFieldsUseCase = get(),
                    loadNodeIconUseCase = get(),
                    setNodeIconUseCase = get(),

                    insertRecordUseCase = get(),
                    createRecordUseCase = get(),
                    createTempRecordUseCase = get(),
                    editRecordFieldsUseCase = get(),
                    cutOrDeleteRecordUseCase = get(),
                    getRecordFolderUseCase = get(),

                    getUriFromAttachUseCase = get(),
                    createAttachToRecordUseCase = get(),
                    deleteAttachUseCase = get(),
                    editAttachFieldsUseCase = get(),
                    saveAttachToFileUseCase = get(),

                    cryptRecordFilesIfNeedUseCase = get(),

                    parseRecordTagsUseCase = get(),
                    getTagByNameUseCase = get(),
                    renameTagInRecordsUseCase = get(),
                )
            }

            viewModel {
                RecordViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),

                    settingsManager = get(),
                    appPathProvider = get(),
                    buildInfoProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    cryptManager = get(),

                    passwordManager = get(),
                    favoritesManager = get(),
                    interactionManager = get(),
                    syncInteractor = get(),

                    initAppUseCase = get(),
                    initOrCreateStorageUseCase = get(),
                    readStorageUseCase = get(),
                    saveStorageUseCase = get(),
                    checkStoragePasswordUseCase = get(),
                    changePasswordUseCase = get(),
                    decryptStorageUseCase = get(),
                    checkStoragePasswordAndDecryptUseCase = get(),
                    checkStorageFilesExistingUseCase = get(),
                    setupPasswordUseCase = get(),
                    initPasswordUseCase = get(),
                    clearStorageTrashFolderUseCase = get(),

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),

                    createTempRecordUseCase = get(),
                    getRecordHtmlTextDecryptedUseCase = get(),
                    saveRecordHtmlTextUseCase = get(),
                    attachFileToRecordUseCase = get(),
                    saveImageFromUriUseCase = get(),
                    saveImageFromBitmapUseCase = get(),
                    editRecordFieldsUseCase = get(),
                    getRecordFolderUseCase = get(),
                )
            }

            viewModel {
                StorageInfoViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),

                    settingsManager = get(),
                    appPathProvider = get(),
                    buildInfoProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    cryptManager = get(),
                    storageDataProcessor = get(),

                    passwordManager = get(),
                    favoritesManager = get(),
                    interactionManager = get(),
                    syncInteractor = get(),

                    initAppUseCase = get(),
                    initOrCreateStorageUseCase = get(),
                    readStorageUseCase = get(),
                    saveStorageUseCase = get(),
                    checkStoragePasswordUseCase = get(),
                    changePasswordUseCase = get(),
                    decryptStorageUseCase = get(),
                    checkStoragePasswordAndDecryptUseCase = get(),
                    checkStorageFilesExistingUseCase = get(),
                    setupPasswordUseCase = get(),
                    initPasswordUseCase = get(),
                    clearStorageTrashFolderUseCase = get(),

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),

                    cryptRecordFilesIfNeedUseCase = get(),
                    parseRecordTagsUseCase = get(),
                )
            }

            viewModel {
                StorageSettingsViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),

                    settingsManager = get(),
                    appPathProvider = get(),
                    buildInfoProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    cryptManager = get(),
                    storageDataProcessor = get(),

                    passwordManager = get(),
                    favoritesManager = get(),
                    interactionManager = get(),
                    syncInteractor = get(),

                    initAppUseCase = get(),
                    initOrCreateStorageUseCase = get(),
                    readStorageUseCase = get(),
                    saveStorageUseCase = get(),
                    checkStoragePasswordUseCase = get(),
                    changePasswordUseCase = get(),
                    decryptStorageUseCase = get(),
                    checkStoragePasswordAndDecryptUseCase = get(),
                    checkStorageFilesExistingUseCase = get(),
                    setupPasswordUseCase = get(),
                    initPasswordUseCase = get(),
                    clearStorageTrashFolderUseCase = get(),

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),
                )
            }

            viewModel {
                StoragesViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),

                    settingsManager = get(),
                    appPathProvider = get(),
                    buildInfoProvider = get(),

                    storagesRepo = get(),

                    checkStorageFilesExistingUseCase = get(),
                    initStorageFromDefaultSettingsUseCase = get(),
                    deleteStorageUseCase = get(),
                )
            }

            viewModel {
                IconsViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),
                    settingsManager = get(),
                    appPathProvider = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                    getIconsFolderNamesUseCase = get(),
                    getNodesIconsFromFolderUseCase = get(),
                )
            }

        }

        viewModel {
            CommonSettingsViewModel(
                app = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                notificator = get(),
                settingsManager = get(),
                appPathProvider = get(),
                buildInfoProvider = get(),
                failureHandler = get(),
                clearAllStoragesTrashFolderUseCase = get(),
            )
        }

        viewModel {
            LogsViewModel(
                app = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),
                settingsManager = get(),
                appPathProvider = get(),
            )
        }

    }
}
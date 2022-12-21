package com.gee12.mytetroid.di.modules

import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.ui.logs.LogsViewModel
import com.gee12.mytetroid.ui.main.MainViewModel
import com.gee12.mytetroid.ui.node.icon.IconsViewModel
import com.gee12.mytetroid.ui.settings.CommonSettingsViewModel
import com.gee12.mytetroid.ui.settings.storage.StorageSettingsViewModel
import com.gee12.mytetroid.ui.storage.StorageViewModel
import com.gee12.mytetroid.ui.storage.info.StorageInfoViewModel
import com.gee12.mytetroid.ui.storages.StoragesViewModel
import com.gee12.mytetroid.viewmodels.*
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

                    commonSettingsProvider = get(),
                    buildInfoProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    storageCrypter = get(),

                    passInteractor = get(),
                    favoritesManager = get(),
                    tagsInteractor = get(),
                    interactionInteractor = get(),
                    syncInteractor = get(),
                    trashInteractor = get(),

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

                    commonSettingsProvider = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                    sensitiveDataProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    storageCrypter = get(),
                    storageDataProcessor = get(),

                    favoritesManager = get(),
                    passInteractor = get(),
                    tagsInteractor = get(),
                    interactionInteractor = get(),
                    syncInteractor = get(),
                    trashInteractor = get(),
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

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),

                    globalSearchUseCase = get(),
                    createNodeUseCase = get(),
                    insertNodeUseCase = get(),
                    deleteNodeUseCase = get(),
                    editNodeFieldsUseCase = get(),
                    loadNodeIconUseCase = get(),
                    setNodeIconUseCase = get(),

                    insertRecordUseCase = get(),
                    createRecordUseCase = get(),
                    createTempRecordUseCase = get(),
                    editRecordFieldsUseCase = get(),
                    cutOrDeleteRecordUseCase = get(),

                    getFileFromAttachUseCase = get(),
                    createAttachToRecordUseCase = get(),
                    deleteAttachUseCase = get(),
                    editAttachFieldsUseCase = get(),
                    saveAttachUseCase = get(),

                    cryptRecordFilesUseCase = get(),
                    parseRecordTagsUseCase = get(),
                )
            }

            viewModel {
                RecordViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),

                    buildInfoProvider = get(),
                    commonSettingsProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    storageCrypter = get(),

                    passInteractor = get(),
                    tagsInteractor = get(),
                    favoritesManager = get(),
                    interactionInteractor = get(),
                    syncInteractor = get(),
                    trashInteractor = get(),

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

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),

                    createTempRecordUseCase = get(),
                    getRecordHtmlTextDecryptedUseCase = get(),
                    saveRecordHtmlTextUseCase = get(),
                    createAttachToRecordUseCase = get(),
                    saveImageFromUriUseCase = get(),
                    saveImageFromBitmapUseCase = get(),
                    editRecordFieldsUseCase = get(),
                )
            }

            viewModel {
                StorageInfoViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),

                    buildInfoProvider = get(),
                    commonSettingsProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    storageCrypter = get(),
                    storageDataProcessor = get(),

                    passInteractor = get(),
                    tagsInteractor = get(),
                    favoritesManager = get(),
                    interactionInteractor = get(),
                    syncInteractor = get(),
                    trashInteractor = get(),

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

                    getFileModifiedDateUseCase = get(),
                    getFolderSizeUseCase = get(),
                    getNodeByIdUseCase = get(),
                    getRecordByIdUseCase = get(),

                    cryptRecordFilesUseCase = get(),
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

                    buildInfoProvider = get(),
                    commonSettingsProvider = get(),
                    storageProvider = get(),
                    sensitiveDataProvider = get(),
                    storagePathProvider = get(),
                    recordPathProvider = get(),
                    dataNameProvider = get(),

                    storagesRepo = get(),
                    storageCrypter = get(),

                    passInteractor = get(),
                    tagsInteractor = get(),
                    favoritesManager = get(),
                    interactionInteractor = get(),
                    syncInteractor = get(),
                    trashInteractor = get(),

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

                    buildInfoProvider = get(),
                    commonSettingsProvider = get(),

                    storagesInteractor = get(),

                    checkStorageFilesExistingUseCase = get(),
                    initStorageFromDefaultSettingsUseCase = get(),
                )
            }

            viewModel {
                IconsViewModel(
                    app = androidApplication(),
                    resourcesProvider = get(),
                    logger = get(),
                    notificator = get(),
                    failureHandler = get(),
                    commonSettingsProvider = get(),
                    storageProvider = get(),
                    storagePathProvider = get(),
                    getIconsFoldersUseCase = get(),
                    getIconsFromFolderUseCase = get(),
                )
            }

        }

        viewModel {
            CommonSettingsViewModel(
                app = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                notificator = get(),
                buildInfoProvider = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
            )
        }

        viewModel {
            LogsViewModel(
                app = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),
                commonSettingsProvider = get(),
            )
        }

    }
}
package com.gee12.mytetroid.di

import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.helpers.StorageProvider
import com.gee12.mytetroid.viewmodels.*
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.core.scope.ScopeID
import org.koin.dsl.module

object ViewModelsModule {
    val viewModelsModule = module {

        factory { ScopeContainer() }

//        scope<DependencyScope> {
//            scoped(/*qualifier = named("scopedContainer")*/) {
//                StorageProvider(
//                    logger = get(),
//                    crypter = get(),
//                    dataProcessor = get(),
//                )
//            }
//        }

//        factory { (scopeId: ScopeID, name: String) ->
//            Environment(getScope(scopeId).get(qualifier = named(name)))
//        }

        viewModel { //(scopeId: ScopeID, name: String) ->
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
                storageCrypter = /*getScope(scopeId).*/get(),

                passInteractor = get(),
                favoritesInteractor = get(),
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

                favoritesInteractor = get(),
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
                favoritesInteractor = get(),
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

        viewModel { (storageProvider: IStorageProvider) ->
            StorageInfoViewModel(
                app = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),

                buildInfoProvider = get(),
                commonSettingsProvider = get(),
                storageProvider = storageProvider,
                sensitiveDataProvider = get(),
                storagePathProvider = get(),
                recordPathProvider = get(),
                dataNameProvider = get(),

                storagesRepo = get(),
                storageCrypter = get(),

                passInteractor = get(),
                tagsInteractor = get(),
                favoritesInteractor = get(),
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
                favoritesInteractor = get(),
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
            StoragesViewModel(
                app = androidApplication(),
                resourcesProvider = get(),
                logger = get(),
                notificator = get(),
                failureHandler = get(),

                buildInfoProvider = get(),
                commonSettingsProvider = get(),
                storageProvider = get(),

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
                storagePathProvider = get(),
                getIconsFoldersUseCase = get(),
                getIconsFromFolderUseCase = get(),
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
package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.helpers.*
import com.gee12.mytetroid.interactors.IconsInteractor
import com.gee12.mytetroid.logs.ITetroidLogger

class IconsViewModel(
    app: Application,
    resourcesProvider: IResourcesProvider,
    logger: ITetroidLogger,
    notificator: INotificator,
    val iconsInteractor: IconsInteractor,
    failureHandler: IFailureHandler,
    commonSettingsProvider: CommonSettingsProvider,
) : BaseViewModel(
    app,
    resourcesProvider,
    logger,
    notificator,
    failureHandler,
    commonSettingsProvider,
) {

    fun getIconsFolders() = iconsInteractor.getIconsFolders()

    fun getIconsFromFolder(folderName: String) = iconsInteractor.getIconsFromFolder(folderName)
}
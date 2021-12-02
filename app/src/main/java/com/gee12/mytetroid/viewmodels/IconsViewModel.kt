package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.interactors.IconsInteractor
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.repo.CommonSettingsRepo

class IconsViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
    settingsRepo: CommonSettingsRepo,
    val storageInteractor: StorageInteractor
) : BaseViewModel(app/*, logger*/, settingsRepo) {

    val iconsInteractor = IconsInteractor(logger, storageInteractor)

    fun getPathToIcons() = storageInteractor.getPathToIcons()

    fun getIconsFolders() = iconsInteractor.getIconsFolders()

    fun getIconsFromFolder(folderName: String) = iconsInteractor.getIconsFromFolder(folderName)
}
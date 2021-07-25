package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.interactors.IconsInteractor
import com.gee12.mytetroid.interactors.StorageInteractor

class IconsViewModel(
    app: Application,
    val storageInteractor: StorageInteractor
) : BaseViewModel(app) {

    protected val iconsInteractor = IconsInteractor(storageInteractor)

    fun getPathToIcons() = storageInteractor.getPathToIcons()

    fun getIconsFolders() = iconsInteractor.getIconsFolders()

    fun getIconsFromFolder(folderName: String) = iconsInteractor.getIconsFromFolder(folderName)
}
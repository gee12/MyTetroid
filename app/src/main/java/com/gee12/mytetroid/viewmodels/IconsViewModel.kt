package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.interactors.IconsInteractor
import com.gee12.mytetroid.interactors.StorageInteractor

class IconsViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
    val storageInteractor: StorageInteractor
) : BaseViewModel(app/*, logger*/) {

    val iconsInteractor = IconsInteractor(logger, storageInteractor)

    fun getPathToIcons() = storageInteractor.getPathToIcons()

    fun getIconsFolders() = iconsInteractor.getIconsFolders()

    fun getIconsFromFolder(folderName: String) = iconsInteractor.getIconsFromFolder(folderName)
}
package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.TetroidStorageData
import com.gee12.mytetroid.interactors.IconsInteractor

class IconsViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
    storageData: TetroidStorageData
) : BaseViewModel(app/*, logger*/) {

    val storageInteractor = storageData.storageInteractor

    val iconsInteractor = IconsInteractor(logger, storageInteractor)

    fun getPathToIcons() = storageInteractor.getPathToIcons()

    fun getIconsFolders() = iconsInteractor.getIconsFolders()

    fun getIconsFromFolder(folderName: String) = iconsInteractor.getIconsFromFolder(folderName)
}
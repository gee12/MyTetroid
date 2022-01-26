package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.TetroidStorageData
import com.gee12.mytetroid.interactors.IconsInteractor

class IconsViewModel(
    app: Application,
    storageData: TetroidStorageData
    /*logger: TetroidLogger?,*/
) : BaseViewModel(app/*, logger*/) {

    val iconsInteractor = IconsInteractor(logger, storageData.storageInteractor.getPathToIcons())

    fun getIconsFolders() = iconsInteractor.getIconsFolders()

    fun getIconsFromFolder(folderName: String) = iconsInteractor.getIconsFromFolder(folderName)
}
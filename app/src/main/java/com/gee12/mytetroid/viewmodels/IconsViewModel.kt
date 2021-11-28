package com.gee12.mytetroid.viewmodels

import android.app.Application
import com.gee12.mytetroid.interactors.IconsInteractor
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.logs.TetroidLogger

class IconsViewModel(
    app: Application,
    /*logger: TetroidLogger?,*/
    val storageInteractor: StorageInteractor
) : BaseViewModel(app/*, logger*/) {

    protected val iconsInteractor = IconsInteractor(storageInteractor)

    fun getPathToIcons() = storageInteractor.getPathToIcons()

    fun getIconsFolders() = iconsInteractor.getIconsFolders()

    fun getIconsFromFolder(folderName: String) = iconsInteractor.getIconsFromFolder(folderName)
}
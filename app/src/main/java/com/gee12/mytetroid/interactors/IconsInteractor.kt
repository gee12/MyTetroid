package com.gee12.mytetroid.interactors

import android.content.Context
import android.text.TextUtils
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.data.DataManager
import com.gee12.mytetroid.model.TetroidIcon
import java.io.File
import java.util.*

class IconsInteractor(
    val storageInteractor: StorageInteractor
) {

    /**
     * Получение списка каталогов с иконками в каталоге "icons/".
     * @param context
     * @return
     */
    fun getIconsFolders(): List<String>? {
        val folder = File(storageInteractor.getPathToIcons())
        if (!folder.exists()) {
            return null
        }
        val res: MutableList<String> = ArrayList()
        for (fileEntry in folder.listFiles()) {
            if (fileEntry.isDirectory) {
                res.add(fileEntry.name)
            }
        }
        return res
    }

    /**
     * Получение списка иконок (файлов .svg) в подкаталоге каталога "icons/".
     * @param folderName
     * @return
     */
    fun getIconsFromFolder(folderName: String): List<TetroidIcon>? {
        if (TextUtils.isEmpty(folderName)) {
            return null
        }
        val iconsFolderFullName = storageInteractor.getPathToIcons() + Constants.SEPAR.toString() + folderName
        val folder = File(iconsFolderFullName)
        if (!folder.exists()) {
            return null
        }
        val res: MutableList<TetroidIcon> = ArrayList()
        for (fileEntry in folder.listFiles()) {
            if (fileEntry.isFile) {
                val name = fileEntry.name
                if (!name.lowercase().endsWith(".svg")) continue
                val icon = TetroidIcon(folderName, name)
                res.add(icon)
            }
        }
        return res
    }

}
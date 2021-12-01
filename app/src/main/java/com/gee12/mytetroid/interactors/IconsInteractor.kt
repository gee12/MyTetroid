package com.gee12.mytetroid.interactors

import android.text.TextUtils
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.utils.FileUtils
import java.io.File
import java.util.*

/**
 * Создается для конкретного хранилища.
 */
class IconsInteractor(
    private val logger: ITetroidLogger,
    private val storageInteractor: StorageInteractor
) {

    /**
     * Получение списка каталогов с иконками в каталоге "icons/".
     * @return
     */
    fun getIconsFolders(): List<String>? {
        val folder = File(storageInteractor.getPathToIcons())
        if (!folder.exists()) {
            return null
        }
        val res: MutableList<String> = ArrayList()
        folder.listFiles()?.forEach { fileEntry ->
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
        folder.listFiles()?.forEach { fileEntry ->
            if (fileEntry.isFile) {
                val name = fileEntry.name
                if (!name.lowercase().endsWith(".svg")) return@forEach
                val icon = TetroidIcon(folderName, name)
                res.add(icon)
            }
        }
        return res
    }

    fun loadIconIfNull(icon: TetroidIcon) {
        if (icon.icon != null) return
        try {
            icon.icon  = FileUtils.loadSVGFromFile("${storageInteractor.getPathToIcons()}/${icon.folder}/${icon.name}")
        } catch (ex: Exception) {
            logger.logError(ex)
        }
    }
}
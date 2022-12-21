package com.gee12.mytetroid.domain.interactor

import com.gee12.mytetroid.domain.ClipboardManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.domain.repo.StoragesRepo
import com.gee12.mytetroid.common.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TrashInteractor(
    private val logger: ITetroidLogger,
    private val storagesRepo: StoragesRepo
) {

    enum class TrashClearResult {
        NONE,
        SUCCESS,
        FAILURE,
        NEED_ASK
    }

    suspend fun clearTrashFoldersIfNeeded(): TrashClearResult {
        return withContext(Dispatchers.IO) {

            // очищаем "буфер обмена", т.к. каталог(и) записи из корзины будут удалены
            // и нечего будет вставлять
            ClipboardManager.clear()

            try {
                storagesRepo.getStorages().forEach { storage ->
                    storage.trashPath?.let {
                        clearTrashFolder(it)
                    }
                }
                TrashClearResult.SUCCESS
            } catch (ex: Exception) {
                logger.logError(ex, false)
                TrashClearResult.FAILURE
            }
        }
    }

    suspend fun clearTrashFolderBeforeExit(storage: TetroidStorage, isNeedAsk: Boolean): TrashClearResult {
        return when {
            !storage.isClearTrashBeforeExit -> TrashClearResult.NONE
            storage.isAskBeforeClearTrashBeforeExit && isNeedAsk -> TrashClearResult.NEED_ASK
            clearTrashFolder(storage) -> TrashClearResult.SUCCESS
            else -> TrashClearResult.FAILURE
        }
    }

    suspend fun clearTrashFolder(storage: TetroidStorage): Boolean {
        if (storage.trashPath == null) return false

        // очищаем "буфер обмена", т.к. каталог(и) записи из корзины будут удалены
        // и нечего будет вставлять
        ClipboardManager.clear()

        return withContext(Dispatchers.IO) {
            try {
                return@withContext clearTrashFolder(storage.trashPath!!)
            } catch (ex: Exception) {
                logger.logError(ex, false)
                false
            }
        }
    }

    private fun clearTrashFolder(trashPath: String): Boolean {
        if (trashPath.isEmpty()) return false

        val trashDir = File(trashPath)
        return FileUtils.clearDir(trashDir)
    }

}
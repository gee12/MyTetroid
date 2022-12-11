package com.gee12.mytetroid.interactors

import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.repo.StoragesRepo

class StoragesInteractor(
    private val storagesRepo: StoragesRepo
) {

    suspend fun getStorages() = storagesRepo.getStorages()

    suspend fun getStoragesCount() = storagesRepo.getStoragesCount()

    suspend fun addStorage(storage: TetroidStorage) = storagesRepo.addStorage(storage)

    suspend fun deleteStorage(storage: TetroidStorage) = storagesRepo.deleteStorage(storage)

    suspend fun setIsDefault(storage: TetroidStorage) = storagesRepo.setIsDefault(storage)

}
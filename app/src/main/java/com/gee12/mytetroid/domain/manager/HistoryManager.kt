package com.gee12.mytetroid.domain.manager

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.database.map.history.toDbEntity
import com.gee12.mytetroid.database.map.history.toEntity
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.repo.HistoryDbRepo
import com.gee12.mytetroid.model.HistoryEntity
import com.gee12.mytetroid.model.TetroidFile
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.model.enums.TetroidObjectType
import java.util.Date

/**
 * Класс для работы с историей открытия объектов хранилища.
 */
class HistoryManager(
    private val storageProvider: IStorageProvider,
    private val historyDbRepo: HistoryDbRepo,
) {

    companion object {
        private const val HISTORY_MAX_SIZE = 100
    }

    private val storageId: Int
        get() = storageProvider.storage?.id ?: 0

    private val chain = mutableListOf<HistoryEntity>()
    private var currentIndex = -1

    private val lastIndex: Int
        get() = chain.size - 1

    suspend fun getAll(storageId: Int): Either<Failure, List<HistoryEntity>> {
        return historyDbRepo.getAll(storageId)
            .map { dbEntities ->
                dbEntities.map { it.toEntity() }
            }.map { items ->
                items.also {
                    chain.clear()
                    chain.addAll(items)
                    currentIndex = lastIndex
                }
            }
    }

    fun hasPrevious(): Boolean {
        return currentIndex >= 1
    }

    operator fun hasNext(): Boolean {
        return currentIndex < lastIndex
    }

    fun toPrevious(): HistoryEntity? {
        return if (hasPrevious()) {
            currentIndex--
            chain[currentIndex]
        } else {
            null
        }
    }

    fun toNext(): HistoryEntity? {
        return if (hasNext()) {
            currentIndex++
            chain[currentIndex]
        } else {
            null
        }
    }

    suspend fun addToHistory(record: TetroidRecord): Either<Failure, Unit> {
        return addItem(
            HistoryEntity(
                storageId = storageId,
                obj = record,
                type = TetroidObjectType.RECORD,
                createdDate = Date(),
            )
        )
    }

    suspend fun addToHistory(node: TetroidNode): Either<Failure, Unit> {
        return addItem(
            HistoryEntity(
                storageId = storageId,
                obj = node,
                type = TetroidObjectType.NODE,
                createdDate = Date(),
            )
        )
    }

    suspend fun addToHistory(attach: TetroidFile): Either<Failure, Unit> {
        return addItem(
            HistoryEntity(
                storageId = storageId,
                obj = attach,
                type = TetroidObjectType.ATTACH,
                createdDate = Date(),
            )
        )
    }

    suspend fun addToHistory(tag: TetroidTag): Either<Failure, Unit> {
        return addItem(
            HistoryEntity(
                storageId = storageId,
                obj = tag,
                type = TetroidObjectType.TAG,
                createdDate = Date(),
            )
        )
    }

    private suspend fun addItem(historyEntity: HistoryEntity): Either<Failure, Unit> {
        // удаляем звенья, вместо которых нужно записать новую историю
        // (когда курсор не в конце цепи)
        if (currentIndex < lastIndex) {
            for (i in lastIndex downTo currentIndex + 1) {
                val entity = chain[i]
                historyDbRepo.delete(entity = entity.toDbEntity())
                    .onFailure {
                        return it.toLeft()
                    }.onSuccess {
                        chain.removeAt(i)
                    }
            }
        }
        // удаляем оставшиеся звенья по этому объекту и добавляем новое звено сверху
        for (i in lastIndex downTo 0) {
            val entity = chain[i]
            if (entity.type == historyEntity.type && entity.obj.id == historyEntity.obj.id) {
                historyDbRepo.delete(entity = entity.toDbEntity())
                    .onFailure {
                        return it.toLeft()
                    }.onSuccess {
                        chain.removeAt(i)
                        currentIndex--
                    }
            }
        }
        return historyDbRepo.insert(entity = historyEntity.toDbEntity())
            .map {
                chain.add(historyEntity)
                currentIndex++
            }.flatMap {
                historyDbRepo.getCount(storageId = storageId)
                    .flatMap { count ->
                        if (count > HISTORY_MAX_SIZE) {
                            historyDbRepo.deleteOldestItem(storageId = storageId)
                                .map { Unit }
                        } else {
                            Unit.toRight()
                        }
                    }
            }
    }

    suspend fun delete(historyEntity: HistoryEntity): Either<Failure, Unit> {
        return historyDbRepo.delete(entity = historyEntity.toDbEntity())
            .map {
                chain.remove(historyEntity)
                //TODO? currentIndex--
            }
    }

    suspend fun deleteAll(storageId: Int): Either<Failure, Unit> {
        return historyDbRepo.deleteAll(storageId)
            .map {
                chain.clear()
                currentIndex = 0
            }
    }

}

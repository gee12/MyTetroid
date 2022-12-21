package com.gee12.mytetroid.domain.usecase.record

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.TetroidRecordComparator
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.FavoritesManager
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord

class GetRecordByIdUseCase(
    private val storageProvider: IStorageProvider,
    private val favoritesManager: FavoritesManager,
) : UseCase<TetroidRecord, GetRecordByIdUseCase.Params>() {

    data class Params(
        val recordId: String,
    )

    private val comparator = TetroidRecordComparator(TetroidRecord.FIELD_ID)

    suspend fun run(recordId: String): Either<Failure, TetroidRecord> {
        return run(Params(recordId))
    }

    override suspend fun run(params: Params): Either<Failure, TetroidRecord> {
        return findRecordInHierarchy(params.recordId)
    }

    private fun findRecordInHierarchy(recordId: String): Either<Failure, TetroidRecord> {
        findRecord(storageProvider.getRootNode().records, recordId)?.let { record ->
            return record.toRight()
        }
        val record = if (storageProvider.isLoadedFavoritesOnly()) {
            findRecord(favoritesManager.getFavoriteRecords(), recordId)
        } else {
            findRecordInHierarchy(storageProvider.getRootNodes(), recordId)
        }
        return record?.toRight() ?: Failure.Record.NotFound(recordId).toLeft()
    }

    private fun findRecord(records: List<TetroidRecord?>, recordId: String?): TetroidRecord? {
        for (record in records) {
            if (comparator.compare(recordId, record)) {
                return record
            }
        }
        return null
    }

    private fun findRecordInHierarchy(nodes: List<TetroidNode>, recordId: String): TetroidRecord? {
        for (node in nodes) {
            findRecord(node.records, recordId)?.let { record ->
                return record
            }
            if (node.isExpandable) {
                val found = findRecordInHierarchy(node.subNodes, recordId)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

}
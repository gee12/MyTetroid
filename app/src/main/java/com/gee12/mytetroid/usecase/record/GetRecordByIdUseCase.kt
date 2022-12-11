package com.gee12.mytetroid.usecase.record

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.TetroidRecordComparator
import com.gee12.mytetroid.data.xml.IStorageDataProcessor
import com.gee12.mytetroid.helpers.IStorageProvider
import com.gee12.mytetroid.interactors.FavoritesInteractor
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord

class GetRecordByIdUseCase(
    private val storageProvider: IStorageProvider,
    private val storageDataProcessor: IStorageDataProcessor,
    private val favoritesInteractor: FavoritesInteractor,
) : UseCase<TetroidRecord, GetRecordByIdUseCase.Params>() {

    data class Params(
        val recordId: String,
//        val rootNodes: List<TetroidNode>,
    )

    private val comparator = TetroidRecordComparator(TetroidRecord.FIELD_ID)

    suspend fun run(recordId: String): Either<Failure, TetroidRecord> {
        return run(Params(recordId))
    }

    override suspend fun run(params: Params): Either<Failure, TetroidRecord> {
        return findRecordInHierarchy(params.recordId)
    }

    private fun findRecordInHierarchy(recordId: String):Either<Failure, TetroidRecord> {
        findRecord(storageProvider.getRootNode().records, recordId)?.let { record ->
            return record.toRight()
        }
        return if (storageDataProcessor.isLoadFavoritesOnlyMode()) {
            findRecord(favoritesInteractor.getFavoriteRecords(), recordId)?.toRight()
                ?: Failure.Record.NotFound(recordId).toLeft()
        } else {
            findRecordInHierarchy(storageProvider.getRootNodes(), recordId)
        }
    }

    private fun findRecord(records: List<TetroidRecord?>, recordId: String?): TetroidRecord? {
        for (record in records) {
            if (comparator.compare(recordId, record)) {
                return record
            }
        }
        return null
    }

    private fun findRecordInHierarchy(nodes: List<TetroidNode>, recordId: String): Either<Failure, TetroidRecord> {
        for (node in nodes) {
            findRecord(node.records, recordId)?.let { record ->
                return record.toRight()
            }
            if (node.isExpandable) {
                findRecordInHierarchy(node.subNodes, recordId)
                    .onSuccess {
                        return it.toRight()
                    }
            }
        }
        return Failure.Record.NotFound(recordId).toLeft()
    }

}
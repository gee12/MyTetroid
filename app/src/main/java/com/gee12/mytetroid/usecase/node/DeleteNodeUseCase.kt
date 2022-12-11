package com.gee12.mytetroid.usecase.node

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.interactors.FavoritesInteractor
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.model.TetroidTag
import com.gee12.mytetroid.usecase.record.CheckRecordFolderUseCase
import com.gee12.mytetroid.usecase.record.MoveOrDeleteRecordFolderUseCase
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.usecase.tag.DeleteRecordTagsUseCase

/**
 * Удаление или вырезание ветки из родительской ветки.
 */
class DeleteNodeUseCase(
    private val logger: ITetroidLogger,
    private val favoritesInteractor: FavoritesInteractor,
    private val deleteRecordTagsUseCase: DeleteRecordTagsUseCase,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val moveOrDeleteRecordFolder: MoveOrDeleteRecordFolderUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, DeleteNodeUseCase.Params>() {

    data class Params(
        val node: TetroidNode,
        val movePath: String,
        val isCutting: Boolean,
        val rootNodes: List<TetroidNode>,
        val tagsMap: HashMap<String, TetroidTag>,
        val getRecordFolderCallback: (TetroidRecord) -> String,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val node = params.node
        val isCutting = params.isCutting

        logger.logOperStart(LogObj.NODE, if (isCutting) LogOper.CUT else LogOper.DELETE, node)

        // удаляем ветку из дерева
        val parentNodes = (if (node.parentNode != null) {
            node.parentNode.subNodes
        } else {
            params.rootNodes
        }) as MutableList<TetroidNode>
        if (!parentNodes.remove(node)) {
//            logger.logError(resourcesProvider.getString(R.string.error_node_not_found_with_id_mask) + node.id)
            return Failure.Node.NotFound(nodeId = node.id).toLeft()
        }

        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                // удаление всех объектов ветки рекурсивно
                deleteNodeRecursively(params, breakOnFsErrors = false)
            }.onFailure {
                logger.logOperCancel(LogObj.NODE, if (isCutting) LogOper.CUT else LogOper.DELETE)
            }.map { None }
    }

    private suspend fun deleteNodeRecursively(
        params: Params,
        breakOnFsErrors: Boolean,
    ): Either<Failure, None> {
        val node = params.node
        val movePath = params.movePath

        if (node.recordsCount > 0) {
            for (record in node.records) {
                val recordFolderPath = params.getRecordFolderCallback(record)

                // удаляем из избранного
                if (record.isFavorite) {
                    favoritesInteractor.remove(record, false)
                }
                deleteRecordTagsUseCase.run(
                    DeleteRecordTagsUseCase.Params(record/*, params.tagsMap*/)
                ).onFailure {
                    logger.logFailure(it, show = false)
                }
                // проверяем существование каталога
                checkRecordFolderUseCase.run(
                    CheckRecordFolderUseCase.Params(
                        folderPath = recordFolderPath,
                        isCreate = false,
                        showMessage = false
                    )
                ).flatMap {
                    // перемещаем каталог
                    moveOrDeleteRecordFolder.run(
                        MoveOrDeleteRecordFolderUseCase.Params(
                            record = record,
                            folderPath = recordFolderPath,
                            movePath = movePath,
                        )
                    )
                }.onFailure {
                    if (breakOnFsErrors) {
                        return it.toLeft()
                    }
                }
            }
        }

        for (subNode in node.subNodes) {
            deleteNodeRecursively(params, breakOnFsErrors)
                .onFailure {
                    if (breakOnFsErrors) {
                        return it.toLeft()
                    }
                }
        }

        return None.toRight()
    }

}
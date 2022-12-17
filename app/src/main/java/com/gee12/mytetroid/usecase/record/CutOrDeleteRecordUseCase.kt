package com.gee12.mytetroid.usecase.record

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.providers.IRecordPathProvider
import com.gee12.mytetroid.interactors.FavoritesManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase
import com.gee12.mytetroid.usecase.tag.DeleteRecordTagsUseCase

/**
 * Удаление/вырезание записи из ветки.
 * @param withoutDir Нужно ли пропустить работу с каталогом записи
 * @param movePath Путь к каталогу, куда следует переместить каталог записи (не обязательно)
 * @param isCutting Если true, то запись вырезается, иначе - удаляется
 */
class CutOrDeleteRecordUseCase(
    private val logger: ITetroidLogger,
    private val recordPathProvider: IRecordPathProvider,
    private val favoritesManager: FavoritesManager,
    private val checkRecordFolderUseCase: CheckRecordFolderUseCase,
    private val deleteRecordTagsUseCase: DeleteRecordTagsUseCase,
    private val moveOrDeleteRecordFolderUseCase: MoveOrDeleteRecordFolderUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, CutOrDeleteRecordUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val withoutDir: Boolean,
        val movePath: String,
        val isCutting: Boolean
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val withoutDir = params.withoutDir
        val movePath = params.movePath
        val isCutting = params.isCutting

        logger.logOperStart(LogObj.RECORD, if (isCutting) LogOper.CUT else LogOper.DELETE, record)
        var dirPath: String? = null
        // проверяем существование каталога записи
        if (!withoutDir) {
//            dirPath = (record.isTemp()) ? getPathToRecordFolderInTrash(context, record) : getPathToRecordFolderInBase(record);
            dirPath = recordPathProvider.getPathToRecordFolder(record)
            checkRecordFolderUseCase.run(
                CheckRecordFolderUseCase.Params(
                    folderPath = dirPath,
                    isCreate = false,
                )
            ).onFailure {
                return it.toLeft()
            }
        }

        // удаляем запись из ветки (и соответственно, из дерева)
        val node = record.node
        if (node != null) {
            if (!node.deleteRecord(record)) {
//                logger.logError(resourcesProvider.getString(R.string.error_record_not_exist_in_node_mask))
                return Failure.Record.NotFoundInNode(recordId = record.id).toLeft()
            }
        } else {
            // FIXME: в этом отпадет смысл, когда TetroidRecord будет на Kotlin
//            logger.logError(resourcesProvider.getString(R.string.log_record_not_have_node))
//            return 0
        }

        // перезаписываем структуру хранилища в файл
        saveStorageUseCase.run()
            .flatMap {
                // удаляем из избранного
                if (record.isFavorite) {
                    favoritesManager.remove(record, false)
                }
                // перезагружаем список меток
                deleteRecordTagsUseCase.run(
                    DeleteRecordTagsUseCase.Params(record)
                ).onFailure {
                    logger.logFailure(it, show = false)
                }
            }.onFailure {
                logger.logOperCancel(LogObj.RECORD, LogOper.DELETE)
                return it.toLeft()
            }

        return if (!withoutDir) {
            moveOrDeleteRecordFolderUseCase.run(
                MoveOrDeleteRecordFolderUseCase.Params(
                    record = record,
                    folderPath = dirPath!!,
                    movePath = movePath,
                )
            )
        } else {
            None.toRight()
        }
    }

}
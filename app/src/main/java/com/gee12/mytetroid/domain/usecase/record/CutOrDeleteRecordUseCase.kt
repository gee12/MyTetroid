package com.gee12.mytetroid.domain.usecase.record

import androidx.documentfile.provider.DocumentFile
import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.domain.manager.FavoritesManager
import com.gee12.mytetroid.domain.manager.ScriptsManager
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.domain.usecase.storage.SaveStorageTreeUseCase
import com.gee12.mytetroid.domain.usecase.tag.DeleteRecordTagsUseCase
import com.gee12.mytetroid.model.enums.TetroidObjectType

/**
 * Удаление/вырезание записи из ветки.
 * @param withoutDir Нужно ли пропустить работу с каталогом записи
 * @param movePath Путь к каталогу, куда следует переместить каталог записи (не обязательно)
 * @param isCutting Если true, то запись вырезается, иначе - удаляется
 */
class CutOrDeleteRecordUseCase(
    private val logger: ITetroidLogger,
    private val favoritesManager: FavoritesManager,
    private val scriptsManager: ScriptsManager,
    private val getRecordFolderUseCase: GetRecordFolderUseCase,
    private val deleteRecordTagsUseCase: DeleteRecordTagsUseCase,
    private val moveOrDeleteRecordFolderUseCase: MoveOrDeleteRecordFolderUseCase,
    private val saveStorageTreeUseCase: SaveStorageTreeUseCase,
) : UseCase<UseCase.None, CutOrDeleteRecordUseCase.Params>() {

    data class Params(
        val record: TetroidRecord,
        val withoutDir: Boolean,
        val isCutting: Boolean
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val record = params.record
        val withoutDir = params.withoutDir
        val isCutting = params.isCutting

        logger.logOperStart(LogObj.RECORD, if (isCutting) LogOper.CUT else LogOper.DELETE, record)
        var recordFolder: DocumentFile? = null
        // проверяем существование каталога записи
        if (!withoutDir) {
            recordFolder = getRecordFolderUseCase.run(
                GetRecordFolderUseCase.Params(
                    record = record,
                    createIfNeed = false,
                    inTrash = false,
                )
            ).foldResult(
                onLeft = {
                    return it.toLeft()
                },
                onRight = { it }
            )
        }

        // удаляем запись из ветки (и соответственно, из дерева)
        val node = record.node
        if (node != null) {
            if (!node.deleteRecord(record)) {
                return Failure.Record.NotFoundInNode(recordId = record.id).toLeft()
            }
        } else {
            // FIXME: в этом отпадет смысл, когда TetroidRecord будет на Kotlin
//            logger.logError(resourcesProvider.getString(R.string.log_record_not_have_node))
//            return 0
        }

        // перезаписываем структуру хранилища в файл
        return saveStorageTreeUseCase.run()
            .flatMap {
                // удаляем из избранного
                if (record.isFavorite) {
                    favoritesManager.remove(record, false)
                }
                // удаляем скрипты, активные только для этой записи
                scriptsManager.deleteScriptToObject(
                    objectId = record.id,
                    objectTypeId = TetroidObjectType.RECORD.id,
                )
                // перезагружаем список меток
                deleteRecordTagsUseCase.run(
                    DeleteRecordTagsUseCase.Params(record)
                ).onFailure {
                    logger.logFailure(it, show = false)
                }

                if (!withoutDir) {
                    moveOrDeleteRecordFolderUseCase.run(
                        MoveOrDeleteRecordFolderUseCase.Params(
                            record = record,
                            recordFolder = recordFolder!!,
                            isMoveToTrash = true,
                        )
                    )
                } else {
                    None.toRight()
                }
            }.onFailure {
                logger.logOperCancel(LogObj.RECORD, LogOper.DELETE)
            }
    }

}
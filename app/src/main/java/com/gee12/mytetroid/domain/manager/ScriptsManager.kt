package com.gee12.mytetroid.domain.manager

import com.gee12.mytetroid.common.map
import com.gee12.mytetroid.database.map.script.toDbEntity
import com.gee12.mytetroid.database.map.script.toEntity
import com.gee12.mytetroid.database.map.scriptToObject.toDbEntity
import com.gee12.mytetroid.database.map.scriptToObject.toEntity
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.repo.ScriptsDbRepo
import com.gee12.mytetroid.domain.repo.ScriptsToObjectsDbRepo
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.model.ITetroidObject
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.TetroidScriptToObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

class ScriptsManager(
    private val storageProvider: IStorageProvider,
    private val scriptsRepo: ScriptsDbRepo,
    private val scriptsToObjectsRepo: ScriptsToObjectsDbRepo,
    private val getRecordByIdUseCase: GetRecordByIdUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {

    private val storageId: Int
        get() = storageProvider.storage?.id ?: 0

    suspend fun getScripts(obj: ITetroidObject? = null): List<TetroidScript> {
        val objectScripts = scriptsRepo.getAll(
            storageId = storageId,
        )

        return objectScripts.map { scriptDbEntity ->
            val objectTypeId = obj?.type
            val objectId = obj?.id

            val objects = scriptsToObjectsRepo.getAllByScriptId(
                scriptId = scriptDbEntity.id,
            ).toMutableList()

            var isEnabled = false
            var objectName: String? = null
            if (objectTypeId != null && objectId != null) {
                when (objectTypeId) {
                    TetroidObjectType.RECORD.id -> {
                        getRecordByIdUseCase.run(
                            GetRecordByIdUseCase.Params(recordId = objectId)
                        ).map { record ->
                            objectName = record.name

                            // собираем список родительских веток
                            val parentNodes = buildList {
                                var parentNode: TetroidNode? = record.node
                                while (parentNode != null) {
                                    add(parentNode)
                                    parentNode = parentNode?.parentNode
                                }
                            }
                            val parentNodeIds = parentNodes.filterNotNull().map { it.id }

                            // удаляем записи по другим заметкам (лишние)
                            //  или не по родительским веткам
                            objects.removeAll {
                                it.objectTypeId == TetroidObjectType.RECORD.id
                                        && it.objectId != objectId
                                        || it.objectTypeId == TetroidObjectType.NODE.id
                                        && it.objectId !in parentNodeIds
                            }

                            // проверяем isEnabled для самой записи
                            isEnabled = objects.any {
                                it.objectId == record.id
                                        && it.objectTypeId == TetroidObjectType.RECORD.id
                            }
                            if (!isEnabled) {
                                // проверяем isEnabled для родительских веток
                                var parentNode: TetroidNode? = record.node
                                while (!isEnabled && parentNode != null) {
                                    isEnabled = objects.any {
                                        it.objectTypeId == TetroidObjectType.NODE.id
                                                && it.objectId == parentNode?.id
                                    }
                                    parentNode = parentNode?.parentNode
                                }
                            }
                            if (!isEnabled) {
                                // проверяем isEnabled скрипта для всего хранилища
                                isEnabled = objects.any {
                                    it.objectTypeId == null && it.objectId == null
                                }
                            }
                        }
                    }
                    TetroidObjectType.NODE.id -> {
                        getNodeByIdUseCase.run(
                            GetNodeByIdUseCase.Params(nodeId = objectId)
                        ).map { node ->
                            objectName = node.name

                            // собираем список родительских веток
                            val parentNodes = buildList {
                                var parentNode: TetroidNode? = node.parentNode
                                while (parentNode != null) {
                                    add(parentNode)
                                    parentNode = parentNode?.parentNode
                                }
                            }
                            val parentNodeIds = parentNodes.filterNotNull().map { it.id }

                            // удаляем записи по другим веткам (лишние)
                            //  или записи по заметкам
                            objects.removeAll {
                                it.objectTypeId == TetroidObjectType.NODE.id
                                        && it.objectId != objectId
                                        && it.objectId !in parentNodeIds
                                        || it.objectTypeId == TetroidObjectType.RECORD.id
                            }

                            // проверяем isEnabled для самой ветки
                            isEnabled = objects.any {
                                it.objectId == node.id
                                        && it.objectTypeId == TetroidObjectType.NODE.id
                            }
                            if (!isEnabled) {
                                // проверяем isEnabled для родительских веток
                                var parentNode: TetroidNode? = node.parentNode
                                while (!isEnabled && parentNode != null) {
                                    isEnabled = objects.any {
                                        it.objectTypeId == TetroidObjectType.NODE.id
                                                && it.objectId == parentNode?.id
                                    }
                                    parentNode = parentNode?.parentNode
                                }
                            }
                            if (!isEnabled) {
                                // проверяем isEnabled скрипта для всего хранилища
                                isEnabled = objects.any {
                                    it.objectTypeId == null && it.objectId == null
                                }
                            }
                        }
                    }
                    //TetroidObjectType.TAG.id -> TODO ?
                    else -> {
                        objectName = null
                        isEnabled = false
                    }
                }
            } else {
                objectName = null
                isEnabled = objects.any {
                    it.objectId == null && it.objectTypeId == null
                }
            }

            scriptDbEntity.toEntity(
                objects = objects.map {
                    it.toEntity(
                        objectName = if (objectId == it.objectId && objectTypeId == it.objectTypeId) {
                            objectName
                        } else {
                            getObjectName(
                                objectId = it.objectId,
                                objectTypeId = it.objectTypeId,
                            )
                        }
                    )
                },
                isEnabled = isEnabled,
            )
        }
    }

    private suspend fun getObjectName(objectId: String?, objectTypeId: Int?): String? {
        return when (objectTypeId) {
            TetroidObjectType.RECORD.id -> {
                objectId?.let {
                    getRecordByIdUseCase.run(
                        GetRecordByIdUseCase.Params(recordId = objectId)
                    ).foldResult(
                        onLeft = { null },
                        onRight = { it.name }
                    )
                }

            }
            TetroidObjectType.NODE.id -> {
                objectId?.let {
                    getNodeByIdUseCase.run(
                        GetNodeByIdUseCase.Params(nodeId = objectId)
                    ).foldResult(
                        onLeft = { null },
                        onRight = { it.name }
                    )
                }
            }
            //TetroidObjectType.TAG.id -> //TODO ?
            else -> {
                null
            }
        }
    }

    suspend fun insertScript(script: TetroidScript): Boolean {
        return scriptsRepo.insert(script.toDbEntity())
    }

    suspend fun insertScriptToObject(scriptToObject: TetroidScriptToObject): Boolean {
        return scriptsToObjectsRepo.insert(scriptToObject.toDbEntity())
    }

    suspend fun updateScriptIsEnabledForObject(scriptToObject: TetroidScriptToObject, isEnabled: Boolean): Boolean {
        scriptToObject.isEnabled = isEnabled
        return if (isEnabled) {
            if (scriptsToObjectsRepo.getAll(
                    scriptId = scriptToObject.scriptId,
                    objectTypeId = scriptToObject.objectType?.id,
                    objectId = scriptToObject.objectId,
                ).isEmpty()
            ) {
                scriptsToObjectsRepo.insert(scriptToObject.toDbEntity())
            } else {
                true
            }
        } else {
            scriptsToObjectsRepo.delete(
                scriptId = scriptToObject.scriptId,
                objectTypeId = scriptToObject.objectType?.id,
                objectId = scriptToObject.objectId,
            )
        }
    }

    suspend fun updateScriptFields(script: TetroidScript, fileName: String, description: String?): Boolean {
        script.fileName = fileName
        script.description = description
        return scriptsRepo.update(script.toDbEntity())
    }

    suspend fun deleteScript(script: TetroidScript): Boolean {
        val dbEntity = script.toDbEntity()
        return scriptsRepo.delete(dbEntity).also { isInserted ->
            if (isInserted) {
                scriptsToObjectsRepo.deleteByScriptId(scriptId = dbEntity.id)
            }
        }
    }

}
package com.gee12.mytetroid.domain.manager

import com.gee12.mytetroid.common.map
import com.gee12.mytetroid.database.entity.ScriptToObjectDbEntity
import com.gee12.mytetroid.database.map.script.toDbEntity
import com.gee12.mytetroid.database.map.script.toEntity
import com.gee12.mytetroid.database.map.scriptToObject.toDbEntity
import com.gee12.mytetroid.database.map.scriptToObject.toEntity
import com.gee12.mytetroid.domain.provider.IStorageProvider
import com.gee12.mytetroid.domain.repo.ScriptsDbRepo
import com.gee12.mytetroid.domain.repo.ScriptsToObjectsDbRepo
import com.gee12.mytetroid.domain.usecase.node.GetNodeByIdUseCase
import com.gee12.mytetroid.domain.usecase.record.GetRecordByIdUseCase
import com.gee12.mytetroid.model.*
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

    suspend fun getScripts(obj: TetroidObject? = null): List<TetroidScript> {
        val storageScripts = scriptsRepo.getAll(
            storageId = storageId,
        )

        return storageScripts.map { scriptDbEntity ->
            val objectTypeId = obj?.type
            val objectId = obj?.id

            val objects = getScriptObjects(
                scriptId = scriptDbEntity.id,
            ).toMutableList()

            val isActive = isScriptActive(
                objectTypeId = objectTypeId,
                objectId = objectId,
                objects = objects,
            )

            scriptDbEntity.toEntity(
                isActive = isActive,
            ).also { script ->
                script.objects = objects.map {
                    it.toEntity(
                        script = script,
                        obj = if (objectId == it.objectId && objectTypeId == it.objectTypeId) {
                            obj
                        } else {
                            getObject(
                                objectId = it.objectId,
                                objectTypeId = it.objectTypeId,
                            )
                        }
                    )
                }.filter { it.obj != null || !it.isObjectFilled() }
            }
        }
    }

    suspend fun getScriptObjects(scriptId: Int): List<ScriptToObjectDbEntity> {
        return scriptsToObjectsRepo.getAllByScriptId(
            scriptId = scriptId,
        )
    }

    private suspend fun isScriptActive(
        objectTypeId: Int?,
        objectId: String?,
        objects: MutableList<ScriptToObjectDbEntity>,
    ): Boolean {
        var isActive = false
        if (objectTypeId != null && objectId != null) {
            when (objectTypeId) {
                TetroidObjectType.RECORD.id -> {
                    getRecordByIdUseCase.run(
                        GetRecordByIdUseCase.Params(recordId = objectId)
                    ).map { record ->

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

                        // проверяем включен ли для самой записи
                        isActive = objects.any {
                            it.objectId == record.id
                                    && it.objectTypeId == TetroidObjectType.RECORD.id
                        }
                        if (!isActive) {
                            // проверяем включен ли для родительских веток
                            var parentNode: TetroidNode? = record.node
                            while (!isActive && parentNode != null) {
                                isActive = objects.any {
                                    it.objectTypeId == TetroidObjectType.NODE.id
                                            && it.objectId == parentNode?.id
                                }
                                parentNode = parentNode?.parentNode
                            }
                        }
                        if (!isActive) {
                            // проверяем включен ли для всего хранилища
                            isActive = objects.any {
                                it.objectTypeId == null && it.objectId == null
                            }
                        }
                    }
                }
                TetroidObjectType.NODE.id -> {
                    getNodeByIdUseCase.run(
                        GetNodeByIdUseCase.Params(nodeId = objectId)
                    ).map { node ->

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

                        // проверяем включен ли для самой ветки
                        isActive = objects.any {
                            it.objectId == node.id
                                    && it.objectTypeId == TetroidObjectType.NODE.id
                        }
                        if (!isActive) {
                            // проверяем включен ли для родительских веток
                            var parentNode: TetroidNode? = node.parentNode
                            while (!isActive && parentNode != null) {
                                isActive = objects.any {
                                    it.objectTypeId == TetroidObjectType.NODE.id
                                            && it.objectId == parentNode?.id
                                }
                                parentNode = parentNode?.parentNode
                            }
                        }
                        if (!isActive) {
                            // проверяем включен ли для всего хранилища
                            isActive = objects.any {
                                it.objectTypeId == null && it.objectId == null
                            }
                        }
                    }
                }
                //TetroidObjectType.TAG.id -> TODO ?
                else -> {
                    isActive = false
                }
            }
        } else {
            isActive = objects.any {
                it.objectId == null && it.objectTypeId == null
            }
        }
        return isActive
    }

    private suspend fun getObject(objectId: String?, objectTypeId: Int?): TetroidObject? {
        return when (objectTypeId) {
            TetroidObjectType.RECORD.id -> {
                objectId?.let {
                    getRecordByIdUseCase.run(
                        GetRecordByIdUseCase.Params(recordId = objectId)
                    ).foldResult(
                        onLeft = { null },
                        onRight = { it }
                    )
                }

            }
            TetroidObjectType.NODE.id -> {
                objectId?.let {
                    getNodeByIdUseCase.run(
                        GetNodeByIdUseCase.Params(nodeId = objectId)
                    ).foldResult(
                        onLeft = { null },
                        onRight = { it }
                    )
                }
            }
            //TetroidObjectType.TAG.id -> //TODO ?
            else -> {
                null
            }
        }
    }

    suspend fun isUniqueFileName(storageId: Int, scriptId: Int?, fileName: String): Boolean {
        return scriptsRepo.isUniqueFileName(storageId, scriptId, fileName)
    }

    suspend fun insertScript(script: TetroidScript): Boolean {
        val dbEntity = script.toDbEntity()
        return scriptsRepo.insert(dbEntity).also {
            script.id = dbEntity.id
        }
    }

    suspend fun insertScriptToObject(scriptToObject: TetroidScriptToObject): Boolean {
        val dbEntity = scriptToObject.toDbEntity()
        return scriptsToObjectsRepo.insert(dbEntity).also {
            scriptToObject.id = dbEntity.id
        }
    }

    suspend fun updateScriptIsActiveForObject(scriptToObject: TetroidScriptToObject, isActive: Boolean): Boolean {
        scriptToObject.isActive = isActive
        return if (isActive) {
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

    suspend fun deleteScriptByStorageId(storageId: Int): Boolean {
        val scriptsByStorageId = scriptsRepo.getAll(storageId)
        scriptsByStorageId.forEach { script ->
            scriptsToObjectsRepo.deleteByScriptId(scriptId = script.id)
        }
        return scriptsRepo.deleteByStorageId(storageId)
    }

    suspend fun deleteScriptToObject(objectId: String, objectTypeId: Int): Boolean {
        return scriptsToObjectsRepo.deleteByObject(
            objectTypeId = objectTypeId,
            objectId = objectId,
        )
    }

}
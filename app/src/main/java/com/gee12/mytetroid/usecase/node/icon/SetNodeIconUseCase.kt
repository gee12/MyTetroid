package com.gee12.mytetroid.usecase.node.icon

import com.gee12.mytetroid.common.*
import com.gee12.mytetroid.data.crypt.IStorageCrypter
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.logs.LogOper
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.usecase.storage.SaveStorageUseCase

class SetNodeIconUseCase(
    private val logger: ITetroidLogger,
    private val crypter: IStorageCrypter,
    private val loadNodeIconUseCase: LoadNodeIconUseCase,
    private val saveStorageUseCase: SaveStorageUseCase,
) : UseCase<UseCase.None, SetNodeIconUseCase.Params>() {

    data class Params(
        val node: TetroidNode,
        val iconFileName: String?,
//        val storageProvider: IStorageProvider,
//        val storageCrypter: IStorageEncrypter,
    )

    override suspend fun run(params: Params): Either<Failure, None> {
        val node = params.node
        val iconFileName = params.iconFileName

//        if (iconFileName != null && iconFileName.isEmpty()) {
//            return Failure.ArgumentIsEmpty()
//        }
        logger.logOperStart(LogObj.NODE_FIELDS, LogOper.CHANGE, node)
        val oldIconName = node.getIconName(true)
        // обновляем поля
        val crypted = node.isCrypted
        if (crypted && !iconFileName.isNullOrEmpty()) {
            node.iconName = crypter.encryptTextBase64(iconFileName)
        }
        if (crypted) {
            node.setDecryptedIconName(iconFileName)
        }
        // перезаписываем структуру хранилища в файл
        return saveStorageUseCase.run()
            .flatMap {
                loadNodeIconUseCase.run(
                    LoadNodeIconUseCase.Params(node)
                )
            }.onFailure {
                logger.logOperCancel(LogObj.NODE_FIELDS, LogOper.CHANGE)
                // возвращаем изменения
                node.iconName = oldIconName
                if (crypted) {
                    node.setDecryptedIconName(crypter.decryptTextBase64(oldIconName))
                }
            }
    }

}
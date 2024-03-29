package com.gee12.mytetroid.domain.usecase.storage

import android.content.Context
import com.gee12.mytetroid.common.Either
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.UseCase
import com.gee12.mytetroid.common.toRight
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.model.TetroidStorage

class FillStorageFieldsFromDefaultSettingsUseCase(
    private val context: Context, // TODO: CommonSettingsProvider
) : UseCase<TetroidStorage, FillStorageFieldsFromDefaultSettingsUseCase.Params>() {

    data class Params(
        val storage: TetroidStorage,
    )

    suspend fun run(storage: TetroidStorage): Either<Failure, TetroidStorage> {
        return run(Params(storage))
    }

    override suspend fun run(params: Params): Either<Failure, TetroidStorage> {
        return params.storage.apply {
            // основное
            isLoadFavoritesOnly = CommonSettings.isLoadFavoritesOnlyDef(context)
            isKeepLastNode = CommonSettings.isKeepLastNodeDef(context)
            // корзина
            isClearTrashBeforeExit = CommonSettings.isClearTrashBeforeExitDef(context)
            isAskBeforeClearTrashBeforeExit = CommonSettings.isAskBeforeClearTrashBeforeExitDef(context)
            // шифрование
            isSavePassLocal = CommonSettings.isSaveMiddlePassHashLocalDef(context)
            isDecryptToTemp = CommonSettings.isDecryptFilesInTempDef(context)
            // синхронизация
            syncProfile.apply {
                isEnabled = CommonSettings.isSyncStorageDef(context)
                appName = CommonSettings.getSyncAppNameDef(context)
                command = CommonSettings.getSyncCommandDef(context)
                isSyncBeforeInit = CommonSettings.isSyncBeforeInitDef(context)
                isAskBeforeSyncOnInit = CommonSettings.isAskBeforeSyncOnInitDef(context)
                isSyncBeforeExit = CommonSettings.isSyncBeforeExitDef(context)
                isAskBeforeSyncOnExit = CommonSettings.isAskBeforeSyncOnExitDef(context)
                isCheckOutsideChanging = CommonSettings.isCheckOutsideChangingDef(context)
            }
        }.toRight()
    }

}
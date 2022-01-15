package com.gee12.mytetroid.viewmodels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.App
import com.gee12.mytetroid.TetroidEnvironment
import com.gee12.mytetroid.TetroidStorageData
import com.gee12.mytetroid.viewmodels.*

class TetroidViewModelFactory(
    val app: Application,
    val isUseCurrentStorageData: Boolean = true,
    val storageId: Int? = null
) : ViewModelProvider.NewInstanceFactory() {

    @JvmOverloads
    constructor(
        app: Application,
        isUseCurrentStorageData: Boolean
    ) : this(app, isUseCurrentStorageData, null)

    @JvmOverloads
    constructor(
        app: Application,
        storageId: Int? = null
    ) : this(app, false, storageId)

    val environment: TetroidEnvironment?
        get() = App.current

    val currentStorageData: TetroidStorageData?
        get() = if (isUseCurrentStorageData || storageId != null && storageId == environment?.storageData?.storageId)
            environment?.storageData else null

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CommonSettingsViewModel::class.java) -> {
                CommonSettingsViewModel(
                    app = app,
                ) as T
            }
            modelClass.isAssignableFrom(StorageViewModel::class.java) -> {
                StorageViewModel(
                    app = app,
                    storageData = currentStorageData
//                    logger = env.logger,
                ) as T
            }
//            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
            modelClass == MainViewModel::class.java -> {
                MainViewModel(
                    app = app,
//                    logger = env.logger,
                ) as T
            }
            modelClass.isAssignableFrom(RecordViewModel::class.java) -> {
                RecordViewModel(
                    app = app,
                    storageData = environment?.storageData!!
//                    logger = env.logger,
                ) as T
            }
            modelClass.isAssignableFrom(StoragesViewModel::class.java) -> {
                StoragesViewModel(
                    app = app,
//                    logger = env.logger,
                ) as T
            }
            modelClass.isAssignableFrom(IconsViewModel::class.java) -> {
                IconsViewModel(
                    app = app,
                    storageData = environment?.storageData!!
//                    logger = env.logger,
                ) as T
            }
            modelClass.isAssignableFrom(LogsViewModel::class.java) -> {
                LogsViewModel(
                    app = app,
//                    logger = env.logger,
                ) as T
            }
            modelClass.isAssignableFrom(StorageInfoViewModel::class.java) -> {
                StorageInfoViewModel(
                    app = app,
                    storageData = currentStorageData
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}

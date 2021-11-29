package com.gee12.mytetroid.viewmodels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.App
import com.gee12.mytetroid.data.CommonSettings
import com.gee12.mytetroid.viewmodels.*

class TetroidViewModelFactory(val app: Application) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val env = App.current

        return when {
            modelClass.isAssignableFrom(BaseViewModel::class.java) -> {
                BaseViewModel(
                    application = app,
//                    logger = env.logger
                ) as T
            }
            modelClass.isAssignableFrom(StorageViewModel::class.java) -> {
                StorageViewModel(
                    app = app,
//                    logger = env.logger,
                    xmlLoader = env.xmlLoader,
                    crypter = env.crypter,
                    storagesRepo = env.storagesRepo
                ) as T
            }
//            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
            modelClass == MainViewModel::class.java -> {

                // FIXME: как-то может можно это отсюда убрать ?
                //  проблема в том, что init() должен вызваться до создания logger,
                //  в котором при его создании читаются опции из CommonSettings
                //  Но в MainViewModel.init() это не засунешь, т.к. инициализация MainViewModel запускается позже
                //  инициализации BaseViewModel ..!
                CommonSettings.init(app)

                MainViewModel(
                    app = app,
//                    logger = env.logger,
                    xmlLoader = env.xmlLoader,
                    crypter = env.crypter,
                    storagesRepo = env.storagesRepo
                ) as T
            }
            modelClass.isAssignableFrom(RecordViewModel::class.java) -> {
                RecordViewModel(
                    app = app,
//                    logger = env.logger,
                    xmlLoader = env.xmlLoader,
                    crypter = env.crypter,
                    storagesRepo = env.storagesRepo
                ) as T
            }
            modelClass.isAssignableFrom(StoragesViewModel::class.java) -> {
                StoragesViewModel(
                    app = app,
//                    logger = env.logger,
                    storagesRepo = env.storagesRepo!!
                ) as T
            }
            modelClass.isAssignableFrom(IconsViewModel::class.java) -> {
                IconsViewModel(
                    app = app,
//                    logger = env.logger,
                    storageInteractor = env.storageInteractor!!
                ) as T
            }
            modelClass.isAssignableFrom(LogsViewModel::class.java) -> {
                LogsViewModel(
                    app = app,
//                    logger = env.logger
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}
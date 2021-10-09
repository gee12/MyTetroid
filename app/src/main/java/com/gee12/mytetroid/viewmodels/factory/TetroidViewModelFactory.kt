package com.gee12.mytetroid.viewmodels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.App
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.viewmodels.*

class TetroidViewModelFactory(val app: Application): ViewModelProvider.NewInstanceFactory() {

    companion object {
//        private val hashMapViewModel = HashMap<String, ViewModel>()

//        fun getViewModel(key: String, factory: () -> ViewModel): ViewModel {
//            return if (hashMapViewModel.containsKey(key)) {
//                hashMapViewModel[key]!!
//            } else {
//                factory.invoke().also {  hashMapViewModel[key] = it }
//            }
//        }
    }

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        // FIXME: использовать DI
        App.storagesRepo = StoragesRepo(app)

        val className = modelClass.simpleName
        return when {
            modelClass.isAssignableFrom(BaseViewModel::class.java) -> {
                /*getViewModel(className)}*/
                    BaseViewModel(app).also {
                        it.logger = App.logger
                    }
               /* }*/ as T
            }
            modelClass.isAssignableFrom(StorageSettingsViewModel::class.java) -> {
                /*getViewModel(className)}*/ StorageSettingsViewModel(app, App.storagesRepo, App.xmlLoader) /*}*/ as T
            }
            modelClass.isAssignableFrom(StorageViewModel::class.java) -> {
                /*getViewModel(className)}*/ StorageViewModel(app, App.storagesRepo, App.xmlLoader).apply {
//                    initStorageFromLastStorageId()
                } /*}*/ as T
            }
//            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
            modelClass == MainViewModel::class.java -> {
                /*getViewModel(className) {*/
                    MainViewModel(app, App.storagesRepo).also {
//                        // FIXME: использовать di
//                        if (xmlLoader == null) {
//                            storageInteractor = it.storageInteractor
//                            xmlLoader = it.xmlLoader
//                            logger = it.logger
//                        }
                    }
               /* }*/ as T
            }
            modelClass.isAssignableFrom(RecordViewModel::class.java) -> {
//                getViewModel(className) { RecordViewModel(app, storagesRepo, xmlLoader) } as T
                /*getViewModel(className) {*/ RecordViewModel(app, App.storagesRepo, App.xmlLoader) /*}*/ as T
            }
            modelClass.isAssignableFrom(StoragesViewModel::class.java) -> {
                /*getViewModel(className)}*/ StoragesViewModel(app, App.storageInteractor, App.storagesRepo) /*}*/ as T
            }
            modelClass.isAssignableFrom(IconsViewModel::class.java) -> {
                /*getViewModel(className)}*/ IconsViewModel(app, App.storageInteractor) /*}*/ as T
            }
            modelClass.isAssignableFrom(LogsViewModel::class.java) -> {
                /*getViewModel(className)}*/ LogsViewModel(app, App.logger) /*}*/ as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}

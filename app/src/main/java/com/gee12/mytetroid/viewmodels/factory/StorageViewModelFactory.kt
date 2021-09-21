package com.gee12.mytetroid.viewmodels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.viewmodels.MainViewModel
import com.gee12.mytetroid.viewmodels.RecordViewModel
import com.gee12.mytetroid.viewmodels.StorageEncryptionViewModel
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel

class StorageViewModelFactory(val app: Application): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(app, StoragesRepo(app)) as T
            }
            modelClass.isAssignableFrom(RecordViewModel::class.java) -> {
                RecordViewModel(app, StoragesRepo(app)) as T
            }
            modelClass.isAssignableFrom(StorageEncryptionViewModel::class.java) -> {
                StorageEncryptionViewModel(app, StoragesRepo(app)) as T
            }
            modelClass.isAssignableFrom(StorageSettingsViewModel::class.java) -> {
                StorageSettingsViewModel(app, StoragesRepo(app)) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

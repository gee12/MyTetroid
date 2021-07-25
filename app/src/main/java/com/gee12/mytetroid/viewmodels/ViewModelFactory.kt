package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.repo.StoragesRepo

class StoragesViewModelFactory(val app: Application): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoragesViewModel::class.java)) {
            return StoragesViewModel(app, StoragesRepo(app)) as T
        } else if (modelClass.isAssignableFrom(StorageSettingsViewModel::class.java)) {
            return StorageSettingsViewModel(app, StoragesRepo(app)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

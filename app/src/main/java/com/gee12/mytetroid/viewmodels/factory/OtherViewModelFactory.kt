package com.gee12.mytetroid.viewmodels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.interactors.StorageInteractor
import com.gee12.mytetroid.repo.StoragesRepo
import com.gee12.mytetroid.viewmodels.IconsViewModel
import com.gee12.mytetroid.viewmodels.StoragesViewModel

class OtherViewModelFactory(val app: Application, val storageInteractor: StorageInteractor): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoragesViewModel::class.java)) {
            return StoragesViewModel(app, storageInteractor, StoragesRepo(app)) as T
        } else if (modelClass.isAssignableFrom(IconsViewModel::class.java)) {
            return IconsViewModel(app, storageInteractor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.repo.StoragesRepo

class StoragesViewModelFactory(val app: Application): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = StoragesViewModel(app, StoragesRepo(app)) as T
}

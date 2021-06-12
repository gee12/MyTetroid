package com.gee12.mytetroid.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    private val _errorMessage = MutableLiveData<String>()
    val errorMessgae: LiveData<String> = _errorMessage

}
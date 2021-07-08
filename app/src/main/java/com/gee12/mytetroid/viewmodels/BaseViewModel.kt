package com.gee12.mytetroid.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    private val _errorMessage = MutableLiveData<String>()
    val errorMessgae: LiveData<String> = _errorMessage

    //region Context

    //    fun getString(resId: Int) = (getApplication() as Context).getString(resId)
    fun getString(resId: Int) = getApplication<Application>().resources.getString(resId)

    fun getContext(): Context = getApplication()

    //endregion Context
}
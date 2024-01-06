package com.gee12.mytetroid.ui.base

interface ITetroidComponent : ITetroidFileStorage {
    fun setProgressVisibility(isVisible: Boolean, text: String? = null)
    fun showProgress(textResId: Int)
    fun showProgress(text: String? = null)
    fun hideProgress()
    fun showSnackMoreInLogs()
}
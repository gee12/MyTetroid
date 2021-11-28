package com.gee12.mytetroid.views

interface IViewEventListener {
    fun setProgressVisibility(isVisible: Boolean, text: String? = null)
    fun setProgressText(progressTextResId: Int)
    fun setProgressText(progressText: String?)
    fun showSnackMoreInLogs()
}
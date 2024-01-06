package com.gee12.mytetroid.ui.splash

import com.gee12.mytetroid.ui.base.BaseEvent

sealed class SplashEvent : BaseEvent() {

    object AppInitialized : SplashEvent()

    sealed class Migration : SplashEvent() {
        object NoNeeded : SplashEvent()
        object Finished : SplashEvent()
        object Failed : SplashEvent()
    }

    sealed class CheckPermissions : SplashEvent() {
        object NoNeeded : CheckPermissions()
        object Request : CheckPermissions()
        object Granted : CheckPermissions()
    }
}
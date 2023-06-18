package com.gee12.mytetroid.ui.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.gee12.mytetroid.R
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import org.koin.android.ext.android.inject
import org.koin.core.scope.Scope

abstract class TetroidFragment<VM : BaseStorageViewModel> : Fragment, View.OnTouchListener {

    protected lateinit var scopeSource: ScopeSource
    protected val koinScope: Scope
        get() = scopeSource.scope

    private lateinit var logger: ITetroidLogger

    val resourcesProvider: IResourcesProvider by inject()
    val settingsManager: CommonSettingsManager by inject()

    protected var gestureDetector: GestureDetectorCompat? = null


    protected abstract fun getLayoutResourceId(): Int

    protected abstract fun getViewModelClazz(): Class<VM>

    constructor() {}

    constructor(detector: GestureDetectorCompat?) {
        gestureDetector = detector
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        createAndInitViewModel()

        //logger.logDebug(getString(R.string.log_fragment_opened_mask, javaClass.simpleName))
        return inflater.inflate(getLayoutResourceId(), container, false)
    }

    protected fun createAndInitViewModel() {
        createDependencyScope()
        afterCreateDependencyScope()
    }

    protected open fun createDependencyScope() {
        scopeSource = ScopeSource.createNew()
    }

    protected open fun afterCreateDependencyScope() {
        logger = koinScope.get()
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэкранный режим.
     * @param v
     * @param event
     * @return
     */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (gestureDetector != null) gestureDetector!!.onTouchEvent(event)
        return false
    }

    /**
     * Принудительное отображение иконок у пунктов меню.
     */
    @SuppressLint("RestrictedApi")
    protected fun setForceShowMenuIcons(v: View, menu: MenuBuilder) {
        val menuHelper = MenuPopupHelper(requireContext(), menu, v)
        menuHelper.setForceShowIcon(true)
        menuHelper.show()
    }

}